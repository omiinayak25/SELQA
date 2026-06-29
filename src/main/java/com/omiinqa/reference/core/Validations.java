package com.omiinqa.reference.core;

import java.util.regex.Pattern;

/**
 * Shared, reusable validation rules for the reference domain. Centralizing them
 * keeps business rules consistent across services (registration, profile,
 * password, checkout) and gives BDD scenarios a single source of truth to assert
 * against (DRY).
 */
public final class Validations {

    private static final Pattern EMAIL =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern HAS_UPPER = Pattern.compile(".*[A-Z].*");
    private static final Pattern HAS_LOWER = Pattern.compile(".*[a-z].*");
    private static final Pattern HAS_DIGIT = Pattern.compile(".*[0-9].*");

    /** Minimum acceptable password length. */
    public static final int MIN_PASSWORD_LENGTH = 8;

    private Validations() {
    }

    public static boolean isValidEmail(final String email) {
        return email != null && EMAIL.matcher(email).matches();
    }

    public static boolean isBlank(final String value) {
        return value == null || value.strip().isEmpty();
    }

    /**
     * A password is strong when it is at least {@link #MIN_PASSWORD_LENGTH}
     * characters and contains an upper-case letter, a lower-case letter and a digit.
     */
    public static boolean isStrongPassword(final String password) {
        return password != null
                && password.length() >= MIN_PASSWORD_LENGTH
                && HAS_UPPER.matcher(password).matches()
                && HAS_LOWER.matcher(password).matches()
                && HAS_DIGIT.matcher(password).matches();
    }

    public static void requireValidEmail(final String email, final String code) {
        if (!isValidEmail(email)) {
            throw new DomainException(code, "Invalid email format: " + email);
        }
    }

    public static void requireStrongPassword(final String password, final String code) {
        if (!isStrongPassword(password)) {
            throw new DomainException(code,
                    "Password must be >= " + MIN_PASSWORD_LENGTH
                            + " chars with upper, lower and digit");
        }
    }

    public static void requireNotBlank(final String value, final String field, final String code) {
        if (isBlank(value)) {
            throw new DomainException(code, field + " must not be blank");
        }
    }
}
