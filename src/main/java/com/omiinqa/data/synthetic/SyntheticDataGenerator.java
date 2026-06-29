package com.omiinqa.data.synthetic;

import com.omiinqa.data.faker.TestDataFaker;
import com.omiinqa.data.model.Address;
import com.omiinqa.data.model.Product;
import com.omiinqa.data.model.User;
import com.omiinqa.exceptions.DataException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * Higher-level synthetic dataset generator built on top of {@link TestDataFaker}.
 *
 * <p><strong>Pattern:</strong> Facade + Factory — hides the lower-level
 * {@link TestDataFaker} plumbing behind domain-specific generation methods,
 * and provides two complementary utilities as inner classes:
 * <ul>
 *   <li>{@link DataMasker} — masks PII fields for safe logging/reporting</li>
 *   <li>{@link UniqueDataPool} — guarantees collision-free values across a run</li>
 * </ul>
 * </p>
 *
 * <h2>Seeded reproducibility</h2>
 * <p>Constructing with {@link #SyntheticDataGenerator(long)} passes the seed
 * directly to {@link TestDataFaker}, which wraps a {@link java.util.Random}.
 * The same seed, same JDK, same datafaker version → identical output sequence.
 * This allows a failed test to be replayed deterministically without a fixed
 * data file.</p>
 *
 * <p><strong>Usage:</strong>
 * <pre>{@code
 *   // Seeded — reproducible
 *   SyntheticDataGenerator gen = new SyntheticDataGenerator(42L);
 *   List&lt;User&gt;    users    = gen.generateUsers(10);
 *   List&lt;Product&gt; products = gen.generateProducts(5);
 *
 *   // Generic generation with a custom Supplier
 *   List&lt;String&gt; ids = gen.generate(20, () -> UUID.randomUUID().toString());
 *
 *   // Masking PII before logging
 *   String safeEmail = SyntheticDataGenerator.DataMasker.maskEmail("alice@example.com");
 *
 *   // Unique-value pool
 *   SyntheticDataGenerator.UniqueDataPool&lt;String&gt; emails =
 *       new SyntheticDataGenerator.UniqueDataPool&lt;&gt;(() -> gen.faker().randomEmail());
 *   String unique = emails.next();
 * }</pre>
 * </p>
 */
public final class SyntheticDataGenerator {

    private static final Logger log = LoggerFactory.getLogger(SyntheticDataGenerator.class);

    private final TestDataFaker faker;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Creates a generator with a random (non-reproducible) seed.
     */
    public SyntheticDataGenerator() {
        this.faker = new TestDataFaker();
        log.debug("SyntheticDataGenerator created with random seed");
    }

    /**
     * Creates a seeded, fully reproducible generator.
     *
     * <p>The seed is passed through to {@link TestDataFaker#TestDataFaker(long)},
     * which uses it to initialise {@link java.util.Random}. The same seed on the
     * same datafaker version produces an identical output sequence.</p>
     *
     * @param seed random seed; record the seed that produced a failure so you can
     *             replay it deterministically
     */
    public SyntheticDataGenerator(final long seed) {
        this.faker = new TestDataFaker(seed);
        log.debug("SyntheticDataGenerator created with seed={}", seed);
    }

    // -------------------------------------------------------------------------
    // Public generation API
    // -------------------------------------------------------------------------

    /**
     * Generates a list of fully-populated {@link User} objects.
     *
     * <p>Each user receives a random UUID {@code id}, realistic name/email/
     * username/phone, a randomly-generated password, and a full {@link Address}.
     * All fields are backed by the seeded {@link TestDataFaker} so the same seed
     * always yields the same list.</p>
     *
     * @param count number of users to generate; must be &gt;= 1
     * @return list of generated users in insertion order
     * @throws DataException if {@code count} is less than 1
     */
    public List<User> generateUsers(final int count) {
        validateCount(count, "users");
        log.debug("Generating {} synthetic User(s)", count);
        List<User> users = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            users.add(buildUser());
        }
        return users;
    }

    /**
     * Generates a list of fully-populated {@link Product} objects.
     *
     * <p>Each product receives a random UUID {@code id}, a commerce-domain
     * product name, a realistic price in the range [5.00, 499.99], a category,
     * a lorem-ipsum description, and a placeholder image URL.</p>
     *
     * @param count number of products to generate; must be &gt;= 1
     * @return list of generated products in insertion order
     * @throws DataException if {@code count} is less than 1
     */
    public List<Product> generateProducts(final int count) {
        validateCount(count, "products");
        log.debug("Generating {} synthetic Product(s)", count);
        List<Product> products = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            products.add(buildProduct());
        }
        return products;
    }

    /**
     * Generic dataset generator that accepts any {@link Supplier}{@code <T>} and
     * calls it {@code count} times, collecting results into an ordered list.
     *
     * <p>This is the escape hatch for types not covered by the typed helpers above,
     * e.g. custom domain objects, UUIDs, or primitive collections.</p>
     *
     * @param <T>      element type
     * @param count    number of elements to generate; must be &gt;= 1
     * @param supplier factory for one element; called {@code count} times
     * @return list of generated elements
     * @throws DataException if {@code count} is less than 1
     */
    public <T> List<T> generate(final int count, final Supplier<T> supplier) {
        validateCount(count, "elements");
        log.debug("Generating {} element(s) via Supplier", count);
        List<T> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            result.add(supplier.get());
        }
        return result;
    }

    /**
     * Exposes the underlying {@link TestDataFaker} for callers that need raw
     * faker access (e.g., for seeded {@link UniqueDataPool} suppliers).
     *
     * @return the seeded faker instance
     */
    public TestDataFaker faker() {
        return faker;
    }

    // -------------------------------------------------------------------------
    // Private object builders
    // -------------------------------------------------------------------------

    private User buildUser() {
        return User.builder()
                .id(seededId("usr"))
                .firstName(faker.randomFirstName())
                .lastName(faker.randomLastName())
                .email(faker.randomEmail())
                .username(faker.randomUsername())
                .password(faker.randomPassword())
                .phone(faker.randomPhone())
                .address(Address.builder()
                        .street(faker.randomStreetAddress())
                        .city(faker.randomCity())
                        .state(faker.randomState())
                        .zipCode(faker.randomZipCode())
                        .country("US")
                        .build())
                .build();
    }

    private Product buildProduct() {
        BigDecimal price = BigDecimal.valueOf(
                Double.parseDouble(faker.randomPrice(5.00, 499.99)))
                .setScale(2, RoundingMode.HALF_UP);
        return Product.builder()
                .id(seededId("prd"))
                .name(faker.randomProductName())
                .price(price)
                .category(faker.randomCategory())
                .description(faker.raw().lorem().sentence())
                .imageUrl(faker.raw().internet().image())
                .build();
    }

    /**
     * Produces a <em>deterministic</em>, seed-derived identifier.
     *
     * <p><strong>Why not {@code UUID.randomUUID()}?</strong> {@code UUID.randomUUID()}
     * uses a cryptographically-strong RNG that is <em>not</em> seedable, so two
     * generators built with the same seed would emit different IDs and break
     * reproducibility. Likewise {@code TestDataFaker.randomUuid()} delegates to
     * {@code UUID.randomUUID()} and is therefore non-deterministic.</p>
     *
     * <p>This method instead draws a 12-hex-digit value from the <em>seeded</em>
     * {@link TestDataFaker}'s underlying {@link net.datafaker.Faker} number stream,
     * so the same seed yields the same sequence of IDs. The {@code prefix} keeps
     * user and product IDs visually distinct.</p>
     *
     * @param prefix short type prefix, e.g. {@code "usr"} or {@code "prd"}
     * @return deterministic identifier such as {@code "usr-a3f90c12b4d7"}
     */
    private String seededId(final String prefix) {
        // numerify replaces each '#' with a seeded digit; bothify mixes letters.
        // Drawn from the seeded Faker stream => reproducible for a given seed.
        String suffix = faker.raw().bothify("????????????", true);
        return prefix + "-" + suffix;
    }

    private static void validateCount(final int count, final String type) {
        if (count < 1) {
            throw new DataException("Count must be >= 1 but was " + count + " for " + type);
        }
    }

    // =========================================================================
    // Inner class: DataMasker
    // =========================================================================

    /**
     * Masks Personally Identifiable Information (PII) fields so that test
     * logs and reports never expose real (or realistic fake) sensitive data.
     *
     * <p><strong>Pattern:</strong> Utility / Static Factory — all methods are
     * static and idempotent; no state is held.</p>
     *
     * <p>Masking strategy:
     * <ul>
     *   <li><strong>Email</strong> — preserves domain and first character:
     *       {@code "alice@example.com"} → {@code "a***@example.com"}</li>
     *   <li><strong>Phone</strong> — keeps last 4 digits, masks the rest:
     *       {@code "+1-555-867-5309"} → {@code "****5309"}</li>
     *   <li><strong>Credit card</strong> — keeps last 4 digits:
     *       {@code "4111111111111111"} → {@code "************1111"}</li>
     *   <li><strong>Name</strong> — keeps first character, masks the rest:
     *       {@code "Alice Smith"} → {@code "A**** S****"}</li>
     * </ul>
     * </p>
     *
     * <p><strong>Usage:</strong>
     * <pre>{@code
     *   String safe = DataMasker.maskEmail("alice@example.com"); // "a***@example.com"
     *   log.info("Logging user email: {}", safe);
     * }</pre>
     * </p>
     */
    public static final class DataMasker {

        private static final Pattern EMAIL_PATTERN =
                Pattern.compile("^(.)(.*)(@.+)$");

        private DataMasker() {
            // utility — not instantiable
        }

        /**
         * Masks an email address, preserving the first character of the local part
         * and the full domain for diagnostics.
         *
         * <p>Example: {@code "alice@example.com"} → {@code "a***@example.com"}</p>
         *
         * @param email raw email address; {@code null} returns {@code "null"}
         * @return masked email string
         */
        public static String maskEmail(final String email) {
            if (email == null) {
                return "null";
            }
            var matcher = EMAIL_PATTERN.matcher(email);
            if (matcher.matches()) {
                String local = matcher.group(2);
                String mask = "*".repeat(Math.max(local.length(), 3));
                return matcher.group(1) + mask + matcher.group(3);
            }
            return "***";
        }

        /**
         * Masks a phone number, keeping only the last 4 digits.
         *
         * <p>Example: {@code "+1-555-867-5309"} → {@code "****5309"}</p>
         *
         * @param phone raw phone string; {@code null} returns {@code "null"}
         * @return masked phone string
         */
        public static String maskPhone(final String phone) {
            if (phone == null) {
                return "null";
            }
            String digitsOnly = phone.replaceAll("[^0-9]", "");
            if (digitsOnly.length() <= 4) {
                return "****";
            }
            String last4 = digitsOnly.substring(digitsOnly.length() - 4);
            return "*".repeat(digitsOnly.length() - 4) + last4;
        }

        /**
         * Masks a credit card number, keeping only the last 4 digits (PAN truncation,
         * per PCI-DSS requirement 3.3).
         *
         * <p>Example: {@code "4111111111111111"} → {@code "************1111"}</p>
         *
         * @param creditCard raw card number (digits or formatted with spaces/dashes);
         *                   {@code null} returns {@code "null"}
         * @return masked card string
         */
        public static String maskCreditCard(final String creditCard) {
            if (creditCard == null) {
                return "null";
            }
            String digitsOnly = creditCard.replaceAll("[^0-9]", "");
            if (digitsOnly.length() <= 4) {
                return "****";
            }
            String last4 = digitsOnly.substring(digitsOnly.length() - 4);
            return "*".repeat(digitsOnly.length() - 4) + last4;
        }

        /**
         * Masks a person's name, keeping only the first character of each word.
         *
         * <p>Example: {@code "Alice Smith"} → {@code "A**** S****"}</p>
         *
         * @param name full name; {@code null} returns {@code "null"}
         * @return masked name string
         */
        public static String maskName(final String name) {
            if (name == null) {
                return "null";
            }
            if (name.isBlank()) {
                return "***";
            }
            String[] parts = name.trim().split("\\s+");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) {
                    sb.append(' ');
                }
                String part = parts[i];
                if (part.isEmpty()) {
                    continue;
                }
                sb.append(part.charAt(0));
                sb.append("*".repeat(Math.max(part.length() - 1, 3)));
            }
            return sb.toString();
        }
    }

    // =========================================================================
    // Inner class: UniqueDataPool
    // =========================================================================

    /**
     * Guarantees unique values across a test run by maintaining a {@link Set}
     * of previously-yielded values and re-trying the supplier on collision.
     *
     * <p><strong>Pattern:</strong> Decorator — wraps any {@link Supplier}{@code <T>}
     * and enforces the uniqueness invariant without changing the production data
     * model. Collisions are expected to be rare with faker, so the retry loop
     * is bounded at 1 000 attempts before throwing, preventing infinite loops
     * on low-cardinality pools (e.g., boolean suppliers).</p>
     *
     * <p><strong>Usage:</strong>
     * <pre>{@code
     *   SyntheticDataGenerator gen = new SyntheticDataGenerator(42L);
     *   UniqueDataPool&lt;String&gt; emails =
     *       new UniqueDataPool&lt;&gt;(() -> gen.faker().randomEmail());
     *
     *   String email1 = emails.next(); // guaranteed unique
     *   String email2 = emails.next(); // guaranteed != email1
     *   emails.reset(); // clear seen set to start fresh
     * }</pre>
     * </p>
     *
     * @param <T> type of values managed by this pool; must implement
     *            {@link Object#equals(Object)} and {@link Object#hashCode()}
     *            correctly (all standard Java types do)
     */
    public static final class UniqueDataPool<T> {

        private static final int MAX_RETRIES = 1_000;

        private final Supplier<T> supplier;
        private final Set<T> seen;

        /**
         * Creates a new pool backed by the given supplier.
         *
         * @param supplier factory for candidate values; called until a novel value
         *                 is produced
         */
        public UniqueDataPool(final Supplier<T> supplier) {
            this.supplier = supplier;
            this.seen = new HashSet<>();
        }

        /**
         * Returns the next value that has not been yielded by this pool before.
         *
         * @return a value from the supplier that is unique within this pool's
         *         lifetime (or since the last {@link #reset()})
         * @throws DataException if a unique value cannot be found within
         *                       {@value #MAX_RETRIES} attempts
         */
        public T next() {
            for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
                T candidate = supplier.get();
                if (seen.add(candidate)) {
                    return candidate;
                }
            }
            throw new DataException(
                    "UniqueDataPool: could not produce a unique value after "
                            + MAX_RETRIES + " attempts. Consider using a larger cardinality supplier.");
        }

        /**
         * Returns the number of values yielded since creation or the last reset.
         *
         * @return seen-value count
         */
        public int size() {
            return seen.size();
        }

        /**
         * Clears the seen-value set so values yielded previously may be returned
         * again. Useful when setting up multiple independent test scenarios with
         * the same pool instance.
         */
        public void reset() {
            seen.clear();
        }
    }
}
