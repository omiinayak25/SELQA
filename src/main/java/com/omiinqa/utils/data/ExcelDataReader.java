package com.omiinqa.utils.data;

import com.omiinqa.exceptions.DataException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Classpath-based Excel (.xlsx) test data reader backed by Apache POI.
 *
 * <p><strong>Pattern:</strong> Utility / Static Factory — all methods are
 * stateless; the workbook is opened, read, and closed within each call so
 * callers never manage POI resources directly.</p>
 *
 * <p>Two consumption shapes are provided:
 * <ol>
 *   <li>{@code readSheet} → {@code List<Map<String,String>>} — header-aware;
 *       the first row is treated as column names, subsequent rows are mapped
 *       {@code header → cell value}. Ideal for config-driven tests.</li>
 *   <li>{@code toDataProvider} → {@code Object[][]} — direct feed for TestNG
 *       {@code @DataProvider}; first row is the header and is excluded.</li>
 * </ol>
 * </p>
 *
 * <p>Cell-type handling: string, numeric (including dates — surfaced as the
 * ISO-8601 string representation), boolean, formula (evaluated to string),
 * and blank/null (mapped to {@code ""}) are all supported.</p>
 *
 * <p><strong>Note on binary test data:</strong> A hand-authored {@code .xlsx}
 * cannot be checked in as plain text source. Use
 * {@link ExcelWorkbookGenerator} to write a sample workbook at test startup
 * and then point this reader at the generated file path. The Javadoc on
 * {@link ExcelWorkbookGenerator} explains the integration.</p>
 *
 * <p><strong>Usage:</strong>
 * <pre>{@code
 *   // Read by sheet name
 *   List<Map<String,String>> rows =
 *       ExcelDataReader.readSheet("testdata/test-data-sheet.xlsx", "Users");
 *
 *   // TestNG DataProvider
 *   Object[][] dp =
 *       ExcelDataReader.toDataProvider("testdata/test-data-sheet.xlsx", "Login");
 * }</pre>
 * </p>
 */
public final class ExcelDataReader {

    private static final Logger log = LoggerFactory.getLogger(ExcelDataReader.class);

