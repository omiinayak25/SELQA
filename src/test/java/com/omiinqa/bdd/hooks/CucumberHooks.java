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
 * <p>Starts a ThreadLocal driver before each scenario and always quits it
 * after, attaching a failure screenshot to Allure. Scenario context is cleared
 * on teardown so no state bleeds between scenarios.</p>
 */
public class CucumberHooks {

    private static final Logger LOG = LoggerFactory.getLogger(CucumberHooks.class);

    @Before(order = 0)
    public void startDriver(final Scenario scenario) {
        MDC.put("testName", scenario.getName());
        LOG.info("▶ SCENARIO START: {}", scenario.getName());
        DriverManager.startDriver();
    }

    @After(order = 100)
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
            ScenarioContext.clear();
            LOG.info("■ SCENARIO END: {} [{}]", scenario.getName(), scenario.getStatus());
            MDC.remove("testName");
        }
    }
}
