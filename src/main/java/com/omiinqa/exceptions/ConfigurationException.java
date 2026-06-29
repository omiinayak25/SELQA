package com.omiinqa.exceptions;

/** Thrown when configuration is missing, malformed, or cannot be loaded. */
public class ConfigurationException extends FrameworkException {

    private static final long serialVersionUID = 1L;

    public ConfigurationException(final String message) {
        super(message);
    }

    public ConfigurationException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
