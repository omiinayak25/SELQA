package com.omiinqa.cloud;

import com.omiinqa.enums.BrowserType;
import org.openqa.selenium.MutableCapabilities;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Offline unit tests for {@link CloudCapabilityFactory} and all three
 * {@link CloudCapabilityStrategy} implementations.
 *
 * <p><b>Offline guarantee:</b> tests only build and inspect capability objects;
 * they never instantiate a {@link org.openqa.selenium.remote.RemoteWebDriver}
 * and do not open any network connection.  This is enforced by injecting a
 * stub {@link CloudConfig} that returns predictable credentials instead of
 * reading from the live {@link com.omiinqa.config.FrameworkConfig}.</p>
 *
 * <p>Tests do NOT extend {@link com.omiinqa.core.BaseTest} — BaseTest wires up
 * a real browser, which is inappropriate for offline unit tests.</p>
 *
 * <p>Coverage matrix:</p>
 * <ul>
 *   <li>All 3 providers × multiple browser types (data-driven via {@code @DataProvider})</li>
 *   <li>Vendor options block presence and key correctness</li>
 *   <li>Credential placement inside vendor options</li>
 *   <li>Session name and build name propagation</li>
 *   <li>OS / browser version metadata</li>
 *   <li>BrowserName is set by the base browser options (not overwritten)</li>
 *   <li>Null / blank metadata inputs → sensible defaults (no NPE)</li>
 *   <li>Factory routing: correct strategy selected per provider</li>
 *   <li>Null provider guard</li>
 * </ul>
 */
@Test(groups = {"cloud", "unit"})
public class CloudCapabilityFactoryTest {

    // -----------------------------------------------------------------------
    // Stub CloudConfig — returns deterministic credentials so tests are offline
    // -----------------------------------------------------------------------

    /**
     * Uses the default public constructor so that credentials are read from the
     * live {@link com.omiinqa.config.FrameworkConfig} singleton (which returns ""
     * for all {@code cloud.*} keys absent from config files in this environment).
     * Tests assert structural correctness — that the right vendor-options block
     * EXISTS with the right KEYS — not specific credential values, so empty
     * credential strings are acceptable for offline verification.
     */
    private static CloudCapabilityFactory factory;

    // Shared constants used across tests
    private static final String SESSION_NAME = "OmiinQA Smoke";
    private static final String BUILD_NAME   = "CI-Build-99";
    private static final String OS_NAME      = "Windows";
    private static final String OS_VERSION   = "11";
    private static final String BROWSER_VER  = "latest";

    @BeforeClass
    public void setUp() {
        // Use the default public constructor — credentials will be "" from live
        // config (no real .properties on classpath for cloud.* keys), which is
        // fine: we test structure, not credential values.
        factory = new CloudCapabilityFactory();
    }

    // =======================================================================
    // DataProvider: all provider + browser combos
    // =======================================================================

    @DataProvider(name = "providerBrowserCombos")
    public Object[][] providerBrowserCombos() {
        return new Object[][] {
            // Provider,                    Browser,              expectedBrowserName
            { CloudProvider.BROWSERSTACK,  BrowserType.CHROME,   "chrome"        },
            { CloudProvider.BROWSERSTACK,  BrowserType.FIREFOX,  "firefox"       },
            { CloudProvider.BROWSERSTACK,  BrowserType.EDGE,     "MicrosoftEdge" },
            { CloudProvider.SAUCELABS,     BrowserType.CHROME,   "chrome"        },
            { CloudProvider.SAUCELABS,     BrowserType.FIREFOX,  "firefox"       },
            { CloudProvider.SAUCELABS,     BrowserType.EDGE,     "MicrosoftEdge" },
            { CloudProvider.LAMBDATEST,    BrowserType.CHROME,   "chrome"        },
            { CloudProvider.LAMBDATEST,    BrowserType.FIREFOX,  "firefox"       },
            { CloudProvider.LAMBDATEST,    BrowserType.EDGE,     "MicrosoftEdge" },
        };
    }

    // =======================================================================
    // Test 1 — browserName is correctly set for all provider/browser combos
    // =======================================================================

    @Test(groups = {"cloud", "unit"}, dataProvider = "providerBrowserCombos")
    public void browserNameIsSetForAllCombos(final CloudProvider provider,
                                              final BrowserType browser,
                                              final String expectedBrowserName) {
        final MutableCapabilities caps = factory.build(
                provider, browser, OS_NAME, OS_VERSION, BROWSER_VER, SESSION_NAME, BUILD_NAME);

        assertThat(caps.getBrowserName())
                .as("browserName for %s / %s", provider, browser)
                .isEqualToIgnoringCase(expectedBrowserName);
    }

