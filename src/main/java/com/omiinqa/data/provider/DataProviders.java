package com.omiinqa.data.provider;

import com.omiinqa.data.factory.CredentialsFactory;
import com.omiinqa.data.factory.UserFactory;
import com.omiinqa.data.faker.TestDataFaker;
import com.omiinqa.data.model.Credentials;
import com.omiinqa.data.model.User;
import com.omiinqa.utils.data.CsvDataReader;
import com.omiinqa.utils.data.ExcelDataReader;
import com.omiinqa.utils.data.JsonDataReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.DataProvider;

import java.util.List;

/**
 * Central registry of TestNG {@link DataProvider} methods for the data-management layer.
 *
 * <p><strong>Pattern:</strong> Data Provider / Registry — collects all
 * {@code @DataProvider}-annotated methods in one class so test authors can
 * reference them via a single {@code dataProviderClass} declaration rather than
 * scattering provider logic across test classes. Providers are named as string
 * constants so IDEs and Javadoc searches find them easily.</p>
 *
 * <p>Each provider returns {@code Object[][]} where each inner array is one
 * TestNG test invocation. Column conventions are documented on each method.</p>
 *
 * <p><strong>Usage in a test class:</strong>
 * <pre>{@code
 *   @Test(dataProvider = "loginCredentials",
 *         dataProviderClass = DataProviders.class,
 *         groups = {"smoke", "login"})
 *   public void testLogin(String username, String password, String expectedOutcome) {
 *       loginPage.login(username, password);
 *       // assert based on expectedOutcome
 *   }
 * }</pre>
 * </p>
 *
 * <p>Providers sourcing from CSV/JSON/Excel files resolve paths relative to the
 * classpath root — place files under {@code src/test/resources/testdata/}.</p>
 */
public final class DataProviders {

    private static final Logger log = LoggerFactory.getLogger(DataProviders.class);

    // Classpath paths for test data files
    private static final String LOGIN_DATA_CSV      = "testdata/login-data.csv";
    private static final String CHECKOUT_CSV        = "testdata/checkout-customers.csv";
    private static final String SEARCH_TERMS_CSV    = "testdata/search-terms.csv";
    private static final String USERS_JSON          = "testdata/users.json";
    private static final String PRODUCTS_JSON       = "testdata/products.json";

    private DataProviders() {
        // registry class — not instantiable
    }

    // =========================================================================
    // Login / Authentication
    // =========================================================================

    /**
     * Provides SauceDemo login credential combinations sourced from
     * {@code testdata/login-data.csv}.
     *
     * <p>CSV column order: {@code username, password, expectedOutcome}</p>
     * <p>Falls back to hard-coded {@link CredentialsFactory} rows if the CSV
     * is unavailable (e.g., in a fresh clone before test-data generation).</p>
     *
     * <p>Provider name: {@code "loginCredentials"}</p>
     *
     * <p>Return shape: {@code Object[][] { {String username, String password,
     * String expectedOutcome}, ... }}</p>
     *
     * @return Object[][] where each row is [username, password, expectedOutcome]
     */
    @DataProvider(name = "loginCredentials", parallel = false)
    public static Object[][] loginCredentials() {
        log.debug("DataProvider 'loginCredentials' loading from '{}'", LOGIN_DATA_CSV);
        try {
            return CsvDataReader.toDataProvider(LOGIN_DATA_CSV);
        } catch (Exception e) {
            log.warn("CSV '{}' not found; using CredentialsFactory fallback: {}",
                    LOGIN_DATA_CSV, e.getMessage());
            return buildLoginFallback();
        }
    }

    /**
     * Provides SauceDemo credentials sourced directly from {@link CredentialsFactory}
     * for tests that want typed objects rather than raw strings.
     *
     * <p>Provider name: {@code "sauceDemoCredentials"}</p>
     *
     * <p>Return shape: {@code Object[][] { {Credentials}, ... }}</p>
     *
     * @return Object[][] where each row is [Credentials]
     */
    @DataProvider(name = "sauceDemoCredentials", parallel = false)
    public static Object[][] sauceDemoCredentials() {
        return new Object[][] {
            { CredentialsFactory.standardUser() },
            { CredentialsFactory.lockedOutUser() },
            { CredentialsFactory.problemUser() },
            { CredentialsFactory.performanceGlitchUser() }
        };
    }

