package com.omiinqa.api.models.jsonplaceholder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a comment resource from the JSONPlaceholder REST API
 * ({@code https://jsonplaceholder.typicode.com/comments}).
 *
 * <p>JSONPlaceholder seeds 500 comments (IDs 1–500), each linked to a post via
 * {@code postId}.  Comments can be fetched in bulk, by ID, or as a nested
 * sub-collection of a post via {@code GET /posts/{id}/comments}.</p>
 *
 * <p>The {@code email} field in this model represents the commenter's email
 * address — it is distinct from the user email in {@link JsonPlaceholderUser}
 * and is stored as a plain string (no relationship enforced by the fake API).</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Comment {

    /** Server-assigned comment identifier (1–500). */
    private int id;

    /** Foreign key linking the comment to its parent post. */
    private int postId;

    /** Commenter's display name. */
    private String name;

    /** Commenter's email address. */
    private String email;

    /** Full comment text. */
    private String body;
}
