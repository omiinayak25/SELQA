package com.omiinqa.api.soap;

import java.util.ArrayList;
import java.util.List;

/**
 * Fluent builder for hand-crafted SOAP 1.1 and SOAP 1.2 envelopes.
 *
 * <p><b>Design rationale:</b> JAXB and WS-Import bring heavy code-generation
 * overhead and version-skew risk for test environments.  Building XML as
 * a string lets test authors target arbitrary SOAP operations without schemas,
 * keeps the classpath clean, and makes envelope structure immediately visible
 * in test code.  The builder pattern (GoF) provides a discoverable, type-safe
 * API for the composable parts of an envelope: namespaces, header blocks,
 * and a single body element.</p>
 *
 * <h2>SOAP 1.1 envelope example</h2>
 * <pre>{@code
 * String xml = new SoapEnvelopeBuilder()
 *     .version(SoapVersion.SOAP_11)
 *     .namespace("tem", "http://tempuri.org/")
 *     .bodyElement("<tem:GetWeather><tem:CityName>London</tem:CityName></tem:GetWeather>")
 *     .build();
 * }</pre>
 *
 * <h2>SOAP 1.2 with a custom header</h2>
 * <pre>{@code
 * String xml = new SoapEnvelopeBuilder()
 *     .version(SoapVersion.SOAP_12)
 *     .headerBlock("<wsse:Security>...</wsse:Security>")
 *     .bodyElement("<tem:Lookup/>")
 *     .build();
 * }</pre>
 *
 * <p>Instances are NOT thread-safe; create a new builder per request.</p>
 */
public final class SoapEnvelopeBuilder {

    /** SOAP protocol version selection. */
    public enum SoapVersion {
        /**
         * SOAP 1.1 — uses namespace {@code http://schemas.xmlsoap.org/soap/envelope/}
         * and {@code text/xml} content type.
         */
        SOAP_11("http://schemas.xmlsoap.org/soap/envelope/", "soapenv"),
        /**
         * SOAP 1.2 — uses namespace {@code http://www.w3.org/2003/05/soap-envelope}
         * and {@code application/soap+xml} content type.
         */
        SOAP_12("http://www.w3.org/2003/05/soap-envelope", "soap");

        final String namespaceUri;
        final String prefix;

        SoapVersion(final String namespaceUri, final String prefix) {
            this.namespaceUri = namespaceUri;
            this.prefix       = prefix;
        }
    }

    private SoapVersion version = SoapVersion.SOAP_11;
    private final List<String[]> extraNamespaces = new ArrayList<>();   // [prefix, uri]
    private final List<String>   headerBlocks    = new ArrayList<>();
    private String bodyElement = "";

    /**
     * Selects the SOAP version.  Defaults to {@link SoapVersion#SOAP_11}.
     *
     * @param version the desired protocol version; must not be {@code null}
     * @return {@code this} for chaining
     */
    public SoapEnvelopeBuilder version(final SoapVersion version) {
        this.version = version;
        return this;
    }

    /**
     * Registers an additional XML namespace declaration on the root
     * {@code Envelope} element.
     *
     * @param prefix       namespace prefix (e.g., {@code "tem"})
     * @param namespaceUri the full namespace URI (e.g., {@code "http://tempuri.org/"})
     * @return {@code this} for chaining
     */
    public SoapEnvelopeBuilder namespace(final String prefix, final String namespaceUri) {
        this.extraNamespaces.add(new String[]{prefix, namespaceUri});
        return this;
    }

    /**
     * Appends a raw XML fragment inside the {@code <Header>} element.
     * Call multiple times to add several header blocks (e.g., WS-Security +
     * WS-Addressing).
     *
     * @param xmlFragment well-formed XML fragment; must not be {@code null}
     * @return {@code this} for chaining
     */
    public SoapEnvelopeBuilder headerBlock(final String xmlFragment) {
        this.headerBlocks.add(xmlFragment);
        return this;
    }

    /**
     * Sets the single XML fragment that becomes the content of the SOAP
     * {@code <Body>} element.  Typically a single operation element with its
     * child parameters (e.g., {@code <tem:GetUser><tem:id>1</tem:id></tem:GetUser>}).
     *
     * @param xmlFragment well-formed XML fragment; must not be {@code null}
     * @return {@code this} for chaining
     */
    public SoapEnvelopeBuilder bodyElement(final String xmlFragment) {
        this.bodyElement = xmlFragment;
        return this;
    }

    /**
     * Produces the complete, well-formed SOAP envelope XML string ready to be
     * sent as the HTTP request body.
     *
     * <p>The XML declaration ({@code <?xml version="1.0" ...?>}) is omitted
     * intentionally: many SOAP endpoints tolerate or ignore it, but some reject
     * the byte-order mark that certain processors prepend.  Omitting it is the
     * safest default for testing purposes.</p>
     *
     * @return the complete envelope as a UTF-8 string
     */
    public String build() {
        final String p  = version.prefix;
        final String ns = version.namespaceUri;

        final StringBuilder sb = new StringBuilder(512);
        sb.append('<').append(p).append(":Envelope")
          .append(" xmlns:").append(p).append("=\"").append(ns).append('"');

        for (final String[] entry : extraNamespaces) {
            sb.append(" xmlns:").append(entry[0]).append("=\"").append(entry[1]).append('"');
        }
        sb.append('>');

        // Header (only emitted when at least one block is present)
        if (!headerBlocks.isEmpty()) {
            sb.append('<').append(p).append(":Header>");
            for (final String block : headerBlocks) {
                sb.append(block);
            }
            sb.append("</").append(p).append(":Header>");
        }

        // Body (always present)
        sb.append('<').append(p).append(":Body>")
          .append(bodyElement)
          .append("</").append(p).append(":Body>")
          .append("</").append(p).append(":Envelope>");

        return sb.toString();
    }

    /**
     * Returns the {@code Content-Type} header value appropriate for the
     * configured SOAP version.
     *
     * <ul>
     *   <li>SOAP 1.1 → {@code text/xml; charset=utf-8}</li>
     *   <li>SOAP 1.2 → {@code application/soap+xml; charset=utf-8}</li>
     * </ul>
     *
     * @return content-type string; never {@code null}
     */
    public String contentType() {
        return version == SoapVersion.SOAP_11
               ? "text/xml; charset=utf-8"
               : "application/soap+xml; charset=utf-8";
    }
}
