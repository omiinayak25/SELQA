package com.omiinqa.pages.theinternet;

import com.omiinqa.components.TableComponent;
import com.omiinqa.config.FrameworkConfig;
import com.omiinqa.core.BasePage;
import com.omiinqa.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;

/**
 * Page Object for the Sortable Data Tables exercise on The-Internet
 * ({@code https://the-internet.herokuapp.com/tables}).
 *
 * <p>The page renders two sortable HTML tables ({@code #table1} and
 * {@code #table2}). Column headers are anchor links wrapped inside
 * {@code <th>} cells; clicking a header link sorts the column in
 * ascending / descending order.</p>
 *
 * <p>Table data is exposed through the reusable {@link TableComponent}, which
 * provides row counts, cell text, column values, and row searches without
 * duplicating table-navigation logic in this class.</p>
 *
 * <p><b>Page Object Model contract:</b></p>
 * <ul>
 *   <li>No assertions — all state is surfaced through accessor methods so this
 *       class is reusable across positive, negative, and boundary test cases.</li>
 *   <li>Locators are {@code private static final} {@link By} fields — declared
 *       once, named for intent, impossible to scatter or duplicate.</li>
 *   <li>Synchronisation is delegated to {@link WaitUtils}; no raw
 *       {@code Thread.sleep} calls appear here.</li>
 * </ul>
 */
public class SortableDataTablesPage extends BasePage {

    // ---------------------------------------------------------------- locators

    /** Root element of the first sortable table. */
    private static final By TABLE1_ROOT = By.cssSelector("#table1");

    /** Root element of the second sortable table. */
    private static final By TABLE2_ROOT = By.cssSelector("#table2");

    // ----------------------------------------------------------------- actions

    /**
     * Navigates the browser to the Sortable Data Tables page URL as configured
     * in {@link FrameworkConfig#appUrl(String)} with the {@code /tables}
     * path appended.
     *
     * @return this {@link SortableDataTablesPage} for method chaining
     */
    public SortableDataTablesPage open() {
        final String url = FrameworkConfig.get().appUrl("theinternet") + "/tables";
        log.info("Opening Sortable Data Tables page: {}", url);
        driver().get(url);
        waitForUrlContains("/tables");
        return this;
    }

    /**
     * Clicks the column header link identified by {@code headerText} inside the
     * table specified by {@code tableId}.
     *
     * <p>Table headers on this page are anchor elements nested inside {@code <th>}
     * cells ({@code <th><a href="…">Header Text</a></th>}). Clicking the anchor
     * triggers a client-side sort. Successive calls toggle the sort direction.</p>
     *
     * @param tableId    the {@code id} attribute of the target table element
     *                   (e.g. {@code "table1"} or {@code "table2"})
     * @param headerText the exact visible text of the column header link to click
     *                   (e.g. {@code "Last Name"}, {@code "Email"})
     */
    public SortableDataTablesPage clickColumnHeader(final String tableId,
                                                    final String headerText) {
        log.info("Clicking column header '{}' in table '#{}'", headerText, tableId);
        final By headerLinkLocator = By.xpath(
                String.format("//*[@id='%s']//th/a[normalize-space(text())='%s']",
                        tableId, headerText));
        final WebElement headerLink = WaitUtils.clickable(driver(), headerLinkLocator);
        headerLink.click();
        return this;
    }

    // ----------------------------------------------------------------- queries

    /**
     * Returns a {@link TableComponent} wrapping the first sortable table
     * ({@code #table1}).
     *
     * <p>The component is constructed fresh on each call to reflect the current
     * DOM state, particularly after sorting operations that reorder rows.</p>
     *
     * @return a new {@link TableComponent} rooted at {@code #table1}
     */
    public TableComponent getTable1() {
        log.debug("Obtaining TableComponent for #table1");
        final WebElement root = WaitUtils.present(driver(), TABLE1_ROOT);
        return new TableComponent(root);
    }

    /**
     * Returns a {@link TableComponent} wrapping the second sortable table
     * ({@code #table2}).
     *
     * <p>The component is constructed fresh on each call to reflect the current
     * DOM state, particularly after sorting operations that reorder rows.</p>
     *
     * @return a new {@link TableComponent} rooted at {@code #table2}
     */
    public TableComponent getTable2() {
        log.debug("Obtaining TableComponent for #table2");
        final WebElement root = WaitUtils.present(driver(), TABLE2_ROOT);
        return new TableComponent(root);
    }

    /**
     * Returns the list of column header labels for the table identified by
     * {@code tableId}.
     *
     * <p>On this page, headers contain anchor text so the values are read from
     * the visible text of each {@code <th>} — the {@link TableComponent}
     * already trims and collects them through its {@code getColumnHeaders()}
     * method.</p>
     *
     * @param tableId the {@code id} attribute of the target table element
     * @return ordered list of header label strings; never {@code null}
     */
    public List<String> getColumnHeaders(final String tableId) {
        final By rootLocator = By.cssSelector("#" + tableId);
        final WebElement root = WaitUtils.present(driver(), rootLocator);
        return new TableComponent(root).getColumnHeaders();
    }
}
