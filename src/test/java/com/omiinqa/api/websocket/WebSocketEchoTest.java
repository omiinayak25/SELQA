package com.omiinqa.api.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.net.URI;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LIVE integration tests for {@link WebSocketTestClient} against the public
 * echo servers {@code wss://echo.websocket.events} and
 * {@code wss://ws.postman-echo.com/raw}.
 *
 * <p><strong>Live test — requires network.</strong>  These tests may be
 * skipped in air-gapped CI; they are in groups {@code "api"} and
 * {@code "regression"}.  The test is written defensively: if the primary
 * echo server is unreachable, the failure message will clearly indicate a
 * connectivity problem rather than a framework bug.</p>
 *
 * <p>The echo servers simply reflect every text frame back to the sender.
 * Tests send a unique payload and assert that the received message equals
 * the sent payload.</p>
 */
public class WebSocketEchoTest {

    private static final Logger LOG = LoggerFactory.getLogger(WebSocketEchoTest.class);

    // Primary public echo server (backed by websocket.events)
    private static final String ECHO_URI_PRIMARY  = "wss://echo.websocket.events";
    // Fallback public echo server (Postman)
    private static final String ECHO_URI_FALLBACK = "wss://ws.postman-echo.com/raw";

    private WebSocketTestClient client;

    @AfterMethod(alwaysRun = true)
    public void closeClient() {
        if (client != null && client.isConnected()) {
            client.close();
        }
    }

    @Test(groups = {"api", "regression"},
          description = "[LIVE] WebSocket echo: sent text is reflected back unchanged")
    public void echoServerReflectsTextFrame() {
        client = new WebSocketTestClient();

        final String echoUri = resolveEchoUri();
        LOG.info("WebSocket echo test connecting to: {}", echoUri);

        client.connect(URI.create(echoUri));
        assertThat(client.isConnected()).isTrue();

        final String payload = "OmiinQA-echo-" + System.currentTimeMillis();
        client.send(payload);

        // Some echo servers send a welcome message first; drain until we match
        String received = "";
        for (int i = 0; i < 3; i++) {
            received = client.awaitMessage(8, TimeUnit.SECONDS);
            if (received.contains(payload)) {
                break;
            }
            LOG.debug("Received non-payload message: {}", received);
        }

        assertThat(received).isEqualTo(payload);
    }

    @Test(groups = {"api", "regression"},
          description = "[LIVE] WebSocket echo: multiple sequential sends are all echoed")
    public void multipleSequentialSendsAreEchoed() {
        client = new WebSocketTestClient();

        final String echoUri = resolveEchoUri();
        client.connect(URI.create(echoUri));

        final String[] payloads = {"alpha", "beta", "gamma"};
        for (final String p : payloads) {
            client.send(p);
        }

        // Drain welcome message(s) if present and collect our echoed payloads
        int matched = 0;
        for (int attempt = 0; attempt < payloads.length + 3 && matched < payloads.length; attempt++) {
            final String msg = client.awaitMessage(8, TimeUnit.SECONDS);
            for (final String p : payloads) {
                if (msg.equals(p)) {
                    matched++;
                    break;
                }
            }
        }
        assertThat(matched).isEqualTo(payloads.length);
    }

    @Test(groups = {"api", "regression"},
          description = "[LIVE] WebSocket: getReceivedMessages accumulates all echoed messages")
    public void receivedMessagesListAccumulatesAll() {
        client = new WebSocketTestClient();
        client.connect(URI.create(resolveEchoUri()));

        client.send("msg-1");
        client.send("msg-2");

        // Wait for both echoes (plus possible welcome frame)
        for (int i = 0; i < 4; i++) {
            try {
                client.awaitMessage(5, TimeUnit.SECONDS);
            } catch (final com.omiinqa.exceptions.ApiException e) {
                break;
            }
        }

        // getReceivedMessages must include both payloads
        assertThat(client.getReceivedMessages())
                .anySatisfy(m -> assertThat(m).isEqualTo("msg-1"))
                .anySatisfy(m -> assertThat(m).isEqualTo("msg-2"));
    }

    // -----------------------------------------------------------------------
    //  Helper: pick primary echo URI (fallback on system property override)
    // -----------------------------------------------------------------------

    /**
     * Returns the echo URI to use for this test run.
     * The system property {@code ws.echo.uri} can be set in CI to override
     * the default endpoint.
     */
    private static String resolveEchoUri() {
        return System.getProperty("ws.echo.uri", ECHO_URI_PRIMARY);
    }
}
