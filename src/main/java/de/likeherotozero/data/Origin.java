package de.likeherotozero.data;

public enum Origin {
    GCP_IMPORT("Global Carbon Project · Version 2025v15"),
    LOCAL_CONTRIBUTION("Lokaler Beitrag"),
    LOCAL_CORRECTION("Lokale Korrektur");

    private final String label;

    Origin(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
