package com.omiinqa.pages.theinternet;

import com.omiinqa.config.FrameworkConfig;
import com.omiinqa.core.BasePage;
import com.omiinqa.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.Select;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Page Object for the The-Internet dropdown exercise
 * ({@code https://the-internet.herokuapp.com/dropdown}).
 *
 * <p><b>Design decisions:</b></p>
 * <ul>
 *   <li>{@link Select} is instantiated on every getter/action against the element
 *       to remain resilient to DOM re-renders between calls; the element is
 *       re-fetched from the live DOM via {@link WaitUtils#visible} each time.</li>
 *   <li>{@link #getAllOptions()} excludes the placeholder option whose value is
 *       {@code ""} so callers receive only the meaningful choices.</li>
 *   <li>No assertions are included — callers use the string getters to build
 *       verification logic.</li>
 * </ul>
 *
 * <p>Always call {@link #open()} at the start of a test to navigate to a known
 * state.</p>
 */
public class DropdownPage extends BasePage {

    // ---------------------------------------------------------------- locators

    /** The native {@code <select>} element. */
    private static final By DROPDOWN = By.id("dropdown");

    // ----------------------------------------------------------------- actions

    /**
     * Navigates the browser to the dropdown page URL as configured in
     * {@code FrameworkConfig.get().appUrl("theinternet")} with the
     * {@code /dropdown} path appended.
     *
     * @return this {@link DropdownPage} for method chaining
     */
    public DropdownPage open() {
        final String url = FrameworkConfig.get().appUrl("theinternet") + "/dropdown";
        log.info("Opening The-Internet dropdown page: {}", url);
        driver().get(url);
        waitForUrlContains("/dropdown");
        return this;
    }

    /**
     * Selects an option from the dropdown by its visible (displayed) text, using
     * the framework's {@link com.omiinqa.core.BasePage#selectFromDropdownByVisibleText}
     * helper.
     *
     * @param visibleText the exact text shown in the option element
     *                    (e.g. {@code "Option 1"})
     */
    public void selectOption(final String visibleText) {
        log.info("Selecting dropdown option: '{}'", visibleText);
        selectFromDropdownByVisibleText(DROPDOWN, visibleText);
    }

    // ----------------------------------------------------------------- queries

    /**
     * Returns the visible text of the currently selected option in the dropdown.
     *
     * <p>If no option has been explicitly chosen, the first option in the list
     * (the placeholder) is returned per standard HTML {@code <select>} semantics.</p>
     *
     * @return trimmed text of the first selected {@code <option>}; never
     *         {@code null}
     */
    public String getSelectedOption() {
        final Select select = new Select(WaitUtils.visible(driver(), DROPDOWN));
        return select.getFirstSelectedOption().getText().trim();
    }

    /**
     * Returns the visible text of every option in the dropdown, including the
     * placeholder option (if any) that has an empty {@code value} attribute.
     *
     * <p>Use this method to validate the complete list of choices without relying
     * on hard-coded index positions.</p>
     *
     * @return ordered list of all option texts; never {@code null}
     */
    public List<String> getAllOptions() {
        final Select select = new Select(WaitUtils.visible(driver(), DROPDOWN));
        return select.getOptions().stream()
                .map(el -> el.getText().trim())
                .collect(Collectors.toList());
    }
}
