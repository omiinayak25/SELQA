package com.omiinqa.api.soap;

import com.omiinqa.exceptions.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Lightweight SOAP 1.1/1.2 client built on top of {@link HttpClient} (JDK 21).
 *
 * <p><b>Design rationale:</b> REST Assured is optimized for JSON/REST and
 * provides no first-class SOAP support.  Using the JDK's own {@code HttpClient}
 * avoids additional dependencies while giving full control over the
 * {@code Content-Type} and {@code SOAPAction} headers that distinguish SOAP
 * from plain HTTP.  A single shared {@code HttpClient} instance (configured
 * once at construction) reuses the underlying connection pool, which is
 * important when testing SOAP endpoints that maintain keep-alive connections.</p>
 *
 * <p><b>Protocol notes:</b></p>
 * <ul>
 *   <li>SOAP 1.1 requires the {@code SOAPAction} HTTP header (may be empty
 *       string {@code ""}).  The {@code Content-Type} must be
 *       {@code text/xml; charset=utf-8}.</li>
 *   <li>SOAP 1.2 folds the action into the {@code Content-Type} parameter
 *       ({@code application/soap+xml; action="..."}) and the
 *       {@code SOAPAction} header is deprecated but harmless.</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * SoapClient client = new SoapClient();
 *
 * String envelope = new SoapEnvelopeBuilder()
 *     .version(SoapEnvelopeBuilder.SoapVersion.SOAP_11)
 *     .namespace("tem", "http://tempuri.org/")
 *     .bodyElement("<tem:Add><tem:a>3</tem:a><tem:b>4</tem:b></tem:Add>")
 *     .build();
 *
 * SoapResponse response = client.post(
 *     "http://www.dneonline.com/calculator.asmx",
 *     "http://tempuri.org/Add",
 *     envelope);
 *
 * assertThat(response.statusCode()).isEqualTo(200);
 * assertThat(response.extract("AddResult")).isEqualTo("7");
 * }</pre>
 */
public final class SoapClient {

    private static final Logger LOG = LoggerFactory.getLogger(SoapClient.class);

    /** Default connect + request timeout applied to every SOAP call. */
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    private final HttpClient httpClient;
    private final Duration   timeout;

    /**
     * Creates a {@code SoapClient} with default settings (30-second timeout,
     * HTTP/1.1 preferred, follow-redirects enabled).
     */
    public SoapClient() {
        this(DEFAULT_TIMEOUT);
    }

    /**
     * Creates a {@code SoapClient} with a custom per-request timeout.
     *
     * @param timeout connect-and-read timeout applied to each call; must not be {@code null}
     */
    public SoapClient(final Duration timeout) {
        this.timeout = timeout;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(timeout)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * Sends a SOAP request and returns a parsed {@link SoapResponse}.
     *
     * <p>The method sets:</p>
     * <ul>
     *   <li>{@code Content-Type} to the version-appropriate MIME type.</li>
     *   <li>{@code SOAPAction} to {@code soapAction} (required for SOAP 1.1;
     *       an empty string is legal and suppresses the header value).</li>
     *   <li>{@code Accept: text/xml, application/soap+xml} to signal that
     *       both SOAP versions are acceptable in the response.</li>
     * </ul>
     *
     * @param endpoint      full HTTP/HTTPS URL of the SOAP endpoint; must not be {@code null}
     * @param soapAction    value for the {@code SOAPAction} header (SOAP 1.1);
     *                      may be an empty string but must not be {@code null}
     * @param xmlBody       the complete SOAP envelope XML (see {@link SoapEnvelopeBuilder});
     *                      must not be {@code null}
     * @return a {@link SoapResponse} containing the HTTP status code and body
     * @throws ApiException if the request cannot be sent or a network error occurs
     */
    public SoapResponse post(final String endpoint,
                             final String soapAction,
                             final String xmlBody) {
        LOG.debug("SOAP POST → {} | SOAPAction: {}", endpoint, soapAction);

        try {
            final HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(timeout)
                    .header("Content-Type", "text/xml; charset=utf-8")
                    .header("SOAPAction",   soapAction)
                    .header("Accept",       "text/xml, application/soap+xml")
                    .POST(HttpRequest.BodyPublishers.ofString(xmlBody, StandardCharsets.UTF_8))
                    .build();

            final HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            LOG.debug("SOAP response: status={} bodyLength={}", response.statusCode(),
                    response.body() == null ? 0 : response.body().length());

            return new SoapResponse(response.statusCode(),
                    response.body() == null ? "" : response.body());

        } catch (final InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new ApiException("SOAP request interrupted: " + ie.getMessage(), ie);
        } catch (final Exception ex) {
            throw new ApiException("SOAP request failed for endpoint [" + endpoint + "]: " + ex.getMessage(), ex);
        }
    }
}