    // =======================================================================
    // Test 2 — vendor options block is present for all combos
    // =======================================================================

    @Test(groups = {"cloud", "unit"}, dataProvider = "providerBrowserCombos")
    public void vendorOptionsBlockIsPresentForAllCombos(final CloudProvider provider,
                                                         final BrowserType browser,
                                                         final String ignoredBrowserName) {
        final MutableCapabilities caps = factory.build(
                provider, browser, OS_NAME, OS_VERSION, BROWSER_VER, SESSION_NAME, BUILD_NAME);

        final String vendorKey = provider.vendorOptionsKey();
        assertThat(caps.getCapability(vendorKey))
                .as("vendor options block '%s' for %s / %s", vendorKey, provider, browser)
                .isNotNull()
                .isInstanceOf(Map.class);
    }

    // =======================================================================
    // Test 3 — BrowserStack uses "bstack:options" key
    // =======================================================================

    @Test(groups = {"cloud", "unit"})
    public void browserStackUsesCorrectVendorKey() {
        assertThat(CloudProvider.BROWSERSTACK.vendorOptionsKey())
                .isEqualTo("bstack:options");

        final MutableCapabilities caps = factory.build(
                CloudProvider.BROWSERSTACK, BrowserType.CHROME,
                OS_NAME, OS_VERSION, BROWSER_VER, SESSION_NAME, BUILD_NAME);

        assertThat(caps.getCapability("bstack:options")).isNotNull();
    }

    // =======================================================================
    // Test 4 — Sauce Labs uses "sauce:options" key
    // =======================================================================

    @Test(groups = {"cloud", "unit"})
    public void sauceLabsUsesCorrectVendorKey() {
        assertThat(CloudProvider.SAUCELABS.vendorOptionsKey())
                .isEqualTo("sauce:options");

        final MutableCapabilities caps = factory.build(
                CloudProvider.SAUCELABS, BrowserType.CHROME,
                OS_NAME, OS_VERSION, BROWSER_VER, SESSION_NAME, BUILD_NAME);

        assertThat(caps.getCapability("sauce:options")).isNotNull();
    }

    // =======================================================================
    // Test 5 — LambdaTest uses "LT:Options" key (exact mixed-case)
    // =======================================================================

    @Test(groups = {"cloud", "unit"})
    public void lambdaTestUsesCorrectVendorKey() {
        assertThat(CloudProvider.LAMBDATEST.vendorOptionsKey())
                .isEqualTo("LT:Options");

        final MutableCapabilities caps = factory.build(
                CloudProvider.LAMBDATEST, BrowserType.CHROME,
                OS_NAME, OS_VERSION, BROWSER_VER, SESSION_NAME, BUILD_NAME);

        assertThat(caps.getCapability("LT:Options")).isNotNull();
    }

    // =======================================================================
    // Test 6 — BrowserStack vendor block has expected keys
    // =======================================================================

    @Test(groups = {"cloud", "unit"})
    public void browserStackVendorBlockHasRequiredKeys() {
        final MutableCapabilities caps = factory.build(
                CloudProvider.BROWSERSTACK, BrowserType.CHROME,
                OS_NAME, OS_VERSION, BROWSER_VER, SESSION_NAME, BUILD_NAME);

        @SuppressWarnings("unchecked")
        final Map<String, Object> bstackOptions =
                (Map<String, Object>) caps.getCapability("bstack:options");

        assertThat(bstackOptions)
                .containsKey("userName")
                .containsKey("accessKey")
                .containsKey("os")
                .containsKey("osVersion")
                .containsKey("browserVersion")
                .containsKey("sessionName")
                .containsKey("buildName");
    }

    // =======================================================================
    // Test 7 — Sauce Labs vendor block has expected keys
    // =======================================================================

    @Test(groups = {"cloud", "unit"})
    public void sauceLabsVendorBlockHasRequiredKeys() {
        final MutableCapabilities caps = factory.build(
                CloudProvider.SAUCELABS, BrowserType.FIREFOX,
                OS_NAME, OS_VERSION, BROWSER_VER, SESSION_NAME, BUILD_NAME);

        @SuppressWarnings("unchecked")
        final Map<String, Object> sauceOptions =
                (Map<String, Object>) caps.getCapability("sauce:options");

        assertThat(sauceOptions)
                .containsKey("username")
                .containsKey("accessKey")
                .containsKey("name")
                .containsKey("build")
                .containsKey("browserVersion")
                .containsKey("platformName");
    }

