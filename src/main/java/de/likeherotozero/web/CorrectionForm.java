package de.likeherotozero.web;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class CorrectionForm {
    @NotNull(message = "Gib einen Emissionswert ein.")
    @DecimalMin(value = "0", message = "Der Emissionswert muss mindestens 0 sein.")
    @Digits(integer = 9, fraction = 6, message = "Verwende höchstens neun Vor- und sechs Nachkommastellen.")
    private BigDecimal amount;

    @NotNull(message = "Öffne den Datensatz erneut.")
    private Long version;

    public CorrectionForm() {
    }

    public CorrectionForm(BigDecimal amount, Long version) {
        this.amount = amount;
        this.version = version;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
