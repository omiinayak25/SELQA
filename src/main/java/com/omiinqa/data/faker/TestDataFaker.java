package com.omiinqa.data.faker;

import net.datafaker.Faker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.Random;
import java.util.UUID;

/**
 * Locale-aware, seed-able wrapper over {@link net.datafaker.Faker} (datafaker 2.4).
 *
 * <p><strong>Pattern:</strong> Facade — hides the datafaker API surface behind
 * domain-specific helper methods so test authors never import {@code Faker}
 * directly. This provides two key benefits:
 * <ol>
 *   <li><em>Seeded reproducibility:</em> passing the same seed to
 *       {@link #TestDataFaker(long)} produces identical sequences, enabling
 *       deterministic replay of failures without a fixed data file.</li>
 *   <li><em>Isolation from library churn:</em> if datafaker's API changes,
 *       only this class needs updating.</li>
 * </ol>
 * </p>
 *
 * <p>Instances are <em>not</em> thread-safe (the underlying {@link Faker} wraps
 * a {@link Random}). For parallel tests each thread should obtain its own
 * instance, or use the {@link #shared()} singleton for non-seeded generation.</p>
 *
 * <p><strong>Usage:</strong>
 * <pre>{@code
 *   TestDataFaker faker = new TestDataFaker();           // random seed, en_US locale
 *   TestDataFaker seeded = new TestDataFaker(42L);       // reproducible
 *   TestDataFaker french = new TestDataFaker(Locale.FRANCE);
 *
 *   String email   = faker.randomEmail();
 *   String name    = faker.randomFullName();
 *   int    age     = faker.randomInt(18, 99);
 * }</pre>
 * </p>
 */
public final class TestDataFaker {

    private static final Logger log = LoggerFactory.getLogger(TestDataFaker.class);

    /** Lazily-initialised shared instance for unseed scenarios. */
    private static volatile TestDataFaker sharedInstance;

    private final Faker faker;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Creates a faker with a random seed and the default {@code en_US} locale.
     */
    public TestDataFaker() {
        this(Locale.US);
    }

    /**
     * Creates a faker with the given locale and a random seed.
     *
     * @param locale target locale for generated text (names, addresses, etc.)
     */
    public TestDataFaker(final Locale locale) {
        this.faker = new Faker(locale);
        log.debug("TestDataFaker created with locale={}", locale);
    }

    /**
     * Creates a seeded faker with the {@code en_US} locale.
     *
     * <p>Seeded instances produce the same sequence on each run, which is useful
     * for reproducing a specific failure scenario without a static data file.</p>
     *
     * @param seed random seed; use the value logged during the failing run
     */
    public TestDataFaker(final long seed) {
        this.faker = new Faker(Locale.US, new Random(seed));
        log.debug("TestDataFaker created with seed={}", seed);
    }

    /**
     * Creates a seeded faker with a specific locale.
     *
     * @param locale target locale
     * @param seed   random seed
     */
    public TestDataFaker(final Locale locale, final long seed) {
        this.faker = new Faker(locale, new Random(seed));
        log.debug("TestDataFaker created with locale={}, seed={}", locale, seed);
    }

    // -------------------------------------------------------------------------
    // Shared instance
    // -------------------------------------------------------------------------

    /**
     * Returns a lazily-initialised shared instance (en_US, random seed).
     * Suitable for scenarios where reproducibility is not required.
     *
     * @return singleton shared instance
     */
    public static TestDataFaker shared() {
        if (sharedInstance == null) {
            synchronized (TestDataFaker.class) {
                if (sharedInstance == null) {
                    sharedInstance = new TestDataFaker();
                }
            }
        }
        return sharedInstance;
    }

    // -------------------------------------------------------------------------
    // Identity / contact helpers
    // -------------------------------------------------------------------------

    /**
     * Returns a realistic-looking email address, e.g. {@code john.doe@example.com}.
     *
     * @return email string; always lowercase
     */
    public String randomEmail() {
        return faker.internet().emailAddress().toLowerCase(Locale.US);
    }

    /**
     * Returns a full name (first + last), e.g. {@code "Alice Smith"}.
     *
     * @return space-separated full name
     */
    public String randomFullName() {
        return faker.name().fullName();
    }

