package com.omiinqa.api;

import com.omiinqa.config.FrameworkConfig;
import io.restassured.RestAssured;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeSuite;

/**
 * Base class for all API-layer TestNG test classes.
 *
 * <p><b>Design rationale:</b> Keeps API tests completely decoupled from the
 * Selenium {@code BaseTest} — API tests need no WebDriver, no browser, and no
 * driver lifecycle.  Extending {@code BaseTest} would spin up a browser on
 * every {@code @BeforeMethod}, violating the test isolation contract and
 * wasting resources in CI pipelines that target the API suite only.</p>
 *
 * <p>Responsibilities of this class:</p>
 * <ul>
 *   <li>One-time REST Assured global configuration (Jackson object-mapper,
 *       URL encoding).</li>
 *   <li>Exposes {@link FrameworkConfig} and a typed SLF4J {@link Logger} to
 *       subclasses.</li>
 *   <li>Does NOT touch {@code DriverManager} or browser state.</li>
 * </ul>
 *
 * <p>All concrete API test classes must extend this class and annotate tests
 * with {@code groups = {"api", "regression"}} or {@code groups = {"api", "smoke"}}.</p>
 */
public abstract class AbstractApiTest {

    /** SLF4J logger available to all subclasses. */
    protected final Logger log = LoggerFactory.getLogger(getClass());

    /** Strongly-typed config facade — resolves base URLs, timeouts, etc. */
    protected final FrameworkConfig config = FrameworkConfig.get();

    /**
     * Performs one-time global REST Assured configuration for the entire
     * API test suite.
     *
     * <p>This method runs once per suite (not per test method) because
     * REST Assured's global config is JVM-wide state.  Subsequent calls
     * would overwrite the same config with identical values, which is
     * harmless but unnecessary.</p>
     */
    @BeforeSuite(alwaysRun = true)
    public void configureRestAssured() {
        log.info("Configuring REST Assured global settings");

        RestAssured.config = RestAssuredConfig.config()
                .objectMapperConfig(
                        ObjectMapperConfig.objectMapperConfig()
                                .defaultObjectMapperType(
                                        io.restassured.mapper.ObjectMapperType.JACKSON_2));

        // Disable global URL encoding; path/query params are encoded individually.
        RestAssured.urlEncodingEnabled = false;

        log.info("REST Assured configured — base URLs resolved from config.properties");
    }
}
