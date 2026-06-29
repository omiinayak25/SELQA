package com.omiinqa.api.models.jsonplaceholder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents the company information for a JSONPlaceholder user.
 *
 * <p>Returned as the {@code company} sub-object inside every {@code /users}
 * response.  The three fields map directly from the JSON wire format without
 * renaming ({@code name}, {@code catchPhrase}, {@code bs}).</p>
 *
 * <p>This is a pure value object: it carries data from the API, participates
 * in equality / serialisation via Lombok, and has no business logic.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Company {

    /** Company name (e.g., {@code "Romaguera-Crona"}). */
    private String name;

    /** Marketing catch-phrase (e.g., {@code "Multi-layered client-server neural-net"}). */
    private String catchPhrase;

    /** Business strategy slogan (e.g., {@code "harness real-time e-markets"}). */
    private String bs;
}
