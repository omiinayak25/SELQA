package com.omiinqa.utils.data;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.omiinqa.exceptions.DataException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Classpath-based YAML test data reader.
 *
 * <p><strong>Pattern:</strong> Utility / Static Factory — uses a Jackson
 * {@link ObjectMapper} configured with {@link YAMLFactory} (the
 * {@code jackson-dataformat-yaml} module) to parse YAML into Java objects.
 * The same mapper configuration as {@link JsonDataReader} is applied:
 * unknown fields are ignored and Java time types are handled via
 * {@link JavaTimeModule}.</p>
 *
 * <p>YAML is a superset of JSON and is convenient for multi-line, human-
 * readable test fixtures that would be awkward in JSON or CSV.</p>
 *
 * <p><strong>Usage:</strong>
 * <pre>{@code
 *   User user = YamlDataReader.readObject("testdata/admin-user.yaml", User.class);
 *   Map<String,Object> config = YamlDataReader.readMap("testdata/feature-flags.yaml");
 *   List<Product> prods = YamlDataReader.readList("testdata/products.yaml", Product.class);
 * }</pre>
 * </p>
 */
public final class YamlDataReader {

    private static final Logger log = LoggerFactory.getLogger(YamlDataReader.class);

    /** Shared, immutable-after-init YAML-flavoured Jackson mapper. */
    private static final ObjectMapper MAPPER = buildMapper();

    private YamlDataReader() {
        // utility — not instantiable
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Deserialises a YAML file into a single POJO.
     *
     * @param <T>       target type
     * @param classpath classpath-relative path, e.g. {@code "testdata/admin-user.yaml"}
     * @param type      target class
     * @return populated instance
     * @throws DataException if the resource is missing or the YAML is malformed
     */
    public static <T> T readObject(final String classpath, final Class<T> type) {
        log.debug("Reading YAML object from '{}' into {}", classpath, type.getSimpleName());
        try (InputStream is = openStream(classpath)) {
            return MAPPER.readValue(is, type);
        } catch (IOException e) {
            throw new DataException("Failed to read YAML object from '" + classpath + "'", e);
        }
    }

    /**
     * Deserialises a YAML list file into a typed list of POJOs.
     *
     * @param <T>         element type
     * @param classpath   classpath-relative path
     * @param elementType element class
     * @return list; never {@code null}
     * @throws DataException if the resource is missing or the YAML is malformed
     */
    public static <T> java.util.List<T> readList(final String classpath,
                                                   final Class<T> elementType) {
        log.debug("Reading YAML list from '{}' into List<{}>", classpath,
                elementType.getSimpleName());
        try (InputStream is = openStream(classpath)) {
            return MAPPER.readValue(is,
                    MAPPER.getTypeFactory().constructCollectionType(java.util.List.class, elementType));
        } catch (IOException e) {
            throw new DataException("Failed to read YAML list from '" + classpath + "'", e);
        }
    }

    /**
     * Parses a YAML file into a {@code Map<String,Object>} for ad-hoc key/value
     * access without a strongly-typed POJO.
     *
     * @param classpath classpath-relative path
     * @return map representation of the YAML document
     * @throws DataException if the resource is missing or the YAML is malformed
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> readMap(final String classpath) {
        log.debug("Reading YAML map from '{}'", classpath);
        try (InputStream is = openStream(classpath)) {
            return MAPPER.readValue(is, new TypeReference<Map<String, Object>>() { });
        } catch (IOException e) {
            throw new DataException("Failed to read YAML map from '" + classpath + "'", e);
        }
    }

    /**
     * Deserialises a YAML file using a Jackson {@link TypeReference},
     * enabling complex generic types such as {@code Map<String, List<User>>}.
     *
     * @param <T>       target type
     * @param classpath classpath-relative path
     * @param typeRef   Jackson type reference
     * @return populated value
     * @throws DataException if the resource is missing or the YAML is malformed
     */
    public static <T> T readValue(final String classpath, final TypeReference<T> typeRef) {
        log.debug("Reading YAML from '{}' via TypeReference", classpath);
        try (InputStream is = openStream(classpath)) {
            return MAPPER.readValue(is, typeRef);
        } catch (IOException e) {
            throw new DataException("Failed to read YAML from '" + classpath + "'", e);
        }
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private static ObjectMapper buildMapper() {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    private static InputStream openStream(final String classpath) {
        InputStream is = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(classpath);
        if (is == null) {
            is = YamlDataReader.class.getResourceAsStream("/" + classpath);
        }
        if (is == null) {
            throw new DataException("Classpath resource not found: '" + classpath + "'");
        }
        return is;
    }
}
