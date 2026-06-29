package com.omiinqa.api.models.jsonplaceholder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a photo resource from the JSONPlaceholder REST API
 * ({@code https://jsonplaceholder.typicode.com/photos}).
 *
 * <p>JSONPlaceholder seeds 5 000 photos (IDs 1–5 000), grouped into 100 albums
 * (50 photos per album).  Photos can be fetched individually, as a full
 * collection, as a nested sub-collection of an album via
 * {@code GET /albums/{id}/photos}, or filtered by {@code albumId} query param.</p>
 *
 * <p>Both {@code url} and {@code thumbnailUrl} are synthetic placeholder URLs
 * (e.g., {@code "https://via.placeholder.com/600/92c952"}) that return actual
 * images in a live environment.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Photo {

    /** Server-assigned photo identifier (1–5000). */
    private int id;

    /** Foreign key linking the photo to its parent album. */
    private int albumId;

    /** Photo caption / title. */
    private String title;

    /** Full-resolution placeholder image URL. */
    private String url;

    /** Thumbnail-resolution placeholder image URL. */
    private String thumbnailUrl;
}
