package com.omiinqa.utils;

import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;

/**
 * Common device viewports for responsive testing, and a helper to apply them.
 *
 * <p>Driving the browser window to known breakpoints lets one set of tests
 * assert responsive behavior (menu collapses, layout reflows) without real
 * devices. Dimensions track widely-used breakpoints rather than any single
 * vendor's catalogue.</p>
 */
public enum Viewport {

    MOBILE_PORTRAIT(375, 667),
    MOBILE_LANDSCAPE(667, 375),
    TABLET_PORTRAIT(768, 1024),
    TABLET_LANDSCAPE(1024, 768),
    LAPTOP(1366, 768),
    DESKTOP(1920, 1080);

    private final int width;
    private final int height;

    Viewport(final int width, final int height) {
        this.width = width;
        this.height = height;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    /** Resize the driver window to this viewport. */
    public void applyTo(final WebDriver driver) {
        driver.manage().window().setSize(new Dimension(width, height));
    }

    public boolean isMobileWidth() {
        return width <= 480;
    }

    public boolean isTabletWidth() {
        return width > 480 && width <= 1024;
    }
}
