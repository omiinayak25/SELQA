package com.omiinqa.exceptions;

/** Thrown when a JDBC connection, query, or transaction fails. */
public class DatabaseException extends FrameworkException {

    private static final long serialVersionUID = 1L;

    public DatabaseException(final String message) {
        super(message);
    }

    public DatabaseException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