    /**
     * Provides invalid login credential combinations for negative authentication tests.
     *
     * <p>Provider name: {@code "invalidLoginCredentials"}</p>
     *
     * <p>Return shape: {@code Object[][] { {String username, String password,
     * String errorMessageFragment}, ... }}</p>
     *
     * @return Object[][] where each row is [username, password, errorFragment]
     */
    @DataProvider(name = "invalidLoginCredentials", parallel = false)
    public static Object[][] invalidLoginCredentials() {
        Credentials locked  = CredentialsFactory.lockedOutUser();
        Credentials invalid = CredentialsFactory.invalidUser();
        Credentials blank   = CredentialsFactory.blankUsername();
        Credentials noPass  = CredentialsFactory.blankPassword();

        return new Object[][] {
            { locked.getUsername(),  locked.getPassword(),  "Sorry, this user has been locked out" },
            { invalid.getUsername(), invalid.getPassword(), "Username and password do not match" },
            { blank.getUsername(),   blank.getPassword(),   "Username is required" },
            { noPass.getUsername(),  noPass.getPassword(),  "Password is required" }
        };
    }

    // =========================================================================
    // Checkout / Customers
    // =========================================================================

    /**
     * Provides checkout customer data (first name, last name, ZIP) sourced from
     * {@code testdata/checkout-customers.csv}.
     *
     * <p>CSV column order: {@code firstName, lastName, zipCode}</p>
     *
     * <p>Provider name: {@code "checkoutCustomers"}</p>
     *
     * <p>Return shape: {@code Object[][] { {String firstName, String lastName,
     * String zipCode}, ... }}</p>
     *
     * @return Object[][] where each row is [firstName, lastName, zipCode]
     */
    @DataProvider(name = "checkoutCustomers", parallel = false)
    public static Object[][] checkoutCustomers() {
        log.debug("DataProvider 'checkoutCustomers' loading from '{}'", CHECKOUT_CSV);
        try {
            return CsvDataReader.toDataProvider(CHECKOUT_CSV);
        } catch (Exception e) {
            log.warn("CSV '{}' not found; using faker fallback: {}", CHECKOUT_CSV, e.getMessage());
            return buildCheckoutFallback();
        }
    }

    // =========================================================================
    // Search
    // =========================================================================

    /**
     * Provides product search terms from {@code testdata/search-terms.csv}.
     *
     * <p>CSV column order: {@code term, expectedResultCount} (expectedResultCount
     * may be "-1" to indicate "any results")</p>
     *
     * <p>Provider name: {@code "searchTerms"}</p>
     *
     * <p>Return shape: {@code Object[][] { {String term, String expectedCount}, ... }}</p>
     *
     * @return Object[][] where each row is [searchTerm, expectedResultCount]
     */
    @DataProvider(name = "searchTerms", parallel = false)
    public static Object[][] searchTerms() {
        log.debug("DataProvider 'searchTerms' loading from '{}'", SEARCH_TERMS_CSV);
        try {
            return CsvDataReader.toDataProvider(SEARCH_TERMS_CSV);
        } catch (Exception e) {
            log.warn("CSV '{}' not found; using inline fallback: {}", SEARCH_TERMS_CSV, e.getMessage());
            return new Object[][] {
                { "sauce",     "-1" },
                { "backpack",  "1"  },
                { "t-shirt",   "-1" },
                { "nonexistent_xyz_abc", "0" }
            };
        }
    }

    // =========================================================================
    // Users (JSON-sourced)
    // =========================================================================

    /**
     * Provides user data as typed {@link User} objects loaded from
     * {@code testdata/users.json}.
     *
     * <p>Provider name: {@code "users"}</p>
     *
     * <p>Return shape: {@code Object[][] { {User}, ... }}</p>
     *
     * @return Object[][] where each row is [User]
     */
    @DataProvider(name = "users", parallel = false)
    public static Object[][] users() {
        log.debug("DataProvider 'users' loading from '{}'", USERS_JSON);
        try {
            List<User> userList = JsonDataReader.readList(USERS_JSON, User.class);
            Object[][] data = new Object[userList.size()][1];
            for (int i = 0; i < userList.size(); i++) {
                data[i][0] = userList.get(i);
            }
            return data;
        } catch (Exception e) {
            log.warn("JSON '{}' not found; using UserFactory fallback: {}", USERS_JSON, e.getMessage());
            return new Object[][] {
                { UserFactory.validUser() },
                { UserFactory.adminUser() }
            };
        }
    }

