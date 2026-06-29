package com.omiinqa.reports.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.omiinqa.exceptions.FrameworkException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Serialises a {@link TestRunResult} to pretty-printed JSON using Jackson.
 *
 * <h2>Strategy Pattern</h2>
 * <p>Implements the <em>ConcreteStrategy</em> role in the exporter strategy
 * family. {@link ReportExporterFacade} holds a reference to each exporter
 * strategy and delegates format-specific serialisation to them, keeping the
 * facade free of format knowledge.</p>
 *
 * <h2>Construction</h2>
 * <p>Callers may inject a pre-configured {@link ObjectMapper} (e.g. one that
 * has custom serialisers registered) or use the no-arg constructor which
 * creates a default mapper with pretty-printing enabled.</p>
 *
 * <pre>
 *   JsonResultExporter exporter = new JsonResultExporter();
 *   String json = exporter.toJson(result);
 *   exporter.writeToFile(result, Path.of("reports/test-run.json"));
 * </pre>
 *
 * @see ReportExporterFacade
 */
public class JsonResultExporter {

    private static final Logger LOG = LoggerFactory.getLogger(JsonResultExporter.class);

    private final ObjectMapper mapper;

    /**
     * Creates an exporter backed by a new, default {@link ObjectMapper} with
     * {@link SerializationFeature#INDENT_OUTPUT} enabled.
     */
    public JsonResultExporter() {
        this(new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT));
    }

    /**
     * Creates an exporter backed by the supplied {@link ObjectMapper}.
     * The caller is responsible for enabling pretty-printing if desired.
     *
     * @param mapper pre-configured Jackson mapper; must not be {@code null}
     */
    public JsonResultExporter(final ObjectMapper mapper) {
        if (mapper == null) {
            throw new IllegalArgumentException("ObjectMapper must not be null");
        }
        this.mapper = mapper;
    }

    /**
     * Serialises the supplied {@link TestRunResult} to a pretty-printed JSON string.
     *
     * @param result the test run data to serialise; must not be {@code null}
     * @return a non-null, non-empty JSON string
     * @throws FrameworkException if Jackson cannot serialise the object
     */
    public String toJson(final TestRunResult result) {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
        } catch (final IOException ex) {
            throw new FrameworkException("Failed to serialise TestRunResult to JSON", ex);
        }
    }

    /**
     * Serialises the supplied {@link TestRunResult} and writes it to {@code outputPath}.
     * Parent directories must already exist; this method does not create them.
     *
     * @param result     the test run data to serialise; must not be {@code null}
     * @param outputPath destination file path; must not be {@code null}
     * @throws FrameworkException if serialisation or file I/O fails
     */
    public void writeToFile(final TestRunResult result, final Path outputPath) {
        final String json = toJson(result);
        try {
            Files.writeString(outputPath, json, StandardCharsets.UTF_8);
            LOG.info("JSON report written to: {}", outputPath.toAbsolutePath());
        } catch (final IOException ex) {
            throw new FrameworkException("Failed to write JSON report to " + outputPath, ex);
        }
    }
}