    /**
     * Returns a given (first) name.
     *
     * @return first name string
     */
    public String randomFirstName() {
        return faker.name().firstName();
    }

    /**
     * Returns a family (last) name.
     *
     * @return last name string
     */
    public String randomLastName() {
        return faker.name().lastName();
    }

    /**
     * Returns a realistic username composed of word + number, e.g. {@code "swift_hawk42"}.
     *
     * <p>The value is lowercased and safe for most username fields.</p>
     *
     * @return alphanumeric username; no spaces
     */
    public String randomUsername() {
        return faker.internet().username().toLowerCase(Locale.US);
    }

    /**
     * Returns a randomly generated password that satisfies typical complexity rules
     * (8–16 characters, mix of alpha and digit).
     *
     * @return password string
     */
    public String randomPassword() {
        return faker.internet().password(8, 16, true, true);
    }

    /**
     * Returns a phone number in a locale-appropriate format.
     *
     * @return phone number string
     */
    public String randomPhone() {
        return faker.phoneNumber().phoneNumber();
    }

    /**
     * Returns a full street address, e.g. {@code "123 Main St, Springfield, IL 62701"}.
     *
     * @return single-line address string
     */
    public String randomAddress() {
        return faker.address().fullAddress();
    }

    /**
     * Returns a street address line only, e.g. {@code "123 Main St"}.
     *
     * @return street address
     */
    public String randomStreetAddress() {
        return faker.address().streetAddress();
    }

    /**
     * Returns a city name.
     *
     * @return city string
     */
    public String randomCity() {
        return faker.address().city();
    }

    /**
     * Returns a state or province name.
     *
     * @return state string
     */
    public String randomState() {
        return faker.address().state();
    }

    /**
     * Returns a ZIP / postal code.
     *
     * @return zip code string
     */
    public String randomZipCode() {
        return faker.address().zipCode();
    }

    /**
     * Returns a company name, e.g. {@code "Acme Corp LLC"}.
     *
     * @return company name string
     */
    public String randomCompany() {
        return faker.company().name();
    }

    /**
     * Returns a random UUID string.
     *
     * @return UUID in {@code xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx} format
     */
    public String randomUuid() {
        return UUID.randomUUID().toString();
    }

    // -------------------------------------------------------------------------
    // Numeric / credit card helpers
    // -------------------------------------------------------------------------

    /**
     * Returns a random integer in the range [{@code min}, {@code max}] (inclusive).
     *
     * @param min lower bound (inclusive)
     * @param max upper bound (inclusive)
     * @return random integer within bounds
     * @throws IllegalArgumentException if {@code min > max}
     */
    public int randomInt(final int min, final int max) {
        if (min > max) {
            throw new IllegalArgumentException("min (" + min + ") must be <= max (" + max + ")");
        }
        return faker.number().numberBetween(min, max + 1);
    }

    /**
     * Returns a random credit card number using a valid Luhn checksum format.
     *
     * <p>The number is for test fixture use only — it passes Luhn checks but
     * is not a live payment card.</p>
     *
     * @return credit card number string (digits only, no spaces)
     */
    public String randomCreditCard() {
        return faker.finance().creditCard().replaceAll("[^0-9]", "");
    }

    /**
     * Returns a product name, e.g. {@code "Ergonomic Granite Keyboard"}.
     *
     * @return product name string
     */
    public String randomProductName() {
        return faker.commerce().productName();
    }

    /**
     * Returns a commerce category, e.g. {@code "Electronics"}.
     *
     * @return category string
     */
    public String randomCategory() {
        return faker.commerce().department();
    }

    /**
     * Returns a price string with two decimal places in the given range.
     *
     * @param min minimum price (inclusive)
     * @param max maximum price (exclusive)
     * @return price formatted as {@code "12.99"}
     */
    public String randomPrice(final double min, final double max) {
        double price = min + (faker.random().nextDouble() * (max - min));
        return String.format(Locale.US, "%.2f", price);
    }

    /**
     * Exposes the underlying {@link Faker} for edge cases not covered by helpers.
     * Prefer the typed helper methods above for readability.
     *
     * @return the underlying datafaker {@link Faker} instance
     */
    public Faker raw() {
        return faker;
    }
}
