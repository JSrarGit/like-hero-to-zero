package de.likeherotozero.web;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public class EmissionForm {
    @NotBlank(message = "Wähle ein Land oder Gebiet aus.")
    @Pattern(regexp = "[A-Z]{3}", message = "Wähle ein Land oder Gebiet aus der Liste.")
    private String country;

    @NotNull(message = "Gib ein Bezugsjahr ein.")
    @Min(value = 1750, message = "Das Bezugsjahr muss 1750 oder später sein.")
    private Integer year;

    @NotNull(message = "Gib einen Emissionswert ein.")
    @DecimalMin(value = "0", message = "Der Emissionswert muss mindestens 0 sein.")
    @Digits(integer = 9, fraction = 6, message = "Verwende höchstens neun Vor- und sechs Nachkommastellen.")
    private BigDecimal amount;

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
