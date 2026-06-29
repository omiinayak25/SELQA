package com.omiinqa.api.soap;

import com.omiinqa.exceptions.ApiException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Immutable value object returned by {@link SoapClient} after every request.
 *
 * <p><b>Design rationale:</b> Wrapping the raw HTTP response in a dedicated
 * value object gives test authors SOAP-aware helpers ({@link #extract(String)})
 * without exposing them to REST Assured or {@code java.net.http} types.  The
 * intentionally simple XPath-like extraction uses regex rather than a DOM
 * parser to avoid pulling in additional dependencies and because SOAP test
 * assertions are typically narrow (one value per assertion).</p>
 *
 * <p>Instances of this class are created only by {@link SoapClient} and are
 * immutable (all fields are final).</p>
 */
public final class SoapResponse {

    private final int    statusCode;
    private final String body;

    /**
     * Package-private constructor — only {@link SoapClient} creates instances.
     *
     * @param statusCode the HTTP status code of the SOAP response
     * @param body       the full response body string (the SOAP envelope XML)
     */
    SoapResponse(final int statusCode, final String body) {
        this.statusCode = statusCode;
        this.body       = body;
    }

    /**
     * Returns the HTTP status code.
     *
     * <p>Note: SOAP 1.1 faults are delivered with HTTP 500; SOAP 1.2 faults
     * may use 400 or 500 depending on the fault code.  Both are distinct from
     * application-level faults inside the SOAP body.</p>
     *
     * @return HTTP status code (e.g., 200, 500)
     */
    public int statusCode() {
        return statusCode;
    }

    /**
     * Returns the full raw body of the HTTP response (the complete SOAP
     * envelope XML).
     *
     * @return raw response body; never {@code null}
     */
    public String body() {
        return body;
    }

    /**
     * Extracts the text content of the <em>first</em> element matching
     * {@code tagName} found anywhere in the response body using a lightweight
     * regex search.
     *
     * <p>This method is intentionally simple: it locates the first occurrence
     * of {@code <tagName>...</tagName>} (ignoring namespace prefixes) and
     * returns the inner text.  For complex XPath or multi-node extraction use
     * a full XML parser on {@link #body()} directly.</p>
     *
     * <p>Example: given a body containing
     * {@code <soap:Body><ns:GetUserResult>Alice</ns:GetUserResult></soap:Body>},
     * calling {@code extract("GetUserResult")} returns {@code "Alice"}.</p>
     *
     * @param tagName the local XML element name to search for (without prefix)
     * @return the inner text content of the first matching element
     * @throws ApiException if no matching element is found in the response body
     */
    public String extract(final String tagName) {
        // Pattern: match <prefix:tagName> or <tagName> (with optional attributes)
        // then capture content up to the closing </...tagName>
        final Pattern pattern = Pattern.compile(
                "<(?:[a-zA-Z0-9_]+:)?" + Pattern.quote(tagName) + "(?:\\s[^>]*)?>([^<]*)<",
                Pattern.DOTALL
        );
        final Matcher matcher = pattern.matcher(body);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        throw new ApiException(
                "Tag <" + tagName + "> not found in SOAP response body. Body was:\n" + body);
    }

    /**
     * Returns {@code true} if the response body contains a {@code <Fault>}
     * element, indicating a SOAP fault regardless of the HTTP status code.
     *
     * @return {@code true} when the response is a SOAP fault
     */
    public boolean isFault() {
        return body != null && body.contains("Fault>");
    }

    @Override
    public String toString() {
        return "SoapResponse{statusCode=" + statusCode + ", bodyLength=" + (body == null ? 0 : body.length()) + '}';
    }
}
