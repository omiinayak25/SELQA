package com.omiinqa.driver.options;

import com.omiinqa.enums.BrowserType;

/**
 * Selects the correct {@link BrowserOptionsStrategy} for a browser
 * (Factory pattern). Local and remote variants of the same browser share
 * a strategy — capabilities are identical; only the driver transport differs.
 */
public final class OptionsStrategyFactory {

    private OptionsStrategyFactory() {
    }

    public static BrowserOptionsStrategy forBrowser(final BrowserType browser) {
        return switch (browser) {
            case CHROME, REMOTE_CHROME -> new ChromeOptionsStrategy();
            case FIREFOX, REMOTE_FIREFOX -> new FirefoxOptionsStrategy();
            case EDGE, REMOTE_EDGE -> new EdgeOptionsStrategy();
        };
    }
}
