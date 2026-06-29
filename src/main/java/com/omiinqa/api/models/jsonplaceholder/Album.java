package com.omiinqa.api.models.jsonplaceholder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents an album resource from the JSONPlaceholder REST API
 * ({@code https://jsonplaceholder.typicode.com/albums}).
 *
 * <p>JSONPlaceholder seeds 100 albums (IDs 1–100), each belonging to one of 10
 * users.  Albums serve as the parent collection for {@link Photo} resources,
 * allowing nested lookups via {@code GET /albums/{id}/photos}.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Album {

    /** Server-assigned album identifier (1–100). */
    private int id;

    /** Foreign key linking the album to its owner in {@code /users}. */
    private int userId;

    /** Album title. */
    private String title;
}
