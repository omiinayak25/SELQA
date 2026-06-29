package com.omiinqa.driver.options;

import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.chrome.ChromeOptions;

import java.util.Map;

/**
 * Chrome capability builder. Hardened with the flags that make Chrome stable
 * in CI/Docker (no sandbox, disabled /dev/shm usage, no first-run popups).
 */
public final class ChromeOptionsStrategy implements BrowserOptionsStrategy {

    @Override
    public MutableCapabilities build(final boolean headless) {
        final ChromeOptions options = new ChromeOptions();
        options.setPageLoadStrategy(PageLoadStrategy.NORMAL);

        if (headless) {
            options.addArguments("--headless=new");
        }

        options.addArguments(
                "--no-sandbox",                       // required in many container images
                "--disable-dev-shm-usage",            // avoid /dev/shm exhaustion in Docker
                "--disable-gpu",
                "--disable-extensions",
                "--disable-notifications",
                "--disable-popup-blocking",
                "--disable-infobars",
                "--remote-allow-origins=*",
                "--window-size=1920,1080");

        // Suppress the "Chrome is being controlled by automated software" banner
        // and the password-manager bubble that can steal focus.
        options.setExperimentalOption("excludeSwitches",
                new String[] {"enable-automation"});
        options.setExperimentalOption("prefs", Map.of(
                "credentials_enable_service", false,
                "profile.password_manager_enabled", false));

        return options;
    }
}
