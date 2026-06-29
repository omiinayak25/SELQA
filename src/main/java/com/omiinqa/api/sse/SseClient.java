package com.omiinqa.api.sse;

import com.omiinqa.exceptions.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Server-Sent Event (SSE) consumer built on {@link HttpClient} (JDK 21).
 *
 * <p><b>Protocol summary:</b> SSE is a one-way HTTP streaming protocol where
 * the server sends a {@code Content-Type: text/event-stream} response and
 * continuously writes UTF-8 text blocks separated by blank lines.  Each block
 * can contain {@code event:}, {@code data:}, {@code id:}, and {@code retry:}
 * fields.  The client reads the stream line-by-line, assembles complete event
 * blocks (terminated by an empty line), and delivers them as {@link SseEvent}
 * objects.</p>
 *
 * <p><b>Design rationale:</b> The JDK {@code HttpClient} supports HTTP/2
 * server push and streaming responses via
 * {@link HttpResponse.BodyHandlers#ofInputStream()}, which avoids loading the
 * entire stream into memory.  Reading is delegated to a background thread so
 * {@link #subscribe(URI, int, long, TimeUnit)} can block until either the
 * requested number of events arrives, the total timeout expires, or the server
 * closes the connection.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * SseClient sse = new SseClient();
 * List<SseEvent> events = sse.subscribe(
 *         URI.create("https://example.com/events"), 5, 10, TimeUnit.SECONDS);
 * assertThat(events).hasSize(5);
 * assertThat(events.get(0).getData()).isNotBlank();
 * }</pre>
 */
public final class SseClient {

    private static final Logger LOG = LoggerFactory.getLogger(SseClient.class);

    /** Expected MIME type for SSE streams. */
    private static final String MIME_EVENT_STREAM = "text/event-stream";

    private final HttpClient httpClient;

    /**
     * Creates an {@code SseClient} with default settings (30-second connect
     * timeout, follow redirects).
     */
    public SseClient() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(30))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * Subscribes to an SSE endpoint and collects events until {@code maxEvents}
     * are received or {@code totalTimeout} elapses (whichever comes first).
     *
     * <p>The method issues a GET request with
     * {@code Accept: text/event-stream, Cache-Control: no-cache} headers per
     * the SSE specification and reads the streaming body on a background thread.
     * The calling thread blocks until the stop condition is met or the timeout
     * expires.</p>
     *
     * @param uri          the SSE endpoint URI; must not be {@code null}
     * @param maxEvents    stop collecting after this many events; use
     *                     {@link Integer#MAX_VALUE} to rely solely on
     *                     {@code totalTimeout}
     * @param totalTimeout maximum wall-clock time to wait for events
     * @param unit         the time unit for {@code totalTimeout}
     * @return an unmodifiable list of received {@link SseEvent} objects, in
     *         arrival order; may be empty if the stream closes immediately
     * @throws ApiException if the HTTP connection fails or the response is not
     *                      an SSE stream
     */
    public List<SseEvent> subscribe(final URI uri,
                                    final int maxEvents,
                                    final long totalTimeout,
                                    final TimeUnit unit) {
        LOG.debug("SSE subscribe → {} (maxEvents={}, timeout={} {})", uri, maxEvents, totalTimeout, unit);

        final HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("Accept",        MIME_EVENT_STREAM)
                .header("Cache-Control", "no-cache")
                .header("Connection",    "keep-alive")
                .GET()
                .build();

        final List<SseEvent> events = Collections.synchronizedList(new ArrayList<>());

        final CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            try {
                final HttpResponse<InputStream> response =
                        httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

                final String ct = response.headers().firstValue("Content-Type").orElse("");
                if (!ct.contains(MIME_EVENT_STREAM)) {
                    LOG.warn("SSE endpoint returned unexpected Content-Type: {}", ct);
                }

                try (final BufferedReader reader = new BufferedReader(
                        new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {

                    final List<String> block = new ArrayList<>();
                    String line;

                    while ((line = reader.readLine()) != null
                            && events.size() < maxEvents) {
                        if (line.isEmpty()) {
                            // Blank line = event block terminator
                            if (!block.isEmpty()) {
                                final SseEvent event = parseBlock(block);
                                if (event.getData() != null && !event.getData().isEmpty()) {
                                    events.add(event);
                                    LOG.debug("SSE event #{}: type={} data={}",
                                            events.size(), event.getEventType(), event.getData());
                                }
                                block.clear();
                            }
                        } else {
                            block.add(line);
                        }
                    }
                    // Flush any trailing block without terminal blank line
                    if (!block.isEmpty()) {
                        final SseEvent event = parseBlock(block);
                        if (event.getData() != null && !event.getData().isEmpty()) {
                            events.add(event);
                        }
                    }
                }
            } catch (final InterruptedException ie) {
                Thread.currentThread().interrupt();
            } catch (final Exception ex) {
                LOG.error("SSE stream error: {}", ex.getMessage(), ex);
            }
        });

        try {
            future.get(totalTimeout, unit);
        } catch (final TimeoutException te) {
            LOG.debug("SSE subscription timed out after {} {} — collected {} events",
                    totalTimeout, unit, events.size());
            future.cancel(true);
        } catch (final InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new ApiException("SSE subscribe interrupted", ie);
        } catch (final ExecutionException ee) {
            throw new ApiException("SSE subscribe failed: " + ee.getCause().getMessage(), ee.getCause());
        }

        return Collections.unmodifiableList(new ArrayList<>(events));
    }

    /**
     * Parses a list of raw SSE lines (one event block) into an {@link SseEvent}.
     *
     * <p>Lines are processed according to the WHATWG EventSource spec:
     * a line starting with {@code :} is a comment and is ignored; lines
     * containing {@code :} split on the first colon into field name and value;
     * lines without {@code :} are treated as field names with an empty value.</p>
     *
     * @param lines the raw lines of a single event block
     * @return a fully populated {@link SseEvent}
     */
    public static SseEvent parseBlock(final List<String> lines) {
        final StringBuilder dataBuilder = new StringBuilder();
        String eventType = null;
        String id        = null;
        Long   retryMs   = null;

        for (final String line : lines) {
            if (line.startsWith(":")) {
                // Comment line — skip
                continue;
            }
            final int colonIdx = line.indexOf(':');
            final String field;
            String value;

            if (colonIdx < 0) {
                field = line.trim();
                value = "";
            } else {
                field = line.substring(0, colonIdx).trim();
                // Value: strip exactly one leading space per the spec
                value = line.substring(colonIdx + 1);
                if (value.startsWith(" ")) {
                    value = value.substring(1);
                }
            }

            switch (field) {
                case "event" -> eventType = value;
                case "data"  -> {
                    if (dataBuilder.length() > 0) {
                        dataBuilder.append('\n');
                    }
                    dataBuilder.append(value);
                }
                case "id"    -> id = value;
                case "retry" -> {
                    try {
                        retryMs = Long.parseLong(value.trim());
                    } catch (final NumberFormatException nfe) {
                        LOG.warn("SSE: invalid retry value: {}", value);
                    }
                }
                default -> LOG.debug("SSE: unknown field '{}' — ignored", field);
            }
        }

        return SseEvent.builder()
                .eventType(eventType)
                .data(dataBuilder.length() > 0 ? dataBuilder.toString() : null)
                .id(id)
                .retryMs(retryMs)
                .build();
    }
}
