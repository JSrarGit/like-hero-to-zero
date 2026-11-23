package de.likeherotozero.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Entity
@Table(name = "countries")
public class Country {
    private static final Map<String, String> GERMAN_NAMES = Arrays.stream(Locale.getISOCountries())
            .map(region -> Locale.of("", region))
            .collect(Collectors.toUnmodifiableMap(Locale::getISO3Country,
                    region -> region.getDisplayCountry(Locale.GERMAN)));

    @Id
    @Column(length = 3)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    protected Country() {
    }

    public Country(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return GERMAN_NAMES.getOrDefault(code, name);
    }
}
