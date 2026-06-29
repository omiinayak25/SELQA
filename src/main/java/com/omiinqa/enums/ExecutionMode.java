package com.omiinqa.enums;

import java.util.Arrays;
import java.util.Locale;

/** Where the WebDriver session physically runs. */
public enum ExecutionMode {

    /** Driver started on the same host as the test JVM. */
    LOCAL,
    /** Driver started on an arbitrary Selenium remote endpoint. */
    REMOTE,
    /** Driver started against a Selenium Grid hub. */
    GRID;

    public static ExecutionMode from(final String value) {
        if (value == null || value.isBlank()) {
            return LOCAL;
        }
        return Arrays.stream(values())
                .filter(m -> m.name().equalsIgnoreCase(value.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown execution mode '" + value + "'. Supported: "
                                + Arrays.toString(values())));
    }

    public String lower() {
        return name().toLowerCase(Locale.ROOT);
    }
}
