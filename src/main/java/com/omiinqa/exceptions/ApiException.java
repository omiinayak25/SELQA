package com.omiinqa.exceptions;

/** Thrown when an API request cannot be built, sent, or its response validated. */
public class ApiException extends FrameworkException {

    private static final long serialVersionUID = 1L;

    public ApiException(final String message) {
        super(message);
    }

    public ApiException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
