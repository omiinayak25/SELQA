package com.omiinqa.exceptions;

/**
 * Root of the framework's unchecked exception hierarchy.
 *
 * <p>All framework-thrown failures extend this type so callers (and the
 * global TestNG listener) can catch a single base type and distinguish
 * framework faults from product-under-test failures. Unchecked by design:
 * test code should not be forced into boilerplate try/catch for
 * infrastructure errors.</p>
 */
public class FrameworkException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public FrameworkException(final String message) {
        super(message);
    }

    public FrameworkException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
