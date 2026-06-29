package com.omiinqa.utils.data;

import com.omiinqa.exceptions.DataException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Programmatic Apache POI workbook generator for seeding Excel test data.
 *
 * <p><strong>Why this class exists:</strong> A {@code .xlsx} binary cannot be
 * reasonably authored by hand or committed as plain-text source. This utility
 * writes a well-structured sample workbook at runtime so that
 * {@link ExcelDataReader} always has a real {@code .xlsx} to exercise against.
 * It is typically called from a TestNG {@code @BeforeSuite} method or a
 * one-off setup script; the generated file is placed under
 * {@code target/testdata/} and never committed to version control.</p>
 *
 * <p><strong>Pattern:</strong> Builder-style static factory — each sheet is
 * defined by a simple {@code String[][]} (header row first, data rows after)
 * and appended via {@link #addSheet}. Call {@link #write} when done.</p>
 *
 * <p><strong>Usage (from a @BeforeSuite method):</strong>
 * <pre>{@code
 *   Path xlsxPath = ExcelWorkbookGenerator.create()
 *       .addSheet("Users", new String[][] {
 *           {"id", "firstName", "lastName", "email"},
 *           {"1", "Alice",     "Smith",    "alice@example.com"},
 *           {"2", "Bob",       "Jones",    "bob@example.com"}
 *       })
 *       .addSheet("Login", new String[][] {
 *           {"username",        "password",    "expected"},
 *           {"standard_user",  "secret_sauce", "pass"},
 *           {"locked_out_user","secret_sauce", "fail"}
 *       })
 *       .write(Paths.get("target/testdata/test-data-sheet.xlsx"));
 *
 *   // Then read with:
 *   List<Map<String,String>> users =
 *       ExcelDataReader.readSheet("testdata/test-data-sheet.xlsx", "Users");
 * }</pre>
 * </p>
 */
public final class ExcelWorkbookGenerator {

    private static final Logger log = LoggerFactory.getLogger(ExcelWorkbookGenerator.class);

    private final Workbook workbook;

    private ExcelWorkbookGenerator() {
        this.workbook = new XSSFWorkbook();
    }

    // -------------------------------------------------------------------------
    // Factory entry point
    // -------------------------------------------------------------------------

    /**
     * Creates a new, empty workbook generator.
     *
     * @return fresh generator instance
     */
    public static ExcelWorkbookGenerator create() {
        return new ExcelWorkbookGenerator();
    }

    // -------------------------------------------------------------------------
    // Fluent builder methods
    // -------------------------------------------------------------------------

    /**
     * Appends a new sheet populated with the given data table.
     *
     * <p>Row 0 of {@code data} is treated as the header and rendered with a
     * distinct style (bold, grey background). Subsequent rows are plain data
     * rows. Columns are auto-sized after population.</p>
     *
     * @param sheetName the worksheet tab name
     * @param data      two-dimensional string table; row 0 is the header
     * @return {@code this} for chaining
     * @throws DataException if a sheet with the same name already exists
     */
    public ExcelWorkbookGenerator addSheet(final String sheetName, final String[][] data) {
        if (workbook.getSheet(sheetName) != null) {
            throw new DataException("Sheet '" + sheetName + "' already exists in workbook");
        }
        log.debug("Adding sheet '{}' with {} rows", sheetName, data.length);
        Sheet sheet = workbook.createSheet(sheetName);
        CellStyle headerStyle = buildHeaderStyle();

        for (int r = 0; r < data.length; r++) {
            Row row = sheet.createRow(r);
            String[] rowData = data[r];
            for (int c = 0; c < rowData.length; c++) {
                Cell cell = row.createCell(c);
                cell.setCellValue(rowData[c]);
                if (r == 0) {
                    cell.setCellStyle(headerStyle);
                }
            }
        }

        // Auto-size all columns based on content
        if (data.length > 0) {
            for (int c = 0; c < data[0].length; c++) {
                sheet.autoSizeColumn(c);
            }
        }
        return this;
    }

    /**
     * Writes the accumulated workbook to the given path, creating any missing
     * parent directories.
     *
     * @param destination target file path (e.g. {@code Paths.get("target/testdata/gen.xlsx")})
     * @return the resolved destination path for chaining with {@link ExcelDataReader}
     * @throws DataException if the file cannot be written
     */
    public Path write(final Path destination) {
        try {
            Files.createDirectories(destination.getParent());
            try (OutputStream os = Files.newOutputStream(destination);
                 Workbook wb = workbook) {
                wb.write(os);
            }
            log.info("Workbook written to '{}'", destination.toAbsolutePath());
            return destination;
        } catch (IOException e) {
            throw new DataException("Failed to write workbook to '" + destination + "'", e);
        }
    }

    /**
     * Convenience overload accepting a {@code String} path.
     *
     * @param destinationPath string path to the target {@code .xlsx} file
     * @return the resolved destination path
     */
    public Path write(final String destinationPath) {
        return write(Paths.get(destinationPath));
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private CellStyle buildHeaderStyle() {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }
}
