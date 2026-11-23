package de.likeherotozero.data;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class CsvInitializer implements ApplicationRunner {
    private static final Logger LOG = LoggerFactory.getLogger(CsvInitializer.class);
    private static final String COUNTRY = "Country";
    private static final String CODE = "ISO 3166-1 alpha-3";
    private static final String YEAR = "Year";
    private static final String TOTAL = "Total";
    private static final Set<String> AGGREGATES = Set.of("WLD", "XIA", "XIS");
    private static final Pattern COUNTRY_CODE = Pattern.compile("[A-Z]{3}");
    private static final CSVFormat FORMAT = CSVFormat.DEFAULT.builder()
            .setHeader().setSkipHeaderRecord(true).setTrim(true).get();

    private final CountryRepository countries;
    private final Path csvPath;

    @PersistenceContext
    private EntityManager entityManager;

    public CsvInitializer(CountryRepository countries, @Value("${app.data.csv}") String csvFile) {
        this.countries = countries;
        this.csvPath = Path.of(csvFile);
    }

    @Override
    @Transactional(rollbackFor = IOException.class)
    public void run(ApplicationArguments args) throws IOException {
        if (countries.count() > 0) {
            LOG.info("Vorhandene Emissionsdatenbank wird weiterverwendet.");
            return;
        }
        if (!Files.isRegularFile(csvPath)) {
            throw new IOException("CSV-Datei fehlt: " + csvPath
                    + ". Setze EMISSIONS_CSV auf den lokalen Pfad zur GCP-Datei.");
        }

        Map<String, Country> importedCountries = new HashMap<>();
        int importedValues = 0;
        try (Reader reader = Files.newBufferedReader(csvPath); CSVParser csv = FORMAT.parse(reader)) {
            if (!csv.getHeaderNames().containsAll(Set.of(COUNTRY, CODE, YEAR, TOTAL))) {
                throw new IOException("Der CSV fehlen die erwarteten Spalten für Land, Code, Jahr oder Gesamtwert.");
            }
            for (CSVRecord row : csv) {
                String code = row.get(CODE);
                String total = row.get(TOTAL);
                if (!COUNTRY_CODE.matcher(code).matches() || AGGREGATES.contains(code) || total.isBlank()) {
                    continue;
                }
                Country country = importedCountries.computeIfAbsent(code, key -> {
                    Country newCountry = new Country(key, row.get(COUNTRY));
                    entityManager.persist(newCountry);
                    return newCountry;
                });
                try {
                    entityManager.persist(new EmissionRecord(country, Integer.parseInt(row.get(YEAR)),
                            new BigDecimal(total), Origin.GCP_IMPORT));
                } catch (NumberFormatException error) {
                    throw new IOException("Ungültige Zahl in CSV-Datensatz " + row.getRecordNumber(), error);
                }
                importedValues++;
            }
        }
        if (importedValues == 0) {
            throw new IOException("Die CSV enthält keine importierbaren Jahreswerte.");
        }
        entityManager.flush();
        LOG.info("CSV importiert: {} Jahreswerte für {} Länder und Gebiete.", importedValues, importedCountries.size());
    }
}