    /**
     * Provides faker-generated random users for property-based / fuzz tests.
     * Each test run generates a fresh set; add a seed to the faker constructor
     * for deterministic replay.
     *
     * <p>Provider name: {@code "randomUsers"}</p>
     *
     * <p>Return shape: {@code Object[][] { {User}, ... }}</p>
     *
     * @return Object[][] of 5 randomly-generated users per invocation
     */
    @DataProvider(name = "randomUsers", parallel = false)
    public static Object[][] randomUsers() {
        int count = 5;
        Object[][] data = new Object[count][1];
        for (int i = 0; i < count; i++) {
            data[i][0] = UserFactory.validUser();
        }
        return data;
    }

    // =========================================================================
    // Excel-backed (exercised against a generated workbook)
    // =========================================================================

    /**
     * Provides login data from an Excel workbook, sourcing the "Login" sheet.
     *
     * <p>This provider expects the workbook to have been pre-generated by
     * {@link com.omiinqa.utils.data.ExcelWorkbookGenerator} into
     * {@code target/testdata/test-data-sheet.xlsx}. If the file is absent the
     * provider falls back to inline data so the suite does not break in CI
     * before the setup step runs.</p>
     *
     * <p>Provider name: {@code "excelLoginData"}</p>
     *
     * <p>Return shape: {@code Object[][] { {String username, String password,
     * String expected}, ... }}</p>
     *
     * @return Object[][] where each row is [username, password, expected]
     */
    @DataProvider(name = "excelLoginData", parallel = false)
    public static Object[][] excelLoginData() {
        String path = "testdata/test-data-sheet.xlsx";
        log.debug("DataProvider 'excelLoginData' loading from '{}'", path);
        try {
            return ExcelDataReader.toDataProvider(path, "Login");
        } catch (Exception e) {
            log.warn("Excel '{}' not found; using fallback: {}", path, e.getMessage());
            return buildLoginFallback();
        }
    }

    /**
     * Provides checkout customer data from the "Customers" sheet of the generated
     * Excel workbook.
     *
     * <p>Provider name: {@code "excelCheckoutCustomers"}</p>
     *
     * <p>Return shape: {@code Object[][] { {String firstName, String lastName,
     * String zipCode}, ... }}</p>
     *
     * @return Object[][] where each row is [firstName, lastName, zipCode]
     */
    @DataProvider(name = "excelCheckoutCustomers", parallel = false)
    public static Object[][] excelCheckoutCustomers() {
        String path = "testdata/test-data-sheet.xlsx";
        log.debug("DataProvider 'excelCheckoutCustomers' loading from '{}'", path);
        try {
            return ExcelDataReader.toDataProvider(path, "Customers");
        } catch (Exception e) {
            log.warn("Excel '{}' not found; using faker fallback: {}", path, e.getMessage());
            return buildCheckoutFallback();
        }
    }

    // =========================================================================
    // Internal fallback helpers
    // =========================================================================

    /** Hard-coded SauceDemo login matrix used when CSV/Excel is unavailable. */
    private static Object[][] buildLoginFallback() {
        return new Object[][] {
            { "standard_user",           "secret_sauce", "pass" },
            { "locked_out_user",         "secret_sauce", "fail" },
            { "problem_user",            "secret_sauce", "pass" },
            { "performance_glitch_user", "secret_sauce", "pass" }
        };
    }

    /** Faker-generated checkout rows used when CSV/Excel is unavailable. */
    private static Object[][] buildCheckoutFallback() {
        TestDataFaker faker = new TestDataFaker();
        return new Object[][] {
            { faker.randomFirstName(), faker.randomLastName(), faker.randomZipCode() },
            { faker.randomFirstName(), faker.randomLastName(), faker.randomZipCode() },
            { faker.randomFirstName(), faker.randomLastName(), faker.randomZipCode() }
        };
    }
}