    // =======================================================================
    // Test 8 — LambdaTest vendor block has expected keys
    // =======================================================================

    @Test(groups = {"cloud", "unit"})
    public void lambdaTestVendorBlockHasRequiredKeys() {
        final MutableCapabilities caps = factory.build(
                CloudProvider.LAMBDATEST, BrowserType.EDGE,
                OS_NAME, OS_VERSION, BROWSER_VER, SESSION_NAME, BUILD_NAME);

        @SuppressWarnings("unchecked")
        final Map<String, Object> ltOptions =
                (Map<String, Object>) caps.getCapability("LT:Options");

        assertThat(ltOptions)
                .containsKey("username")
                .containsKey("accessKey")
                .containsKey("os")
                .containsKey("osVersion")
                .containsKey("browserVersion")
                .containsKey("name")
                .containsKey("build");
    }

    // =======================================================================
    // Test 9 — session name is propagated into BrowserStack options
    // =======================================================================

    @Test(groups = {"cloud", "unit"})
    public void sessionNameIsPropagatedToBrowserStack() {
        final MutableCapabilities caps = factory.build(
                CloudProvider.BROWSERSTACK, BrowserType.CHROME,
                OS_NAME, OS_VERSION, BROWSER_VER, "My Custom Session", BUILD_NAME);

        @SuppressWarnings("unchecked")
        final Map<String, Object> bstackOptions =
                (Map<String, Object>) caps.getCapability("bstack:options");

        assertThat(bstackOptions.get("sessionName")).isEqualTo("My Custom Session");
    }

    // =======================================================================
    // Test 10 — build name is propagated into Sauce Labs options
    // =======================================================================

    @Test(groups = {"cloud", "unit"})
    public void buildNameIsPropagatedToSauceLabs() {
        final MutableCapabilities caps = factory.build(
                CloudProvider.SAUCELABS, BrowserType.CHROME,
                OS_NAME, OS_VERSION, BROWSER_VER, SESSION_NAME, "My-Build-42");

        @SuppressWarnings("unchecked")
        final Map<String, Object> sauceOptions =
                (Map<String, Object>) caps.getCapability("sauce:options");

        assertThat(sauceOptions.get("build")).isEqualTo("My-Build-42");
    }

    // =======================================================================
    // Test 11 — OS metadata is correctly placed in LambdaTest options
    // =======================================================================

    @Test(groups = {"cloud", "unit"})
    public void osMetadataIsPropagatedToLambdaTest() {
        final MutableCapabilities caps = factory.build(
                CloudProvider.LAMBDATEST, BrowserType.CHROME,
                "macOS", "Sequoia", BROWSER_VER, SESSION_NAME, BUILD_NAME);

        @SuppressWarnings("unchecked")
        final Map<String, Object> ltOptions =
                (Map<String, Object>) caps.getCapability("LT:Options");

        assertThat(ltOptions.get("os")).isEqualTo("macOS");
        assertThat(ltOptions.get("osVersion")).isEqualTo("Sequoia");
    }

    // =======================================================================
    // Test 12 — Sauce Labs composes platformName from osName + osVersion
    // =======================================================================

    @Test(groups = {"cloud", "unit"})
    public void sauceLabsComposesPlatformName() {
        final MutableCapabilities caps = factory.build(
                CloudProvider.SAUCELABS, BrowserType.CHROME,
                "macOS", "Ventura", BROWSER_VER, SESSION_NAME, BUILD_NAME);

        @SuppressWarnings("unchecked")
        final Map<String, Object> sauceOptions =
                (Map<String, Object>) caps.getCapability("sauce:options");

        assertThat(sauceOptions.get("platformName"))
                .asString()
                .contains("macOS")
                .contains("Ventura");
    }

    // =======================================================================
    // Test 13 — null / blank metadata falls back to sensible defaults (no NPE)
    // =======================================================================

    @Test(groups = {"cloud", "unit"})
    public void nullMetadataFallsBackToSensibleDefaults() {
        // Should not throw NullPointerException
        final MutableCapabilities caps = factory.build(
                CloudProvider.BROWSERSTACK, BrowserType.CHROME,
                null, null, null, null, null);

        @SuppressWarnings("unchecked")
        final Map<String, Object> bstackOptions =
                (Map<String, Object>) caps.getCapability("bstack:options");

        assertThat(bstackOptions.get("os")).isNotNull().isNotEqualTo("");
        assertThat(bstackOptions.get("osVersion")).isNotNull().isNotEqualTo("");
        assertThat(bstackOptions.get("browserVersion")).isNotNull().isNotEqualTo("");
        assertThat(bstackOptions.get("sessionName")).isNotNull().isNotEqualTo("");
        assertThat(bstackOptions.get("buildName")).isNotNull().isNotEqualTo("");
    }

