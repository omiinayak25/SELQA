package com.omiinqa.unit;

import com.omiinqa.config.ConfigManager;
import com.omiinqa.config.FrameworkConfig;
import com.omiinqa.enums.BrowserType;
import com.omiinqa.exceptions.ConfigurationException;
import org.testng.annotations.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Offline unit tests for the configuration layer. No browser, DB or network —
 * these run anywhere and form the smoke gate for the build.
 */
public class ConfigManagerTest {

    @Test(groups = {"unit", "smoke"})
    public void loadsBaseConfiguration() {
        assertThat(ConfigManager.get().get("browser")).isNotBlank();
    }

    @Test(groups = {"unit", "smoke"})
    public void appliesIntDefaultWhenKeyAbsent() {
        assertThat(ConfigManager.get().getInt("totally.absent.key", 42)).isEqualTo(42);
    }

    @Test(groups = {"unit", "smoke"})
    public void throwsOnMissingRequiredKey() {
        assertThatThrownBy(() -> ConfigManager.get().get("no.such.key"))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("no.such.key");
    }

    @Test(groups = {"unit", "smoke"})
    public void systemPropertyOverridesFile() {
        final String key = "omiinqa.override.probe";
        System.setProperty(key, "overridden");
        try {
            assertThat(ConfigManager.get().get(key, "default")).isEqualTo("overridden");
        } finally {
            System.clearProperty(key);
        }
    }

    @Test(groups = {"unit", "smoke"})
    public void typedFacadeExposesTimeoutsAndBrowser() {
        final FrameworkConfig config = FrameworkConfig.get();
        assertThat(config.browser()).isInstanceOf(BrowserType.class);
        assertThat(config.explicitTimeout()).isGreaterThan(Duration.ZERO);
        assertThat(config.pollingInterval()).isGreaterThan(Duration.ZERO);
        assertThat(config.apiUrl("reqres")).startsWith("http");
        assertThat(config.appUrl("saucedemo")).contains("saucedemo");
    }
}
