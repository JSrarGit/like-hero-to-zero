package de.likeherotozero.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.math.BigDecimal;

@Entity
@Table(name = "emissions", uniqueConstraints = @UniqueConstraint(
        name = "uq_emissions_country_year", columnNames = {"country_code", "emission_year"}))
public class EmissionRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "emissionIds")
    @SequenceGenerator(name = "emissionIds", sequenceName = "emission_ids")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "country_code", nullable = false,
            foreignKey = @ForeignKey(name = "fk_emissions_country"))
    private Country country;

    @Column(name = "emission_year", nullable = false)
    private int year;

    @Column(name = "amount_mt", nullable = false, precision = 15, scale = 6)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private Origin origin;

    @Version
    @Column(nullable = false)
    private Long version;

    protected EmissionRecord() {
    }

    public EmissionRecord(Country country, int year, BigDecimal amount, Origin origin) {
        this.country = country;
        this.year = year;
        this.amount = amount;
        this.origin = origin;
    }

    public Country getCountry() {
        return country;
    }

    public int getYear() {
        return year;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Origin getOrigin() {
        return origin;
    }

    public Long getVersion() {
        return version;
    }

    public void correctAmount(BigDecimal correctedAmount) {
        amount = correctedAmount;
        origin = Origin.LOCAL_CORRECTION;
    }
}
