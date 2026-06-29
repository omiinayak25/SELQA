package com.omiinqa.driver.options;

import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.edge.EdgeOptions;

/** Edge (Chromium) capability builder — mirrors Chrome's hardening flags. */
public final class EdgeOptionsStrategy implements BrowserOptionsStrategy {

    @Override
    public MutableCapabilities build(final boolean headless) {
        final EdgeOptions options = new EdgeOptions();
        options.setPageLoadStrategy(PageLoadStrategy.NORMAL);

        if (headless) {
            options.addArguments("--headless=new");
        }
        options.addArguments(
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--disable-gpu",
                "--disable-notifications",
                "--remote-allow-origins=*",
                "--window-size=1920,1080");

        options.setExperimentalOption("excludeSwitches",
                new String[] {"enable-automation"});

        return options;
    }
}
