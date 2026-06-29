package com.omiinqa.bdd.hooks;

import com.omiinqa.bdd.context.ScenarioContext;
import com.omiinqa.driver.DriverManager;
import com.omiinqa.utils.ScreenshotUtils;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.qameta.allure.Allure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.ByteArrayInputStream;

/**
 * Cucumber lifecycle hooks — the BDD analogue of {@code BaseTest}.
 *
 * <p><b>Tag-conditional driver lifecycle.</b> A real browser is started only for
 * scenarios tagged {@code @ui} or {@code @e2e}. Domain-level scenarios (tagged
 * {@code @domain}) and pure API/DB scenarios run with no browser at all, so the
 * large reference-domain BDD suite executes fully offline (and in CI) without a
 * Grid. This keeps every scenario genuinely executable — no fake UI steps.</p>
 *
 * <p>Logging/MDC and {@link ScenarioContext} cleanup run for <em>every</em>
 * scenario so state never bleeds between them, regardless of layer.</p>
 */
public class CucumberHooks {

    private static final Logger LOG = LoggerFactory.getLogger(CucumberHooks.class);

    /** Tag expression selecting scenarios that genuinely need a real browser. */
    private static final String UI_TAGS = "@ui or @e2e";

    @Before(order = 0)
    public void beforeScenario(final Scenario scenario) {
        MDC.put("testName", scenario.getName());
        LOG.info("▶ SCENARIO START: {}", scenario.getName());
    }

    @Before(value = UI_TAGS, order = 10)
    public void startDriver(final Scenario scenario) {
        DriverManager.startDriver();
    }

    @After(value = UI_TAGS, order = 100)
    public void quitDriver(final Scenario scenario) {
        try {
            if (scenario.isFailed() && DriverManager.hasDriver()) {
                final byte[] png = ScreenshotUtils.captureBytes(DriverManager.getDriver());
                if (png.length > 0) {
                    Allure.addAttachment("failure-" + scenario.getName(),
                            "image/png", new ByteArrayInputStream(png), ".png");
                    scenario.attach(png, "image/png", scenario.getName());
                }
            }
        } finally {
            DriverManager.quitDriver();
        }
    }

    /** Always-run cleanup for every scenario (UI, API, DB or domain). */
    @After(order = 0)
    public void afterScenario(final Scenario scenario) {
        ScenarioContext.clear();
        LOG.info("■ SCENARIO END: {} [{}]", scenario.getName(), scenario.getStatus());
        MDC.remove("testName");
    }
}
