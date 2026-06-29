package com.omiinqa.components;

import com.omiinqa.core.BaseComponent;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Generic wrapper around a native HTML {@code <select>} element.
 *
 * <p><b>Why wrap {@link Select}?</b> Selenium's built-in {@code Select} class
 * works well but exposes no logging, no scoped root, and returns raw
 * {@link WebElement} lists. By wrapping it in a {@link BaseComponent}, callers
 * get a uniform, logged API consistent with every other component in the
 * framework. The root element <em>must</em> be a {@code <select>}; the
 * component creates the {@link Select} lazily so StaleElementReferenceException
 * recovery can reconstruct it.</p>
 */
public class DropdownComponent extends BaseComponent {

    /**
     * @param selectElement the {@code <select>} WebElement; must be the
     *                      {@code <select>} itself, not a wrapper div
     */
    public DropdownComponent(final WebElement selectElement) {
        super(selectElement);
    }

    // ----------------------------------------------------------------- helpers

    private Select select() {
        return new Select(root);
    }

    // ----------------------------------------------------------------- queries

    /**
     * @return the currently selected option's visible text
     */
    public String getSelectedText() {
        return select().getFirstSelectedOption().getText().trim();
    }

    /**
     * @return ordered list of all option labels
     */
    public List<String> getAllOptions() {
        return select().getOptions().stream()
                .map(el -> el.getText().trim())
                .collect(Collectors.toList());
    }

    /**
     * @return {@code true} when the dropdown allows multiple selections
     */
    public boolean isMultiSelect() {
        return select().isMultiple();
    }

    // ----------------------------------------------------------------- actions

    /**
     * Selects the option whose visible text matches exactly.
     *
     * @param text the visible text of the option to select
     */
    public void selectByVisibleText(final String text) {
        log.debug("Selecting dropdown option by text: '{}'", text);
        select().selectByVisibleText(text);
    }

    /**
     * Selects the option at the given 0-based index.
     *
     * @param index 0-based option index
     */
    public void selectByIndex(final int index) {
        log.debug("Selecting dropdown option at index: {}", index);
        select().selectByIndex(index);
    }

    /**
     * Selects the option whose {@code value} attribute equals the given string.
     *
     * @param value the {@code value} attribute to match
     */
    public void selectByValue(final String value) {
        log.debug("Selecting dropdown option by value: '{}'", value);
        select().selectByValue(value);
    }

    /**
     * Deselects all options (only valid for multi-select dropdowns).
     */
    public void deselectAll() {
        log.debug("Deselecting all dropdown options");
        select().deselectAll();
    }

    /**
     * Convenience factory: creates a {@link DropdownComponent} from a By locator,
     * waiting for the element to be present before wrapping it.
     *
     * @param locator the locator for the {@code <select>} element
     * @return a new DropdownComponent bound to that element
     */
    public static DropdownComponent from(final By locator,
                                         final org.openqa.selenium.WebDriver driver) {
        final WebElement el = com.omiinqa.utils.WaitUtils.visible(driver, locator);
        return new DropdownComponent(el);
    }
}
