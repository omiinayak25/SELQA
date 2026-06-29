package com.omiinqa.exceptions;

/** Thrown when a WebDriver session cannot be created or configured. */
public class DriverInitializationException extends FrameworkException {

    private static final long serialVersionUID = 1L;

    public DriverInitializationException(final String message) {
        super(message);
    }

    public DriverInitializationException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
