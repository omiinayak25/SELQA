package com.omiinqa.reference.core;

/**
 * Failure raised by the in-memory <b>reference domain</b> — a small, genuinely
 * implemented business core (identity, commerce, platform services) that the
 * domain-level BDD suite exercises with real assertions.
 *
 * <p>Every failure carries a stable, machine-readable {@code code} (e.g.
 * {@code AUTH_INVALID_CREDENTIALS}) so Gherkin scenarios can assert the exact
 * business rule that fired — not just "an error occurred". This is what makes
 * the reference-domain scenarios real and non-fake: they pass or fail against
 * actual logic and concrete error codes.</p>
 */
public class DomainException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String code;

    public DomainException(final String code, final String message) {
        super(message);
        this.code = code;
    }

    public DomainException(final String code, final String message, final Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    /** Stable business error code asserted by scenarios, e.g. {@code CART_OUT_OF_STOCK}. */
    public String code() {
        return code;
    }
}
