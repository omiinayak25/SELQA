package com.omiinqa.api.models.jsonplaceholder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a to-do item resource from the JSONPlaceholder REST API
 * ({@code https://jsonplaceholder.typicode.com/todos}).
 *
 * <p>JSONPlaceholder seeds 200 to-do items (IDs 1–200), owned by 10 users
 * (20 per user).  The {@code completed} boolean flag distinguishes done from
 * outstanding items, enabling filter-by-status tests via
 * {@code GET /todos?completed=true} or {@code ?userId=N}.</p>
 *
 * <p>Write fakes (POST / PUT / PATCH / DELETE) are supported and echo the
 * request body with a synthetic {@code id = 201} on create.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Todo {

    /** Server-assigned to-do identifier (1–200). */
    private int id;

    /** Foreign key linking the to-do to its owner in {@code /users}. */
    private int userId;

    /** Short description of the task. */
    private String title;

    /** {@code true} if the task has been completed; {@code false} otherwise. */
    private boolean completed;
}
