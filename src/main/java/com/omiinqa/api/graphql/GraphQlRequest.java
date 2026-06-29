package com.omiinqa.api.graphql;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Immutable value object representing a GraphQL HTTP request body.
 *
 * <p><b>Design rationale:</b> GraphQL transports all operations — queries,
 * mutations, and subscriptions — as an HTTP POST with a JSON body containing
 * at minimum a {@code query} string and, optionally, a {@code variables} map.
 * Encoding this contract in a dedicated POJO (rather than a raw {@code Map})
 * ensures type safety at construction time, eliminates ad-hoc string
 * concatenation, and allows Jackson to serialize the payload consistently.</p>
 *
 * <p>The {@link JsonInclude} annotation suppresses {@code null} fields so that
 * requests without variables do not send {@code "variables": null}, which some
 * GraphQL servers reject or misinterpret.</p>
 *
 * <p>Lombok's {@link Builder} annotation provides a fluent construction API
 * used by {@link GraphQlClient} and {@link CountriesQueries}.</p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * GraphQlRequest req = GraphQlRequest.builder()
 *     .query("{ country(code: \"US\") { name } }")
 *     .build();
 * }</pre>
 *
 * @see GraphQlClient
 * @see CountriesQueries
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GraphQlRequest {

    /**
     * The GraphQL query or mutation document string.
     * Must not be {@code null} or blank for valid GraphQL requests.
     */
    private String query;

    /**
     * Optional named variable map bound to the query's variable declarations.
     * When {@code null}, the field is omitted from the JSON payload to avoid
     * sending {@code "variables": null}.
     */
    private Map<String, Object> variables;
}
