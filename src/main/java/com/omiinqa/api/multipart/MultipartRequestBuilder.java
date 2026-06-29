package com.omiinqa.api.multipart;

import com.omiinqa.config.FrameworkConfig;
import com.omiinqa.exceptions.ApiException;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Fluent builder for multipart/form-data HTTP requests using REST Assured.
 *
 * <p><b>Design rationale:</b> File uploads combine field parts (plain text
 * form fields) and file parts (binary or text streams) in a single HTTP
 * request encoded as {@code multipart/form-data}.  REST Assured's
 * {@code .multiPart()} DSL already handles MIME boundary generation and
 * {@code Content-Disposition} headers correctly, so this builder acts purely
 * as a configuring facade: it collects all parts via a clean fluent API and
 * then delegates the actual HTTP call to REST Assured, preventing callers from
 * having to interact with the verbose REST Assured setup directly.</p>
 *
 * <p><b>Accepted part types:</b></p>
 * <ul>
 *   <li><em>Field parts</em> — plain string form fields ({@link #field(String, String)})</li>
 *   <li><em>File parts</em> — file uploads with auto-detected or explicit MIME type
 *       ({@link #filePart(String, File)}, {@link #filePart(String, File, String)})</li>
 *   <li><em>Inline byte parts</em> — for programmatically constructed payloads
 *       ({@link #bytePart(String, byte[], String, String)})</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * Response r = new MultipartRequestBuilder()
 *     .endpoint("https://httpbin.org/post")
 *     .field("description", "my upload")
 *     .filePart("file", new File("test-data/sample.txt"), "text/plain")
 *     .post();
 * r.then().statusCode(200);
 * }</pre>
 *
 * <p>Instances are NOT thread-safe; create a new builder per request.</p>
 */
public final class MultipartRequestBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(MultipartRequestBuilder.class);

    // -----------------------------------------------------------------------
    //  Internal part descriptors
    // -----------------------------------------------------------------------

    /** Sealed hierarchy for the two flavours of multipart parts. */
    private sealed interface Part permits FieldPart, FilePart, BytePart {}

    private record FieldPart(String name, String value) implements Part {}

    private record FilePart(String controlName, File file, String mimeType) implements Part {}

    private record BytePart(String controlName, byte[] bytes,
                            String mimeType, String fileName) implements Part {}

    // -----------------------------------------------------------------------
    //  Builder state
    // -----------------------------------------------------------------------

    private String       endpoint;
    private final List<Part>   parts   = new ArrayList<>();
    private final List<String[]> headers = new ArrayList<>();   // [name, value]

    // -----------------------------------------------------------------------
    //  Fluent configuration
    // -----------------------------------------------------------------------

    /**
     * Sets the absolute URL that will receive the multipart POST.
     *
     * @param endpoint full URL (e.g., {@code "https://httpbin.org/post"});
     *                 must not be {@code null}
     * @return {@code this} for chaining
     */
    public MultipartRequestBuilder endpoint(final String endpoint) {
        this.endpoint = endpoint;
        return this;
    }

    /**
     * Resolves the endpoint URL from a known API key via
     * {@link FrameworkConfig#apiUrl(String)} and appends a path suffix.
     *
     * @param apiKey config key (e.g., {@code "httpbin"})
     * @param path   path suffix (e.g., {@code "/post"})
     * @return {@code this} for chaining
     */
    public MultipartRequestBuilder endpoint(final String apiKey, final String path) {
        this.endpoint = FrameworkConfig.get().apiUrl(apiKey) + path;
        return this;
    }

    /**
     * Adds a plain text form field to the multipart body.
     *
     * @param name  the {@code Content-Disposition} field name; must not be {@code null}
     * @param value the field value
     * @return {@code this} for chaining
     */
    public MultipartRequestBuilder field(final String name, final String value) {
        parts.add(new FieldPart(name, value));
        return this;
    }

    /**
     * Adds a file part with automatic MIME type detection.
     *
     * <p>REST Assured uses the JDK's
     * {@link java.nio.file.Files#probeContentType(java.nio.file.Path)} as a
     * fallback; the server ultimately determines the accepted MIME types.</p>
     *
     * @param controlName the form field name (e.g., {@code "file"})
     * @param file        the file to upload; must exist and be readable
     * @return {@code this} for chaining
     */
    public MultipartRequestBuilder filePart(final String controlName, final File file) {
        parts.add(new FilePart(controlName, file, null));
        return this;
    }

    /**
     * Adds a file part with an explicit MIME type.
     *
     * @param controlName the form field name
     * @param file        the file to upload
     * @param mimeType    MIME type (e.g., {@code "image/png"}, {@code "text/csv"})
     * @return {@code this} for chaining
     */
    public MultipartRequestBuilder filePart(final String controlName,
                                             final File file,
                                             final String mimeType) {
        parts.add(new FilePart(controlName, file, mimeType));
        return this;
    }

    /**
     * Adds an in-memory byte array as a named file part.
     *
     * <p>Useful for uploading programmatically generated content (e.g., a CSV
     * built in-test) without writing a temporary file to disk.</p>
     *
     * @param controlName the form field name
     * @param bytes       the raw bytes
     * @param mimeType    MIME type for the part (e.g., {@code "application/octet-stream"})
     * @param fileName    the file name reported in {@code Content-Disposition}
     * @return {@code this} for chaining
     */
    public MultipartRequestBuilder bytePart(final String controlName,
                                             final byte[] bytes,
                                             final String mimeType,
                                             final String fileName) {
        parts.add(new BytePart(controlName, bytes.clone(), mimeType, fileName));
        return this;
    }

    /**
     * Adds an extra HTTP request header (e.g., {@code Authorization}).
     *
     * @param name  header name
     * @param value header value
     * @return {@code this} for chaining
     */
    public MultipartRequestBuilder header(final String name, final String value) {
        headers.add(new String[]{name, value});
        return this;
    }

    // -----------------------------------------------------------------------
    //  Terminal operation
    // -----------------------------------------------------------------------

    /**
     * Executes the multipart POST and returns the REST Assured
     * {@link Response}.
     *
     * <p>All parts registered via the fluent API are applied to the REST
     * Assured {@link RequestSpecification} in registration order.  The
     * {@code Content-Type} is NOT set explicitly because REST Assured generates
     * the correct {@code multipart/form-data; boundary=...} value
     * automatically when at least one {@code .multiPart()} call is made.</p>
     *
     * @return the server's response; never {@code null}
     * @throws ApiException if the endpoint has not been configured or the
     *                      request fails
     */
    public Response post() {
        if (endpoint == null || endpoint.isBlank()) {
            throw new ApiException("endpoint must be set before calling post()");
        }
        LOG.debug("Multipart POST → {} ({} parts)", endpoint, parts.size());

        RequestSpecification spec = RestAssured.given().baseUri(endpoint);

        // Apply extra headers
        for (final String[] h : headers) {
            spec = spec.header(h[0], h[1]);
        }

        // Apply parts
        for (final Part part : parts) {
            if (part instanceof FieldPart fp) {
                spec = spec.multiPart(fp.name(), fp.value());

            } else if (part instanceof FilePart fp) {
                if (fp.mimeType() != null) {
                    spec = spec.multiPart(fp.controlName(), fp.file(), fp.mimeType());
                } else {
                    spec = spec.multiPart(fp.controlName(), fp.file());
                }

            } else if (part instanceof BytePart bp) {
                spec = spec.multiPart(bp.controlName(), bp.fileName(),
                                      bp.bytes(), bp.mimeType());
            }
        }

        try {
            return spec.when().post();
        } catch (final Exception ex) {
            throw new ApiException("Multipart POST failed: " + ex.getMessage(), ex);
        }
    }

    // -----------------------------------------------------------------------
    //  Introspection (for offline unit testing of builder state)
    // -----------------------------------------------------------------------

    /**
     * Returns the number of parts currently registered.
     *
     * @return part count
     */
    public int partCount() {
        return parts.size();
    }

    /**
     * Returns the configured endpoint URL, or {@code null} if not yet set.
     *
     * @return endpoint URL string
     */
    public String getEndpoint() {
        return endpoint;
    }

    /**
     * Returns {@code true} if at least one part with the given control/field
     * name has been registered.
     *
     * @param name the part name to look for
     * @return {@code true} when found
     */
    public boolean hasPart(final String name) {
        return parts.stream().anyMatch(p -> switch (p) {
            case FieldPart fp -> fp.name().equals(name);
            case FilePart  fp -> fp.controlName().equals(name);
            case BytePart  bp -> bp.controlName().equals(name);
        });
    }
}
