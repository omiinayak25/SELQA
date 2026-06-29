package com.omiinqa.api.sse;

import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SseClient} and its line-protocol parser.
 *
 * <h2>Test strategy</h2>
 * <ul>
 *   <li>Offline tests ({@code "unit"}) exercise the static
 *       {@link SseClient#parseBlock(List)} method directly, feeding it
 *       pre-composed line blocks.  This validates the full SSE line-protocol
 *       grammar without a live endpoint.</li>
 *   <li>Live tests ({@code "regression"}) subscribe to a public SSE endpoint.
 *       Because reliable public SSE services are scarce, the live test is
 *       guarded by a system property {@code sse.live=true}.  When the property
 *       is absent or {@code false} the test is skipped with a clear log
 *       message, keeping CI green in air-gapped environments.</li>
 * </ul>
 */
public class SseClientTest {

    // -----------------------------------------------------------------------
    //  Offline unit tests — SSE line-protocol parsing
    // -----------------------------------------------------------------------

    @Test(groups = {"api", "unit"},
          description = "Single data line produces SseEvent with correct data")
    public void singleDataLineProducesEvent() {
        final List<String> block = List.of("data: hello world");
        final SseEvent event = SseClient.parseBlock(block);

        assertThat(event.getData()).isEqualTo("hello world");
        assertThat(event.getEventType()).isNull();
        assertThat(event.getId()).isNull();
    }

    @Test(groups = {"api", "unit"},
          description = "event: field sets the eventType")
    public void eventFieldSetsEventType() {
        final List<String> block = Arrays.asList("event: update", "data: {\"key\":\"val\"}");
        final SseEvent event = SseClient.parseBlock(block);

        assertThat(event.getEventType()).isEqualTo("update");
        assertThat(event.getData()).isEqualTo("{\"key\":\"val\"}");
    }

    @Test(groups = {"api", "unit"},
          description = "id: field is captured")
    public void idFieldIsCaptured() {
        final List<String> block = Arrays.asList("id: 42", "data: tick");
        final SseEvent event = SseClient.parseBlock(block);

        assertThat(event.getId()).isEqualTo("42");
        assertThat(event.getData()).isEqualTo("tick");
    }

    @Test(groups = {"api", "unit"},
          description = "retry: field is parsed as long milliseconds")
    public void retryFieldIsParsedAsLong() {
        final List<String> block = Arrays.asList("retry: 3000", "data: reconnect-hint");
        final SseEvent event = SseClient.parseBlock(block);

        assertThat(event.getRetryMs()).isEqualTo(3000L);
    }

    @Test(groups = {"api", "unit"},
          description = "Multi-line data fields are joined with newline")
    public void multiLineDataFieldsAreJoined() {
        final List<String> block = Arrays.asList(
                "data: line1",
                "data: line2",
                "data: line3"
        );
        final SseEvent event = SseClient.parseBlock(block);

        assertThat(event.getData()).isEqualTo("line1\nline2\nline3");
    }

    @Test(groups = {"api", "unit"},
          description = "Comment lines (starting with :) are ignored")
    public void commentLinesAreIgnored() {
        final List<String> block = Arrays.asList(
                ": this is a comment",
                "data: actual data"
        );
        final SseEvent event = SseClient.parseBlock(block);

        assertThat(event.getData()).isEqualTo("actual data");
    }

    @Test(groups = {"api", "unit"},
          description = "Leading space after colon is stripped per spec")
    public void leadingSpaceAfterColonIsStripped() {
        final List<String> block = List.of("data: spaced value");
        final SseEvent event = SseClient.parseBlock(block);

        assertThat(event.getData()).isEqualTo("spaced value");
    }

    @Test(groups = {"api", "unit"},
          description = "Full event block with all fields parsed correctly")
    public void fullEventBlockParsedCorrectly() {
        final List<String> block = Arrays.asList(
                "id: 99",
                "event: user.created",
                "data: {\"userId\":1}",
                "retry: 5000"
        );
        final SseEvent event = SseClient.parseBlock(block);

        assertThat(event.getId()).isEqualTo("99");
        assertThat(event.getEventType()).isEqualTo("user.created");
        assertThat(event.getData()).isEqualTo("{\"userId\":1}");
        assertThat(event.getRetryMs()).isEqualTo(5000L);
    }

    @Test(groups = {"api", "unit"},
          description = "Unknown fields in event block are silently ignored")
    public void unknownFieldsAreIgnored() {
        final List<String> block = Arrays.asList(
                "data: important",
                "x-custom: should-be-ignored"
        );
        final SseEvent event = SseClient.parseBlock(block);

        assertThat(event.getData()).isEqualTo("important");
    }

    // -----------------------------------------------------------------------
    //  Live regression test — guarded by system property
    // -----------------------------------------------------------------------

    /**
     * Attempts to connect to the SSE endpoint specified by the system property
     * {@code sse.endpoint.url}.  This test is skipped (passes trivially) when
     * the property is absent so CI stays green in environments without network.
     *
     * <p>To run manually:
     * {@code mvn test -Dgroups=api,regression -Dsse.endpoint.url=https://sse.dev/test}</p>
     */
    @Test(groups = {"api", "regression"},
          description = "[LIVE] SSE subscribe collects at least one event from a live endpoint")
    public void liveEndpointDeliversAtLeastOneEvent() {
        final String endpointUrl = System.getProperty("sse.endpoint.url");
        if (endpointUrl == null || endpointUrl.isBlank()) {
            // No endpoint configured — skip gracefully
            System.out.println("[SSE-LIVE] sse.endpoint.url not set — skipping live SSE test");
            return;
        }

        final SseClient client = new SseClient();
        final List<SseEvent> events = client.subscribe(
                java.net.URI.create(endpointUrl), 3, 10, java.util.concurrent.TimeUnit.SECONDS);

        assertThat(events).isNotEmpty();
        events.forEach(e -> assertThat(e.getData()).isNotNull());
    }
}
