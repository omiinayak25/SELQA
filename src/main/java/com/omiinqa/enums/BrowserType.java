package com.omiinqa.enums;

import java.util.Arrays;

/**
 * Supported browsers and execution targets.
 *
 * <p>The {@code remote} flag distinguishes local driver instantiation from
 * Grid/remote execution so the {@code DriverFactory} can branch without
 * string comparisons leaking through the codebase.</p>
 */
public enum BrowserType {

    CHROME("chrome", false),
    FIREFOX("firefox", false),
    EDGE("edge", false),
    REMOTE_CHROME("remote-chrome", true),
    REMOTE_FIREFOX("remote-firefox", true),
    REMOTE_EDGE("remote-edge", true);

    private final String key;
    private final boolean remote;

    BrowserType(final String key, final boolean remote) {
        this.key = key;
        this.remote = remote;
    }

    public String key() {
        return key;
    }

    public boolean isRemote() {
        return remote;
    }

    /**
     * Resolve a configuration string (case / separator insensitive) to a
     * {@link BrowserType}. Accepts {@code chrome}, {@code REMOTE_CHROME},
     * {@code remote-chrome}, etc.
     *
     * @param value raw configuration value
     * @return the matching browser type
     * @throws IllegalArgumentException if no browser matches
     */
    public static BrowserType from(final String value) {
        if (value == null || value.isBlank()) {
            return CHROME;
        }
        final String normalized = value.trim().toLowerCase().replace('_', '-');
        return Arrays.stream(values())
                .filter(b -> b.key.equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unsupported browser '" + value + "'. Supported: "
                                + Arrays.toString(values())));
    }
}
