package com.omiinqa.driver.options;

import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.firefox.FirefoxOptions;

/** Firefox capability builder. */
public final class FirefoxOptionsStrategy implements BrowserOptionsStrategy {

    @Override
    public MutableCapabilities build(final boolean headless) {
        final FirefoxOptions options = new FirefoxOptions();
        options.setPageLoadStrategy(PageLoadStrategy.NORMAL);

        if (headless) {
            options.addArguments("-headless");
        }
        options.addArguments("--width=1920", "--height=1080");

        // Reduce noise and avoid update/first-run prompts in CI.
        options.addPreference("dom.webnotifications.enabled", false);
        options.addPreference("app.update.enabled", false);
        options.addPreference("browser.startup.homepage", "about:blank");

        return options;
    }
}
