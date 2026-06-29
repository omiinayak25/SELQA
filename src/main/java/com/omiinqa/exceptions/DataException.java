package com.omiinqa.exceptions;

/** Thrown when test data cannot be read, parsed, or generated. */
public class DataException extends FrameworkException {

    private static final long serialVersionUID = 1L;

    public DataException(final String message) {
        super(message);
    }

    public DataException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
