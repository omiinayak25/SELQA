package com.omiinqa.enums;

import java.util.Arrays;
import java.util.Locale;

/**
 * Deployment environments the suite can target. Drives which
 * {@code config/env/<env>.properties} overlay is loaded.
 */
public enum Environment {

    DEV,
    QA,
    STAGING,
    PROD;

    public static Environment from(final String value) {
        if (value == null || value.isBlank()) {
            return QA;
        }
        final String normalized = value.trim().toUpperCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(e -> e.name().equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown environment '" + value + "'. Supported: "
                                + Arrays.toString(values())));
    }

    public String fileName() {
        return name().toLowerCase(Locale.ROOT) + ".properties";
    }
}