    private ExcelDataReader() {
        // utility — not instantiable
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Reads all data rows from the named sheet and returns them as a list of
     * header-keyed maps.
     *
     * <p>Row 0 is consumed as the header; all subsequent rows become entries.
     * Completely blank rows (all cells empty) are skipped.</p>
     *
     * @param classpath classpath-relative path to the {@code .xlsx} file
     * @param sheetName exact name of the worksheet tab
     * @return ordered list of row maps; never {@code null}
     * @throws DataException if the resource is absent, the sheet is not found,
     *                       or the workbook cannot be opened
     */
    public static List<Map<String, String>> readSheet(final String classpath,
                                                       final String sheetName) {
        log.debug("Reading Excel sheet '{}' from '{}'", sheetName, classpath);
        try (Workbook wb = openWorkbook(classpath)) {
            Sheet sheet = resolveSheet(wb, sheetName, classpath);
            return parseSheet(sheet);
        } catch (IOException e) {
            throw new DataException(
                    "Failed to close workbook '" + classpath + "'", e);
        }
    }

    /**
     * Reads all data rows from a sheet identified by its zero-based index.
     *
     * @param classpath  classpath-relative path to the {@code .xlsx} file
     * @param sheetIndex zero-based sheet index
     * @return ordered list of row maps; never {@code null}
     * @throws DataException if the resource is absent or the index is out of range
     */
    public static List<Map<String, String>> readSheet(final String classpath,
                                                       final int sheetIndex) {
        log.debug("Reading Excel sheet index {} from '{}'", sheetIndex, classpath);
        try (Workbook wb = openWorkbook(classpath)) {
            Sheet sheet = wb.getSheetAt(sheetIndex);
            return parseSheet(sheet);
        } catch (IOException e) {
            throw new DataException(
                    "Failed to close workbook '" + classpath + "'", e);
        }
    }

    /**
     * Returns sheet data as {@code Object[][]} for TestNG {@code @DataProvider}.
     *
     * <p>Row 0 (header) is excluded. Each subsequent row becomes one
     * {@code Object[]} of {@code String} values in column order.</p>
     *
     * @param classpath classpath-relative path to the {@code .xlsx} file
     * @param sheetName exact sheet name
     * @return two-dimensional array; may be empty but never {@code null}
     * @throws DataException if the resource is absent or the sheet is not found
     */
    public static Object[][] toDataProvider(final String classpath, final String sheetName) {
        log.debug("Building Object[][] from Excel sheet '{}' in '{}'", sheetName, classpath);
        try (Workbook wb = openWorkbook(classpath)) {
            Sheet sheet = resolveSheet(wb, sheetName, classpath);
            return sheetToDataProvider(sheet);
        } catch (IOException e) {
            throw new DataException(
                    "Failed to close workbook '" + classpath + "'", e);
        }
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private static Workbook openWorkbook(final String classpath) {
        InputStream is = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(classpath);
        if (is == null) {
            is = ExcelDataReader.class.getResourceAsStream("/" + classpath);
        }
        if (is == null) {
            throw new DataException("Classpath resource not found: '" + classpath + "'");
        }
        try {
            return new XSSFWorkbook(is);
        } catch (IOException e) {
            throw new DataException("Cannot open workbook '" + classpath + "'", e);
        }
    }

    private static Sheet resolveSheet(final Workbook wb, final String name,
                                       final String classpath) {
        Sheet sheet = wb.getSheet(name);
        if (sheet == null) {
            throw new DataException(
                    "Sheet '" + name + "' not found in workbook '" + classpath + "'");
        }
        return sheet;
    }

    private static List<Map<String, String>> parseSheet(final Sheet sheet) {
        List<Map<String, String>> result = new ArrayList<>();
        Iterator<Row> rowIter = sheet.rowIterator();
        if (!rowIter.hasNext()) {
            return result;
        }
        // First row = headers
        Row headerRow = rowIter.next();
        String[] headers = extractHeaders(headerRow);

        while (rowIter.hasNext()) {
            Row row = rowIter.next();
            Map<String, String> map = new LinkedHashMap<>(headers.length * 2);
            boolean hasData = false;
            for (int c = 0; c < headers.length; c++) {
                Cell cell = row.getCell(c, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                String value = cellToString(cell);
                map.put(headers[c], value);
                if (!value.isEmpty()) {
                    hasData = true;
                }
            }
            if (hasData) {
                result.add(map);
            }
        }
        return result;
    }

    private static Object[][] sheetToDataProvider(final Sheet sheet) {
        List<Map<String, String>> rows = parseSheet(sheet);
        if (rows.isEmpty()) {
            return new Object[0][0];
        }
        // Determine column count from first row
        int cols = rows.get(0).size();
        Object[][] data = new Object[rows.size()][cols];
        for (int r = 0; r < rows.size(); r++) {
            int c = 0;
            for (String val : rows.get(r).values()) {
                data[r][c++] = val;
            }
        }
        return data;
    }

    private static String[] extractHeaders(final Row headerRow) {
        int lastCell = headerRow.getLastCellNum();
        String[] headers = new String[lastCell];
        for (int c = 0; c < lastCell; c++) {
            Cell cell = headerRow.getCell(c, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            headers[c] = cell != null ? cell.getStringCellValue().trim() : "col" + c;
        }
        return headers;
    }

    /**
     * Converts a POI {@link Cell} to its string representation regardless of type.
     *
     * <p>Numeric cells that are date-formatted are converted to ISO-8601 date strings.
     * Formula cells are evaluated to their cached string result. Blank/null cells
     * return {@code ""}.</p>
     */
    static String cellToString(final Cell cell) {
        if (cell == null) {
            return "";
        }
        CellType type = cell.getCellType();
        if (type == CellType.FORMULA) {
            type = cell.getCachedFormulaResultType();
        }
        return switch (type) {
            case STRING  -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
                double d = cell.getNumericCellValue();
                // Render integers without trailing ".0"
                yield (d == Math.floor(d) && !Double.isInfinite(d))
                        ? String.valueOf((long) d)
                        : String.valueOf(d);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default      -> "";
        };
    }
}
