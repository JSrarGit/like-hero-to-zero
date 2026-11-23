package de.likeherotozero.service;

import de.likeherotozero.data.Country;
import de.likeherotozero.data.CountryRepository;
import de.likeherotozero.data.EmissionRecord;
import de.likeherotozero.data.EmissionRepository;
import de.likeherotozero.data.Origin;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Service
public class EmissionService {
    private final CountryRepository countries;
    private final EmissionRepository emissions;

    public EmissionService(CountryRepository countries, EmissionRepository emissions) {
        this.countries = countries;
        this.emissions = emissions;
    }

    @Transactional(readOnly = true)
    public List<Country> countries() {
        Collator collator = Collator.getInstance(Locale.GERMAN);
        return countries.findAll().stream()
                .sorted(Comparator.comparing(Country::getDisplayName, collator))
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean containsCountry(String code) {
        return countries.existsById(code);
    }

    @Transactional(readOnly = true)
    public Optional<EmissionRecord> latest(String countryCode) {
        return emissions.findFirstByCountry_CodeOrderByYearDesc(countryCode);
    }

    @Transactional(readOnly = true)
    public Optional<EmissionRecord> find(String countryCode, int year) {
        return emissions.findByCountry_CodeAndYear(countryCode, year);
    }

    @Transactional
    public boolean add(String countryCode, int year, BigDecimal amount) {
        if (emissions.existsByCountry_CodeAndYear(countryCode, year)) {
            return false;
        }
        Country country = countries.findById(countryCode).orElseThrow();
        emissions.saveAndFlush(new EmissionRecord(country, year, amount, Origin.LOCAL_CONTRIBUTION));
        return true;
    }

    @Transactional
    public boolean correct(String countryCode, int year, BigDecimal amount, Long version) {
        Optional<EmissionRecord> existing = find(countryCode, year);
        if (existing.isEmpty()) {
            return false;
        }
        EmissionRecord record = existing.get();
        if (!Objects.equals(record.getVersion(), version)) {
            throw new OptimisticLockingFailureException("Die Version des Jahreswerts hat sich geändert.");
        }
        record.correctAmount(amount);
        emissions.flush();
        return true;
    }
}
