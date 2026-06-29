package com.omiinqa.bdd.runners;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import org.testng.annotations.DataProvider;

/**
 * TestNG entry point for the Cucumber BDD layer.
 *
 * <p>Glue points at the step definitions and hooks; the Allure Cucumber plugin
 * feeds the same report as the rest of the suite. Scenarios are tagged so CI can
 * run subsets (e.g. {@code -Dcucumber.filter.tags="@smoke"}).</p>
 */
@CucumberOptions(
        features = "src/test/resources/features",
        glue = {"com.omiinqa.bdd.steps", "com.omiinqa.bdd.hooks"},
        plugin = {
                "pretty",
                "html:target/cucumber-report.html",
                "json:target/cucumber.json",
                "io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm"
        },
        monochrome = true
)
public class CucumberTestRunner extends AbstractTestNGCucumberTests {

    /**
     * Scenarios run sequentially by default. Returning the parent provider with
     * parallel disabled keeps a single ThreadLocal driver per scenario simple;
     * enable parallelism here once scenario isolation is verified.
     */
    @Override
    @DataProvider(parallel = false)
    public Object[][] scenarios() {
        return super.scenarios();
    }
}
