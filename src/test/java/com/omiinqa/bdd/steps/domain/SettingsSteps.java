package com.omiinqa.bdd.steps.domain;

import com.omiinqa.bdd.support.DomainWorld;
import com.omiinqa.reference.platform.SettingsService;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for the reference platform / settings domain.
 *
 * <p>Drives the real {@link SettingsService} (in-memory, typed key-value settings)
 * — no external storage, no browser. Outcomes are recorded via {@link DomainWorld}
 * so shared assertions from {@code CommonDomainSteps} ("the operation succeeds",
 * "a domain error X is raised") work unchanged.</p>
 *
 * <p>All step text is prefixed with the noun <em>settings</em> / <em>Settings</em>
 * to guarantee global uniqueness across the full Cucumber step registry.</p>
 *
 * <p>Domain behaviour covered:</p>
 * <ul>
 *   <li>{@code SETTINGS_UNKNOWN_KEY} — key is not registered</li>
 *   <li>{@code SETTINGS_REQUIRED} — required setting set to blank value</li>
 *   <li>{@code SETTINGS_OUT_OF_RANGE} — INT value non-parseable or outside [min, max]</li>
 *   <li>{@code SETTINGS_BAD_ENUM} — ENUM not in allowed list; BOOLEAN not true/false</li>
 *   <li>Positive get/set/reset/resetAll flows with typed value assertion</li>
 *   <li>isDefined check for registered and unregistered keys</li>
 *   <li>getAll count assertion after changes</li>
 * </ul>
 */
public class SettingsSteps {

    private static final String SVC = "settingsService";
    private static final String CURRENT_VALUE = "settings.currentValue";

    private SettingsService service() {
        return DomainWorld.service(SVC, SettingsService::new);
    }

    // ─── Background / Given ───────────────────────────────────────────────────

    /**
     * Resets the settings service to a clean instance (all built-in settings at
     * their defaults) at the start of each scenario. Called from the feature Background.
     */
    @Given("a clean settings service")
    public void cleanSettingsService() {
        DomainWorld.put(SVC, new SettingsService());
    }

    // ─── When — mutating operations ───────────────────────────────────────────

    /**
     * Sets the settings key {@code key} to {@code value}, capturing any
     * {@link com.omiinqa.reference.core.DomainException} so that negative-path
     * assertions in {@code CommonDomainSteps} can verify the error code.
     *
     * @param key   the setting key to update
     * @param value the new value to assign
     */
    @When("I set the settings key {string} to value {string}")
    public void setSettingsKey(final String key, final String value) {
        DomainWorld.run(() -> service().set(key, value));
    }

    /**
     * Reads the current value of the settings key {@code key} and stores it
     * under {@code "settings.currentValue"} in the {@link DomainWorld} so that
     * subsequent Then steps can assert it. Any domain error is captured.
     *
     * @param key the setting key to retrieve
     */
    @When("I get the settings key {string}")
    public void getSettingsKey(final String key) {
        final String result = DomainWorld.capture(() -> service().get(key));
        if (result != null) {
            DomainWorld.put(CURRENT_VALUE, result);
        }
    }

    /**
     * Resets the setting identified by {@code key} to its declared default value,
     * capturing any domain error.
     *
     * @param key the setting key to reset
     */
    @When("I reset the settings key {string}")
    public void resetSettingsKey(final String key) {
        DomainWorld.run(() -> service().reset(key));
    }

    /**
     * Resets all registered settings to their declared default values, capturing
     * any domain error.
     */
    @When("I reset all settings")
    public void resetAllSettings() {
        DomainWorld.run(() -> service().resetAll());
    }

    // ─── Then — value assertions ──────────────────────────────────────────────

    /**
     * Asserts that the value captured by the most recent
     * {@code I get the settings key} step equals {@code expected}.
     *
     * @param expected the expected setting value
     */
    @Then("the settings value is {string}")
    public void theSettingsValueIs(final String expected) {
        final String actual = DomainWorld.get(CURRENT_VALUE);
        assertThat(actual)
                .as("settings current value")
                .isEqualTo(expected);
    }

    /**
     * Asserts that the live value of {@code key} equals {@code expectedValue}
     * by querying the service directly — does not affect {@code lastError}.
     *
     * @param key           the setting key to inspect
     * @param expectedValue the expected value
     */
    @Then("the settings key {string} has value {string}")
    public void settingsKeyHasValue(final String key, final String expectedValue) {
        assertThat(service().get(key))
                .as("value of settings key '%s'", key)
                .isEqualTo(expectedValue);
    }

    /**
     * Asserts that the live value of {@code key} equals the default value that
     * was declared for it in the registry — verifying a reset restored the default.
     *
     * @param key             the setting key to inspect
     * @param expectedDefault the expected default value
     */
    @Then("the settings key {string} has default value {string}")
    public void settingsKeyHasDefaultValue(final String key, final String expectedDefault) {
        assertThat(service().get(key))
                .as("default value of settings key '%s'", key)
                .isEqualTo(expectedDefault);
    }

    /**
     * Asserts that {@code key} is present in the settings registry (isDefined).
     *
     * @param key the setting key to check
     */
    @Then("the settings key {string} is defined")
    public void settingsKeyIsDefined(final String key) {
        assertThat(service().isDefined(key))
                .as("settings key '%s' should be defined", key)
                .isTrue();
    }

    /**
     * Asserts that {@code key} is NOT present in the settings registry.
     *
     * @param key the setting key to check
     */
    @Then("the settings key {string} is not defined")
    public void settingsKeyIsNotDefined(final String key) {
        assertThat(service().isDefined(key))
                .as("settings key '%s' should not be defined", key)
                .isFalse();
    }

    /**
     * Asserts that {@link SettingsService#getAll()} returns exactly
     * {@code expectedCount} entries.
     *
     * @param expectedCount the expected number of settings in the registry snapshot
     */
    @Then("the settings all count is {int}")
    public void settingsAllCountIs(final int expectedCount) {
        final Map<String, String> all = service().getAll();
        assertThat(all)
                .as("settings getAll count")
                .hasSize(expectedCount);
    }
}
