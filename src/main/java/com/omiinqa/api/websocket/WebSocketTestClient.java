package com.omiinqa.api.websocket;

import com.omiinqa.exceptions.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Thread-safe WebSocket test client built on {@link java.net.http.WebSocket} (JDK 21).
 *
 * <p><b>Design rationale:</b> Testing WebSocket interactions requires
 * controlling the exact sequence of frames sent and received.  This client
 * wraps the JDK's asynchronous {@code WebSocket} API behind a synchronous,
 * {@link BlockingQueue}-based facade so test code can use simple
 * {@code awaitMessage()} calls without writing {@code CompletableFuture}
 * chains.  The {@link Listener} inner class implements the JDK
 * {@link WebSocket.Listener} interface and captures every text frame for
 * later inspection.</p>
 *
 * <h2>Thread safety</h2>
 * <ul>
 *   <li>{@link #send(String)} delegates to the JDK {@code WebSocket.sendText},
 *       which is thread-safe per the JDK contract.</li>
 *   <li>{@link #awaitMessage(long, TimeUnit)} blocks the calling thread using
 *       {@link BlockingQueue#poll(long, TimeUnit)} — safe to call from multiple
 *       threads.</li>
 *   <li>{@link #getReceivedMessages()} returns an unmodifiable snapshot.</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * WebSocketTestClient ws = new WebSocketTestClient();
 * ws.connect(URI.create("wss://echo.websocket.events"));
 * ws.send("hello");
 * String echo = ws.awaitMessage(5, TimeUnit.SECONDS);
 * assertThat(echo).isEqualTo("hello");
 * ws.close();
 * }</pre>
 */
public final class WebSocketTestClient {

    private static final Logger LOG = LoggerFactory.getLogger(WebSocketTestClient.class);

    /** Sentinel value pushed to the queue when the connection closes. */
    private static final String CLOSE_SENTINEL = "__CLOSED__";

    private final HttpClient          httpClient;
    private final Duration            connectTimeout;
    private final BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();
    private final List<String>          allMessages  = Collections.synchronizedList(new ArrayList<>());
    private final AtomicBoolean         connected    = new AtomicBoolean(false);

    private volatile WebSocket webSocket;

    /**
     * Creates a client with a 15-second connection timeout.
     */
    public WebSocketTestClient() {
        this(Duration.ofSeconds(15));
    }

    /**
     * Creates a client with the specified connection timeout.
     *
     * @param connectTimeout how long to wait for the handshake to complete;
     *                       must not be {@code null}
     */
    public WebSocketTestClient(final Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .build();
    }

    /**
     * Initiates a WebSocket handshake to the given URI and blocks until the
     * connection is open (or the timeout is exceeded).
     *
     * @param uri the WebSocket endpoint URI (e.g., {@code wss://echo.websocket.events});
     *            must use the {@code ws://} or {@code wss://} scheme
     * @throws ApiException if the handshake fails or times out
     */
    public void connect(final URI uri) {
        LOG.debug("Connecting to WebSocket: {}", uri);
        try {
            final Listener listener = new Listener();
            final CompletableFuture<WebSocket> future =
                    httpClient.newWebSocketBuilder()
                            .buildAsync(uri, listener);

            webSocket = future.get(connectTimeout.toMillis(), TimeUnit.MILLISECONDS);
            connected.set(true);
            LOG.info("WebSocket connected to {}", uri);
        } catch (final InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new ApiException("WebSocket connect interrupted", ie);
        } catch (final Exception ex) {
            throw new ApiException("WebSocket connect failed for [" + uri + "]: " + ex.getMessage(), ex);
        }
    }

    /**
     * Sends a UTF-8 text message to the server.
     *
     * <p>The send is performed asynchronously by the JDK; this method blocks
     * until the frame is handed to the OS network stack (not until the server
     * acknowledges it).</p>
     *
     * @param text the message to send; must not be {@code null}
     * @throws ApiException if the connection is not open or the send fails
     */
    public void send(final String text) {
        if (!connected.get() || webSocket == null) {
            throw new ApiException("WebSocket is not connected. Call connect() first.");
        }
        LOG.debug("WebSocket sending: {}", text);
        try {
            webSocket.sendText(text, true).get(connectTimeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (final InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new ApiException("WebSocket send interrupted", ie);
        } catch (final Exception ex) {
            throw new ApiException("WebSocket send failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * Blocks until a message is received or the timeout elapses.
     *
     * <p>Messages are delivered in the order they are received.  Each call
     * consumes one message from the internal queue; unconsumed messages remain
     * available for subsequent calls.</p>
     *
     * @param timeout the maximum time to wait
     * @param unit    the time unit of the {@code timeout} argument
     * @return the next received text message
     * @throws ApiException if no message arrives within the timeout, or if the
     *                      connection closes before a message is received
     */
    public String awaitMessage(final long timeout, final TimeUnit unit) {
        try {
            final String msg = messageQueue.poll(timeout, unit);
            if (msg == null) {
                throw new ApiException("Timed out waiting for WebSocket message after "
                        + timeout + " " + unit);
            }
            if (CLOSE_SENTINEL.equals(msg)) {
                throw new ApiException("WebSocket connection closed before message was received");
            }
            return msg;
        } catch (final ApiException ae) {
            throw ae;
        } catch (final InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new ApiException("Interrupted while waiting for WebSocket message", ie);
        }
    }

    /**
     * Sends a normal close frame (code 1000, reason {@code "test-done"}) and
     * waits up to the configured timeout for the server to acknowledge.
     *
     * <p>Calling {@code close()} on an already-closed connection is a no-op.</p>
     */
    public void close() {
        if (!connected.compareAndSet(true, false) || webSocket == null) {
            return;
        }
        try {
            LOG.debug("Closing WebSocket connection");
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "test-done")
                     .get(connectTimeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (final InterruptedException ie) {
            Thread.currentThread().interrupt();
        } catch (final Exception ex) {
            LOG.warn("Exception during WebSocket close: {}", ex.getMessage());
        }
    }

    /**
     * Returns an unmodifiable snapshot of all messages received since
     * {@link #connect(URI)} was called.
     *
     * @return immutable list of received text messages, in arrival order
     */
    public List<String> getReceivedMessages() {
        return Collections.unmodifiableList(new ArrayList<>(allMessages));
    }

    /**
     * Returns {@code true} if the WebSocket is currently open.
     *
     * @return {@code true} when connected
     */
    public boolean isConnected() {
        return connected.get();
    }

    // -----------------------------------------------------------------------
    //  WebSocket listener (inner class)
    // -----------------------------------------------------------------------

    /**
     * JDK WebSocket listener that captures text frames in a thread-safe queue.
     *
     * <p>Binary frames and pings are silently discarded; the test scope is
     * text-only.  The listener requests one message at a time via
     * {@link WebSocket#request(long)} to apply back-pressure and prevent the
     * JDK from buffering an unbounded number of frames in memory.</p>
     */
    private final class Listener implements WebSocket.Listener {

        private final StringBuilder textBuffer = new StringBuilder();

        @Override
        public CompletionStage<?> onText(final WebSocket ws,
                                         final CharSequence data,
                                         final boolean last) {
            textBuffer.append(data);
            if (last) {
                final String complete = textBuffer.toString();
                textBuffer.setLength(0);
                LOG.debug("WebSocket received: {}", complete);
                allMessages.add(complete);
                messageQueue.offer(complete);
            }
            ws.request(1);
            return null;
        }

        @Override
        public void onOpen(final WebSocket ws) {
            LOG.debug("WebSocket onOpen");
            ws.request(1);
        }

        @Override
        public CompletionStage<?> onPing(final WebSocket ws, final ByteBuffer message) {
            ws.sendPong(message);
            ws.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(final WebSocket ws,
                                           final int statusCode,
                                           final String reason) {
            LOG.debug("WebSocket onClose: status={} reason={}", statusCode, reason);
            connected.set(false);
            messageQueue.offer(CLOSE_SENTINEL);
            return null;
        }

        @Override
        public void onError(final WebSocket ws, final Throwable error) {
            LOG.error("WebSocket error: {}", error.getMessage(), error);
            connected.set(false);
            messageQueue.offer(CLOSE_SENTINEL);
        }
    }
}
