package com.omiinqa.driver.options;

import org.openqa.selenium.MutableCapabilities;

/**
 * Strategy interface for building per-browser capabilities.
 *
 * <p><b>Why Strategy:</b> Chrome, Firefox and Edge each need different option
 * objects but the factory should treat them uniformly. Each concrete strategy
 * encapsulates one browser's argument set, so adding a browser means adding a
 * class — not editing a {@code switch} (Open/Closed Principle).</p>
 */
public interface BrowserOptionsStrategy {

    /**
     * Build fully-configured capabilities for this browser.
     *
     * @param headless whether to run without a visible UI
     * @return capabilities ready to hand to a driver constructor
     */
    MutableCapabilities build(boolean headless);
}
