package com.omiinqa.utils.data;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.omiinqa.exceptions.DataException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Classpath-based JSON test data reader.
 *
 * <p><strong>Pattern:</strong> Utility / Static Factory — provides a single,
 * zero-state entry point so test authors never manage ObjectMapper lifecycle.
 * A single shared mapper is configured once at class load time: unknown
 * properties are silently ignored (forward-compatible with schema evolution)
 * and {@link JavaTimeModule} is registered so {@code LocalDate}/{@code
 * Instant} fields deserialise without custom code.</p>
 *
 * <p>All methods resolve paths relative to the classpath root; place JSON
 * files under {@code src/test/resources/testdata/} and reference them as
 * {@code "testdata/users.json"}.</p>
 *
 * <p>Failures are surfaced as {@link DataException} (unchecked) so tests
 * fail fast with a clear data-layer message rather than a cryptic NPE.</p>
 *
 * <p><strong>Usage:</strong>
 * <pre>{@code
 *   List<User> users = JsonDataReader.readList("testdata/users.json", User.class);
 *   User u          = JsonDataReader.readObject("testdata/admin.json", User.class);
 *   JsonNode raw    = JsonDataReader.readTree("testdata/products.json");
 * }</pre>
 * </p>
 */
public final class JsonDataReader {

    private static final Logger log = LoggerFactory.getLogger(JsonDataReader.class);

    /** Shared, immutable-after-init Jackson mapper. */
    private static final ObjectMapper MAPPER = buildMapper();

    private JsonDataReader() {
        // utility — not instantiable
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Deserialises a JSON object file into a single POJO.
     *
     * @param <T>       target type
     * @param classpath classpath-relative path, e.g. {@code "testdata/admin.json"}
     * @param type      target class
     * @return populated instance
     * @throws DataException if the resource is missing or the JSON is malformed
     */
    public static <T> T readObject(final String classpath, final Class<T> type) {
        log.debug("Reading JSON object from '{}' into {}", classpath, type.getSimpleName());
        try (InputStream is = openStream(classpath)) {
            return MAPPER.readValue(is, type);
        } catch (IOException e) {
            throw new DataException("Failed to read JSON object from '" + classpath + "'", e);
        }
    }

    /**
     * Deserialises a JSON array file into a typed list of POJOs.
     *
     * @param <T>       element type
     * @param classpath classpath-relative path, e.g. {@code "testdata/users.json"}
     * @param elementType element class
     * @return immutable-friendly list
     * @throws DataException if the resource is missing or the JSON is malformed
     */
    public static <T> List<T> readList(final String classpath, final Class<T> elementType) {
        log.debug("Reading JSON array from '{}' into List<{}>", classpath, elementType.getSimpleName());
        try (InputStream is = openStream(classpath)) {
            return MAPPER.readValue(is,
                    MAPPER.getTypeFactory().constructCollectionType(List.class, elementType));
        } catch (IOException e) {
            throw new DataException("Failed to read JSON list from '" + classpath + "'", e);
        }
    }

    /**
     * Deserialises a JSON file into a type captured by a {@link TypeReference},
     * enabling complex generics such as {@code Map<String, List<User>>}.
     *
     * @param <T>       target type
     * @param classpath classpath-relative path
     * @param typeRef   Jackson type reference
     * @return populated value
     * @throws DataException if the resource is missing or the JSON is malformed
     */
    public static <T> T readValue(final String classpath, final TypeReference<T> typeRef) {
        log.debug("Reading JSON from '{}' via TypeReference", classpath);
        try (InputStream is = openStream(classpath)) {
            return MAPPER.readValue(is, typeRef);
        } catch (IOException e) {
            throw new DataException("Failed to read JSON from '" + classpath + "'", e);
        }
    }

    /**
     * Parses a JSON file into a raw {@link JsonNode} tree for ad-hoc navigation
     * without a target POJO type.
     *
     * @param classpath classpath-relative path
     * @return root JsonNode
     * @throws DataException if the resource is missing or the JSON is malformed
     */
    public static JsonNode readTree(final String classpath) {
        log.debug("Reading JSON tree from '{}'", classpath);
        try (InputStream is = openStream(classpath)) {
            return MAPPER.readTree(is);
        } catch (IOException e) {
            throw new DataException("Failed to read JSON tree from '" + classpath + "'", e);
        }
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private static ObjectMapper buildMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    private static InputStream openStream(final String classpath) {
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(classpath);
        if (is == null) {
            // fallback: absolute classpath leading slash
            is = JsonDataReader.class.getResourceAsStream("/" + classpath);
        }
        if (is == null) {
            throw new DataException("Classpath resource not found: '" + classpath + "'");
        }
        return is;
    }
}
