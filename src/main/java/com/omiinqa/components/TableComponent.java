package com.omiinqa.components;

import com.omiinqa.core.BaseComponent;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Generic HTML table component that works with any standard
 * {@code <table>/<thead>/<tbody>/<tr>/<th>/<td>} structure.
 *
 * <p><b>Genericity rationale:</b> Rather than writing table-reading code in
 * every page object that renders tabular data, this component is constructed
 * once with the table root and exposes a stable API
 * ({@link #getCellText(int, int)}, {@link #getColumnValues(String)}, etc.).
 * Pages compose it by passing the relevant root {@link WebElement}.</p>
 *
 * <p>Row and column indices are <strong>1-based</strong> to align with QA
 * convention ("row 1, col 2") rather than programmer convention.</p>
 */
public class TableComponent extends BaseComponent {

    private static final By HEADER_CELLS = By.cssSelector("thead th");
    private static final By BODY_ROWS    = By.cssSelector("tbody tr");

    /**
     * @param root the {@code <table>} element; all locators are relative to it
     */
    public TableComponent(final WebElement root) {
        super(root);
    }

    // ----------------------------------------------------------------- queries

    /**
     * @return the number of data rows in {@code <tbody>} (does not count header)
     */
    public int getRowCount() {
        final List<WebElement> rows = findAllInRoot(BODY_ROWS);
        return rows.size();
    }

    /**
     * Returns the trimmed text of a cell identified by 1-based row and column.
     *
     * @param row    1-based row index within {@code <tbody>}
     * @param column 1-based column index
     * @return cell text
     * @throws IndexOutOfBoundsException when row/column is out of range
     */
    public String getCellText(final int row, final int column) {
        final List<WebElement> rows = findAllInRoot(BODY_ROWS);
        final WebElement tr = rows.get(row - 1);
        final List<WebElement> cells = tr.findElements(By.tagName("td"));
        return cells.get(column - 1).getText().trim();
    }

    /**
     * Returns all cell values in the column identified by its header text.
     *
     * @param headerText the exact (trimmed) text of the {@code <th>} to look up
     * @return ordered list of cell values in that column; empty if header not found
     */
    public List<String> getColumnValues(final String headerText) {
        final List<WebElement> headers = findAllInRoot(HEADER_CELLS);
        int colIndex = -1;
        for (int i = 0; i < headers.size(); i++) {
            if (headers.get(i).getText().trim().equalsIgnoreCase(headerText)) {
                colIndex = i;
                break;
            }
        }
        if (colIndex < 0) {
            log.warn("Column header '{}' not found in table", headerText);
            return List.of();
        }
        final int finalColIndex = colIndex;
        return findAllInRoot(BODY_ROWS).stream()
                .map(tr -> {
                    final List<WebElement> cells = tr.findElements(By.tagName("td"));
                    return finalColIndex < cells.size()
                            ? cells.get(finalColIndex).getText().trim() : "";
                })
                .collect(Collectors.toList());
    }

    /**
     * Returns the first row whose any cell contains the supplied text
     * (case-insensitive substring match).
     *
     * @param text the text to search for
     * @return an {@link Optional} wrapping the matching row {@link WebElement},
     *         or empty when not found
     */
    public Optional<WebElement> findRowByText(final String text) {
        return findAllInRoot(BODY_ROWS).stream()
                .filter(tr -> tr.getText().toLowerCase()
                        .contains(text.toLowerCase()))
                .findFirst();
    }

    /**
     * @return list of column header labels in document order
     */
    public List<String> getColumnHeaders() {
        return findAllInRoot(HEADER_CELLS).stream()
                .map(el -> el.getText().trim())
                .collect(Collectors.toList());
    }
}