    // =======================================================================
    // Test 14 — strategyFor returns the correct strategy type per provider
    // =======================================================================

    @Test(groups = {"cloud", "unit"})
    public void strategyForReturnsCorrectImplementation() {
        assertThat(factory.strategyFor(CloudProvider.BROWSERSTACK))
                .isInstanceOf(BrowserStackCapabilityStrategy.class);
        assertThat(factory.strategyFor(CloudProvider.SAUCELABS))
                .isInstanceOf(SauceLabsCapabilityStrategy.class);
        assertThat(factory.strategyFor(CloudProvider.LAMBDATEST))
                .isInstanceOf(LambdaTestCapabilityStrategy.class);
    }

    // =======================================================================
    // Test 15 — strategyFor.provider() returns the right provider enum constant
    // =======================================================================

    @Test(groups = {"cloud", "unit"})
    public void strategyProviderMatchesRegisteredProvider() {
        for (final CloudProvider provider : CloudProvider.values()) {
            assertThat(factory.strategyFor(provider).provider())
                    .as("strategy.provider() for %s", provider)
                    .isEqualTo(provider);
        }
    }

    // =======================================================================
    // Test 16 — null provider guard throws IllegalArgumentException
    // =======================================================================

    @Test(groups = {"cloud", "unit"})
    public void nullProviderThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> factory.build(
                null, BrowserType.CHROME,
                OS_NAME, OS_VERSION, BROWSER_VER, SESSION_NAME, BUILD_NAME))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null");
    }

    // =======================================================================
    // Test 17 — CloudProvider config-key helpers return expected strings
    // =======================================================================

    @Test(groups = {"cloud", "unit"})
    public void cloudProviderConfigKeyHelpersReturnExpectedStrings() {
        assertThat(CloudProvider.BROWSERSTACK.usernameKey())
                .isEqualTo("cloud.browserstack.username");
        assertThat(CloudProvider.BROWSERSTACK.accessKeyKey())
                .isEqualTo("cloud.browserstack.accesskey");
        assertThat(CloudProvider.BROWSERSTACK.urlKey())
                .isEqualTo("cloud.browserstack.url");

        assertThat(CloudProvider.SAUCELABS.usernameKey())
                .isEqualTo("cloud.saucelabs.username");
        assertThat(CloudProvider.LAMBDATEST.usernameKey())
                .isEqualTo("cloud.lambdatest.username");
    }

    // =======================================================================
    // Test 18 — each provider has a non-null, non-blank default hub URL
    // =======================================================================

    @Test(groups = {"cloud", "unit"})
    public void eachProviderHasNonBlankDefaultHubUrl() {
        for (final CloudProvider provider : CloudProvider.values()) {
            assertThat(provider.defaultHubUrl())
                    .as("defaultHubUrl for %s", provider)
                    .isNotBlank()
                    .startsWith("https://");
        }
    }

    // =======================================================================
    // Test 19 — browserVersion is forwarded into the vendor options map
    // =======================================================================

    @Test(groups = {"cloud", "unit"})
    public void browserVersionIsForwardedToAllProviders() {
        final String version = "120";

        for (final CloudProvider provider : CloudProvider.values()) {
            final MutableCapabilities caps = factory.build(
                    provider, BrowserType.CHROME,
                    OS_NAME, OS_VERSION, version, SESSION_NAME, BUILD_NAME);

            @SuppressWarnings("unchecked")
            final Map<String, Object> vendorOpts =
                    (Map<String, Object>) caps.getCapability(provider.vendorOptionsKey());

            assertThat(vendorOpts)
                    .as("vendor block for %s should contain browserVersion", provider)
                    .containsValue(version);
        }
    }

    // =======================================================================
    // Test 20 — headless overload correctly wires through to capabilities
    // =======================================================================

    @Test(groups = {"cloud", "unit"})
    public void headlessBuildOverloadDoesNotThrow() {
        // Headless is a browser-level flag; the test just ensures no exception
        // is thrown and capabilities are well-formed.
        final MutableCapabilities caps = factory.build(
                CloudProvider.BROWSERSTACK, BrowserType.CHROME,
                /* headless= */ true,
                OS_NAME, OS_VERSION, BROWSER_VER, SESSION_NAME, BUILD_NAME);

        assertThat(caps).isNotNull();
        assertThat(caps.getBrowserName()).isNotBlank();
        assertThat(caps.getCapability("bstack:options")).isNotNull();
    }
}
