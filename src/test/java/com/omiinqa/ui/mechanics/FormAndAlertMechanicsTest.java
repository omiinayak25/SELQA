package com.omiinqa.ui.mechanics;

import com.omiinqa.core.BaseTest;
import com.omiinqa.pages.theinternet.CheckboxesPage;
import com.omiinqa.pages.theinternet.DropdownPage;
import com.omiinqa.pages.theinternet.InputsPage;
import com.omiinqa.pages.theinternet.JavaScriptAlertsPage;
import com.omiinqa.pages.theinternet.TheInternetLoginPage;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies core HTML form and browser-alert mechanics against the
 * <a href="https://the-internet.herokuapp.com">The Internet</a> practice site.
 *
 * <p>Coverage areas:</p>
 * <ul>
 *   <li><strong>Login authentication</strong> – valid credentials produce a
 *       success flash; invalid or empty credentials produce an error flash.</li>
 *   <li><strong>Checkboxes</strong> – initial checked/unchecked state, toggle
 *       behaviour, and total widget count.</li>
 *   <li><strong>Dropdown</strong> – default selection, explicit option selection,
 *       and full option enumeration.</li>
 *   <li><strong>Number inputs</strong> – integer entry, negative value entry, and
 *       field-clear behaviour.</li>
 *   <li><strong>JavaScript alerts</strong> – alert accept, confirm accept/dismiss,
 *       and prompt with custom text.</li>
 * </ul>
 *
 * <p>Each test method instantiates its page object fresh so tests are completely
 * independent and safe to run in any order or in parallel. Assertions use
 * AssertJ exclusively; no TestNG {@code Assert} calls are present.</p>
 */
@Epic("The-Internet")
@Feature("UI Mechanics")
public class FormAndAlertMechanicsTest extends BaseTest {

    // -------------------------------------------------------------------------
    // Login authentication
    // -------------------------------------------------------------------------

    @Test(groups = {"ui", "regression"})
    @Story("Login")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Valid credentials produce a green success flash message.")
    public void validLoginShowsSuccessFlash() {
        final TheInternetLoginPage loginPage = new TheInternetLoginPage().open();
        loginPage.login("tomsmith", "SuperSecretPassword!");

        assertThat(loginPage.isFlashSuccess())
                .as("success flash should be displayed after valid login")
                .isTrue();
    }

    @Test(groups = {"ui", "regression"})
    @Story("Login")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Wrong password produces an error flash containing 'Your username is invalid!'.")
    public void invalidLoginShowsErrorFlash() {
        final TheInternetLoginPage loginPage = new TheInternetLoginPage().open();
        loginPage.login("tomsmith", "WrongPassword!");

        assertThat(loginPage.isFlashError())
                .as("error flash should be displayed after invalid login")
                .isTrue();
        assertThat(loginPage.getFlashMessage())
                .as("flash message text")
                .contains("Your username is invalid!");
    }

    @Test(groups = {"ui", "regression"})
    @Story("Login")
    @Severity(SeverityLevel.NORMAL)
    @Description("Submitting empty username and password produces an error flash.")
    public void emptyCredentialsShowsError() {
        final TheInternetLoginPage loginPage = new TheInternetLoginPage().open();
        loginPage.login("", "");

        assertThat(loginPage.isFlashError())
                .as("error flash should be displayed when credentials are empty")
                .isTrue();
    }

    // -------------------------------------------------------------------------
    // Checkboxes
    // -------------------------------------------------------------------------

    @Test(groups = {"ui", "regression"})
    @Story("Checkboxes")
    @Severity(SeverityLevel.NORMAL)
    @Description("The first checkbox starts in the unchecked state by default.")
    public void firstCheckboxIsUnchecked() {
        final CheckboxesPage checkboxesPage = new CheckboxesPage().open();

        assertThat(checkboxesPage.isChecked(0))
                .as("first checkbox (index 0) initial state")
                .isFalse();
    }

    @Test(groups = {"ui", "regression"})
    @Story("Checkboxes")
    @Severity(SeverityLevel.NORMAL)
    @Description("The second checkbox starts in the checked state by default.")
    public void secondCheckboxIsChecked() {
        final CheckboxesPage checkboxesPage = new CheckboxesPage().open();

        assertThat(checkboxesPage.isChecked(1))
                .as("second checkbox (index 1) initial state")
                .isTrue();
    }

    @Test(groups = {"ui", "regression"})
    @Story("Checkboxes")
    @Severity(SeverityLevel.NORMAL)
    @Description("Toggling a checkbox inverts its checked state.")
    public void toggleCheckboxChangesState() {
        final CheckboxesPage checkboxesPage = new CheckboxesPage().open();
        final boolean stateBefore = checkboxesPage.isChecked(0);

        checkboxesPage.toggle(0);

        assertThat(checkboxesPage.isChecked(0))
                .as("checkbox state after toggle")
                .isNotEqualTo(stateBefore);
    }

    @Test(groups = {"ui", "regression"})
    @Story("Checkboxes")
    @Severity(SeverityLevel.MINOR)
    @Description("The checkboxes page contains exactly two checkbox controls.")
    public void checkboxCountIsTwo() {
        final CheckboxesPage checkboxesPage = new CheckboxesPage().open();

        assertThat(checkboxesPage.getCheckboxCount())
                .as("total number of checkboxes on the page")
                .isEqualTo(2);
    }

    // -------------------------------------------------------------------------
    // Dropdown
    // -------------------------------------------------------------------------

    @Test(groups = {"ui", "regression"})
    @Story("Dropdown")
    @Severity(SeverityLevel.NORMAL)
    @Description("On page load the dropdown shows a disabled placeholder option.")
    public void dropdownDefaultOptionIsDisabled() {
        final DropdownPage dropdownPage = new DropdownPage().open();

        assertThat(dropdownPage.getSelectedOption())
                .as("default selected dropdown option")
                .containsIgnoringCase("Please select an option");
    }

