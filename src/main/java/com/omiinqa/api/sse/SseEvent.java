package com.omiinqa.api.sse;

import lombok.Builder;
import lombok.Value;

/**
 * Immutable value object representing a single Server-Sent Event (SSE).
 *
 * <p>The SSE protocol (WHATWG EventSource specification) defines four optional
 * fields per event: {@code event} (type), {@code data}, {@code id}, and
 * {@code retry}.  {@code data} is the only field that is always present in a
 * meaningful event; the others are optional.</p>
 *
 * <p>Instances are created only by {@link SseClient}'s internal line parser
 * and are immutable by construction (Lombok {@code @Value}).</p>
 */
@Value
@Builder
public class SseEvent {

    /**
     * The event type from the {@code event:} field.  Defaults to
     * {@code "message"} per the SSE specification when absent.
     * May be {@code null} if the event block contained no {@code event:} line.
     */
    String eventType;

    /**
     * The concatenated data payload from one or more {@code data:} lines.
     * Multi-line data values are joined with {@code \n}.
     */
    String data;

    /**
     * The value of the {@code id:} field for this event, used by clients to
     * track the last-received event for reconnection.  May be {@code null}.
     */
    String id;

    /**
     * The reconnection interval in milliseconds from the {@code retry:} field,
     * or {@code null} if the event contained no {@code retry:} directive.
     */
    Long retryMs;
}
