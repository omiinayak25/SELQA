package com.omiinqa.api.models.jsonplaceholder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a post resource from the JSONPlaceholder REST API
 * ({@code https://jsonplaceholder.typicode.com/posts}).
 *
 * <p>JSONPlaceholder seeds 100 posts (IDs 1–100), each belonging to one of 10
 * users.  The {@code userId} field acts as a foreign key, allowing cross-resource
 * filtering via {@code GET /posts?userId=N} and nested lookups via
 * {@code GET /users/{id}/posts}.</p>
 *
 * <p>Write operations (POST / PUT / PATCH / DELETE) are faked by the server:
 * the request body is echoed back with a synthetic {@code id} for creates, and
 * the patched/deleted record is returned for mutations — no data is persisted.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Post {

    /** Server-assigned post identifier. */
    private int id;

    /** Foreign key linking the post to its author in {@code /users}. */
    private int userId;

    /** Post headline / title. */
    private String title;

    /** Full post body text. */
    private String body;
}