    @Test(groups = {"ui", "regression"})
    @Story("Dropdown")
    @Severity(SeverityLevel.NORMAL)
    @Description("Selecting 'Option 1' updates the displayed selection to 'Option 1'.")
    public void selectOption1FromDropdown() {
        final DropdownPage dropdownPage = new DropdownPage().open();
        dropdownPage.selectOption("Option 1");

        assertThat(dropdownPage.getSelectedOption())
                .as("selected option after choosing Option 1")
                .isEqualTo("Option 1");
    }

    @Test(groups = {"ui", "regression"})
    @Story("Dropdown")
    @Severity(SeverityLevel.NORMAL)
    @Description("Selecting 'Option 2' updates the displayed selection to 'Option 2'.")
    public void selectOption2FromDropdown() {
        final DropdownPage dropdownPage = new DropdownPage().open();
        dropdownPage.selectOption("Option 2");

        assertThat(dropdownPage.getSelectedOption())
                .as("selected option after choosing Option 2")
                .isEqualTo("Option 2");
    }

    @Test(groups = {"ui", "regression"})
    @Story("Dropdown")
    @Severity(SeverityLevel.MINOR)
    @Description("The dropdown exposes both expected selectable options.")
    public void dropdownHasExpectedOptions() {
        final DropdownPage dropdownPage = new DropdownPage().open();

        assertThat(dropdownPage.getAllOptions())
                .as("all options available in the dropdown")
                .contains("Option 1", "Option 2");
    }

    // -------------------------------------------------------------------------
    // Number inputs
    // -------------------------------------------------------------------------

    @Test(groups = {"ui", "regression"})
    @Story("Number Inputs")
    @Severity(SeverityLevel.NORMAL)
    @Description("A positive integer typed into the input is reflected as the field value.")
    public void numberInputAcceptsInteger() {
        final InputsPage inputsPage = new InputsPage().open();
        inputsPage.setNumber("42");

        assertThat(inputsPage.getInputValue())
                .as("input value after typing '42'")
                .isEqualTo("42");
    }

    @Test(groups = {"ui", "regression"})
    @Story("Number Inputs")
    @Severity(SeverityLevel.NORMAL)
    @Description("A negative integer typed into the input is reflected as the field value.")
    public void numberInputAcceptsNegative() {
        final InputsPage inputsPage = new InputsPage().open();
        inputsPage.setNumber("-5");

        assertThat(inputsPage.getInputValue())
                .as("input value after typing '-5'")
                .isEqualTo("-5");
    }

    @Test(groups = {"ui", "regression"})
    @Story("Number Inputs")
    @Severity(SeverityLevel.MINOR)
    @Description("Clearing the input field results in an empty or blank value.")
    public void numberInputClearReturnsEmpty() {
        final InputsPage inputsPage = new InputsPage().open();
        inputsPage.setNumber("99");
        inputsPage.clearInput();

        assertThat(inputsPage.getInputValue())
                .as("input value after clear")
                .isBlank();
    }

    // -------------------------------------------------------------------------
    // JavaScript alerts
    // -------------------------------------------------------------------------

    @Test(groups = {"ui", "regression"})
    @Story("JavaScript Alerts")
    @Severity(SeverityLevel.NORMAL)
    @Description("Accepting a simple alert produces a success result message.")
    public void alertAcceptCapturesText() {
        final JavaScriptAlertsPage alertsPage = new JavaScriptAlertsPage().open();
        alertsPage.clickAlertButton();

        final String alertText = alertsPage.getAlertText();
        assertThat(alertText)
                .as("alert dialog text")
                .isNotEmpty();

        alertsPage.acceptAlert();

        assertThat(alertsPage.getResultText())
                .as("result text after accepting alert")
                .contains("You successfully clicked an alert");
    }

    @Test(groups = {"ui", "regression"})
    @Story("JavaScript Alerts")
    @Severity(SeverityLevel.NORMAL)
    @Description("Accepting a confirm dialog produces 'You clicked: Ok' in the result.")
    public void confirmAcceptedShowsResult() {
        final JavaScriptAlertsPage alertsPage = new JavaScriptAlertsPage().open();
        alertsPage.clickConfirmButton();
        alertsPage.acceptAlert();

        assertThat(alertsPage.getResultText())
                .as("result text after accepting confirm")
                .contains("You clicked: Ok");
    }

    @Test(groups = {"ui", "regression"})
    @Story("JavaScript Alerts")
    @Severity(SeverityLevel.NORMAL)
    @Description("Dismissing a confirm dialog produces 'You clicked: Cancel' in the result.")
    public void confirmDismissedShowsResult() {
        final JavaScriptAlertsPage alertsPage = new JavaScriptAlertsPage().open();
        alertsPage.clickConfirmButton();
        alertsPage.dismissAlert();

        assertThat(alertsPage.getResultText())
                .as("result text after dismissing confirm")
                .contains("You clicked: Cancel");
    }

    @Test(groups = {"ui", "regression"})
    @Story("JavaScript Alerts")
    @Severity(SeverityLevel.NORMAL)
    @Description("Entering text in a prompt dialog echoes that text back in the result.")
    public void promptWithTextShowsResult() {
        final JavaScriptAlertsPage alertsPage = new JavaScriptAlertsPage().open();
        alertsPage.clickPromptButton();
        alertsPage.sendTextToPrompt("Hello World");
        alertsPage.acceptAlert();

        assertThat(alertsPage.getResultText())
                .as("result text after entering text in prompt")
                .contains("You entered: Hello World");
    }
}
