package com.omiinqa.reference.security;

import com.omiinqa.reference.core.DomainException;
import com.omiinqa.security.SecurityPayloads;

import java.util.regex.Pattern;

/**
 * Input sanitisation and validation guard for the reference security domain.
 *
 * <p>Detects known attack payloads from {@link SecurityPayloads} (SQL injection,
 * XSS, path traversal) and enforces structural rules (blank, length, charset)
 * — all before any downstream service processes the input. Each violation throws
 * a {@link DomainException} with a stable, machine-readable error code that BDD
 * scenarios can assert.</p>
 *
 * <p>Error codes raised by this service:</p>
 * <ul>
 *   <li>{@code SEC_SQLI} — SQL-injection pattern detected in input</li>
 *   <li>{@code SEC_XSS} — XSS pattern detected in input</li>
 *   <li>{@code SEC_TRAVERSAL} — path-traversal pattern detected in input</li>
 *   <li>{@code VALIDATION_REQUIRED} — input is null or blank</li>
 *   <li>{@code VALIDATION_TOO_LONG} — input exceeds the configured maximum length</li>
 *   <li>{@code VALIDATION_BAD_CHARSET} — input contains characters outside the safe set</li>
 * </ul>
 */
public final class InputGuard {

    /** Maximum safe field length (characters). */
    public static final int MAX_LENGTH = 255;

    /**
     * Safe charset: printable ASCII minus control characters.
     * Allows letters, digits, spaces, and common punctuation used in
     * usernames/emails/passwords. Excludes raw angle brackets and quotes used
     * in XSS/SQLi without encoding — those are caught by payload checks first.
     */
    private static final Pattern SAFE_CHARSET =
            Pattern.compile("^[\\x20-\\x7E]*$");

    private InputGuard() {
    }

    // -----------------------------------------------------------------------
    // Security payload rejectors
    // -----------------------------------------------------------------------

    /**
     * Reject SQL-injection payloads.
     *
     * @param input the field value to check
     * @throws DomainException {@code SEC_SQLI} if a known SQLi pattern is present
     */
    public static void rejectSqlInjection(final String input) {
        if (input == null) {
            return;
        }
        final String lower = input.toLowerCase();
        for (final String payload : SecurityPayloads.SQL_INJECTION) {
            if (lower.contains(payload.toLowerCase())) {
                throw new DomainException("SEC_SQLI",
                        "SQL-injection pattern detected in input: " + abbreviate(input));
            }
        }
    }

    /**
     * Reject XSS payloads.
     *
     * @param input the field value to check
     * @throws DomainException {@code SEC_XSS} if a known XSS pattern is present
     */
    public static void rejectXss(final String input) {
        if (input == null) {
            return;
        }
        final String lower = input.toLowerCase();
        for (final String payload : SecurityPayloads.XSS) {
            if (lower.contains(payload.toLowerCase())) {
                throw new DomainException("SEC_XSS",
                        "XSS pattern detected in input: " + abbreviate(input));
            }
        }
    }

    /**
     * Reject path-traversal payloads.
     *
     * @param input the field value to check
     * @throws DomainException {@code SEC_TRAVERSAL} if a known path-traversal
     *         pattern is present
     */
    public static void rejectPathTraversal(final String input) {
        if (input == null) {
            return;
        }
        final String lower = input.toLowerCase();
        for (final String payload : SecurityPayloads.PATH_TRAVERSAL) {
            if (lower.contains(payload.toLowerCase())) {
                throw new DomainException("SEC_TRAVERSAL",
                        "Path-traversal pattern detected in input: " + abbreviate(input));
            }
        }
    }

    /**
     * Run all three security payload checks ({@link #rejectSqlInjection},
     * {@link #rejectXss}, {@link #rejectPathTraversal}) in order. The first
     * match raises immediately.
     *
     * @param input the field value to check
     * @throws DomainException with the appropriate {@code SEC_*} code
     */
    public static void assertSafe(final String input) {
        rejectSqlInjection(input);
        rejectXss(input);
        rejectPathTraversal(input);
    }

    // -----------------------------------------------------------------------
    // Structural / charset validators
    // -----------------------------------------------------------------------

    /**
     * Reject blank (null, empty, or whitespace-only) inputs.
     *
     * @param input the field value
     * @param field human-readable field name for the error message
     * @throws DomainException {@code VALIDATION_REQUIRED}
     */
    public static void requireNotBlank(final String input, final String field) {
        if (input == null || input.strip().isEmpty()) {
            throw new DomainException("VALIDATION_REQUIRED",
                    field + " must not be blank");
        }
    }

    /**
     * Reject inputs that exceed {@link #MAX_LENGTH} characters.
     *
     * @param input the field value
     * @param field human-readable field name for the error message
     * @throws DomainException {@code VALIDATION_TOO_LONG}
     */
    public static void requireMaxLength(final String input, final String field) {
        requireMaxLength(input, field, MAX_LENGTH);
    }

    /**
     * Reject inputs that exceed {@code maxLen} characters.
     *
     * @param input  the field value
     * @param field  human-readable field name for the error message
     * @param maxLen maximum allowed length (inclusive)
     * @throws DomainException {@code VALIDATION_TOO_LONG}
     */
    public static void requireMaxLength(final String input, final String field,
                                        final int maxLen) {
        if (input != null && input.length() > maxLen) {
            throw new DomainException("VALIDATION_TOO_LONG",
                    field + " exceeds maximum length of " + maxLen
                            + " (actual: " + input.length() + ")");
        }
    }

    /**
     * Reject inputs containing characters outside the printable ASCII safe set.
     *
     * @param input the field value
     * @param field human-readable field name for the error message
     * @throws DomainException {@code VALIDATION_BAD_CHARSET}
     */
    public static void requireSafeCharset(final String input, final String field) {
        if (input == null) {
            return;
        }
        if (!SAFE_CHARSET.matcher(input).matches()) {
            throw new DomainException("VALIDATION_BAD_CHARSET",
                    field + " contains characters outside the allowed printable-ASCII set");
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static String abbreviate(final String s) {
        return s.length() > 40 ? s.substring(0, 40) + "..." : s;
    }
}
