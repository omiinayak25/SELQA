package com.omiinqa.utils.data;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.RFC4180Parser;
import com.opencsv.RFC4180ParserBuilder;
import com.opencsv.exceptions.CsvException;
import com.omiinqa.exceptions.DataException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Classpath-based CSV test data reader backed by OpenCSV.
 *
 * <p><strong>Pattern:</strong> Utility / Static Factory — zero state, all
 * behaviour expressed through overloaded factory methods so test authors
 * call a single line to get typed data structures.</p>
 *
 * <p>Three consumption shapes are provided to match common test scenarios:
 * <ol>
 *   <li>{@code readRaw} → {@code List<String[]>} — useful when column
 *       count is dynamic or a custom mapping is preferred.</li>
 *   <li>{@code readMaps} → {@code List<Map<String,String>>} — header-aware;
 *       keys are the CSV column headers so downstream code can access data
 *       by meaningful name rather than index.</li>
 *   <li>{@code toDataProvider} → {@code Object[][]} — direct feed for
 *       TestNG {@code @DataProvider} methods; each row becomes one
 *       {@code Object[]} of raw strings.</li>
 * </ol>
 * </p>
 *
 * <p>Files are resolved from the classpath root. Place CSVs under
 * {@code src/test/resources/testdata/} and reference as
 * {@code "testdata/login-data.csv"}.</p>
 *
 * <p>Failures surface as {@link DataException} so test suites fail fast
 * with a clear data-layer message.</p>
 *
 * <p><strong>Usage:</strong>
 * <pre>{@code
 *   // Raw rows (no header stripping)
 *   List<String[]> rows = CsvDataReader.readRaw("testdata/login-data.csv");
 *
 *   // Header-mapped rows
 *   List<Map<String,String>> data = CsvDataReader.readMaps("testdata/login-data.csv");
 *
 *   // TestNG DataProvider
 *   Object[][] dp = CsvDataReader.toDataProvider("testdata/checkout-customers.csv");
 * }</pre>
 * </p>
 */
public final class CsvDataReader {

    private static final Logger log = LoggerFactory.getLogger(CsvDataReader.class);

    private CsvDataReader() {
        // utility — not instantiable
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Returns every row (including header if present) as a raw string array.
     * No transformation is applied; the caller controls interpretation.
     *
     * @param classpath classpath-relative path, e.g. {@code "testdata/search-terms.csv"}
     * @return ordered list of rows; never {@code null}
     * @throws DataException if the resource is absent or cannot be parsed
     */
    public static List<String[]> readRaw(final String classpath) {
        log.debug("Reading raw CSV rows from '{}'", classpath);
        try (CSVReader reader = buildReader(classpath)) {
            return reader.readAll();
        } catch (IOException | CsvException e) {
            throw new DataException("Failed to read CSV from '" + classpath + "'", e);
        }
    }

    /**
     * Returns all data rows as header-keyed maps.
     *
     * <p>The first line is consumed as the header row; subsequent rows are
     * mapped {@code header[i] → row[i]}. Columns beyond the header width
     * are silently dropped; missing columns default to an empty string.</p>
     *
     * @param classpath classpath-relative path
     * @return list of row maps; never {@code null}; empty if file has only a header
     * @throws DataException if the resource is absent or cannot be parsed
     */
    public static List<Map<String, String>> readMaps(final String classpath) {
        log.debug("Reading header-mapped CSV from '{}'", classpath);
        List<String[]> rows = readRaw(classpath);
        if (rows.isEmpty()) {
            return new ArrayList<>();
        }
        String[] headers = rows.get(0);
        List<Map<String, String>> result = new ArrayList<>(rows.size() - 1);
        for (int r = 1; r < rows.size(); r++) {
            String[] row = rows.get(r);
            Map<String, String> map = new LinkedHashMap<>(headers.length * 2);
            for (int c = 0; c < headers.length; c++) {
                map.put(headers[c].trim(), c < row.length ? row[c] : "");
            }
            result.add(map);
        }
        return result;
    }

    /**
     * Converts CSV data rows (skipping the header) into a {@code Object[][]}
     * suitable for TestNG {@link org.testng.annotations.DataProvider}.
     *
     * <p>Each CSV row becomes one {@code Object[]} of raw {@code String}
     * values. The first row is treated as a header and excluded.</p>
     *
     * @param classpath classpath-relative path
     * @return two-dimensional array; row count = data rows, column count = CSV width
     * @throws DataException if the resource is absent or cannot be parsed
     */
    public static Object[][] toDataProvider(final String classpath) {
        log.debug("Building Object[][] from CSV '{}'", classpath);
        List<String[]> rows = readRaw(classpath);
        if (rows.size() <= 1) {
            // Only a header row or completely empty
            return new Object[0][0];
        }
        // skip header (index 0)
        Object[][] data = new Object[rows.size() - 1][];
        for (int i = 1; i < rows.size(); i++) {
            data[i - 1] = rows.get(i);
        }
        return data;
    }

    /**
     * Converts CSV data rows (no header stripping) into a {@code Object[][]}.
     * Use when the file has no header line.
     *
     * @param classpath classpath-relative path
     * @param hasHeader {@code true} to skip the first row as a header
     * @return two-dimensional array
     * @throws DataException if the resource is absent or cannot be parsed
     */
    public static Object[][] toDataProvider(final String classpath, final boolean hasHeader) {
        return hasHeader ? toDataProvider(classpath) : rawToDataProvider(classpath);
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private static Object[][] rawToDataProvider(final String classpath) {
        List<String[]> rows = readRaw(classpath);
        Object[][] data = new Object[rows.size()][];
        for (int i = 0; i < rows.size(); i++) {
            data[i] = rows.get(i);
        }
        return data;
    }

    private static CSVReader buildReader(final String classpath) {
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(classpath);
        if (is == null) {
            is = CsvDataReader.class.getResourceAsStream("/" + classpath);
        }
        if (is == null) {
            throw new DataException("Classpath resource not found: '" + classpath + "'");
        }
        RFC4180Parser parser = new RFC4180ParserBuilder().build();
        return new CSVReaderBuilder(new InputStreamReader(is, StandardCharsets.UTF_8))
                .withCSVParser(parser)
                .build();
    }
}
