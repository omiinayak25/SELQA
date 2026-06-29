package com.omiinqa.api.models.jsonplaceholder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a user resource from the JSONPlaceholder REST API
 * ({@code https://jsonplaceholder.typicode.com/users}).
 *
 * <p>JSONPlaceholder exposes 10 pre-seeded users (IDs 1–10), each with a rich
 * nested structure: top-level scalar fields plus an {@link Address} (which itself
 * contains {@link Geo}) and a {@link Company} sub-object.  This POJO captures
 * the full shape so tests can assert deep field values without raw JSONPath
 * extraction.</p>
 *
 * <p><b>Design notes:</b></p>
 * <ul>
 *   <li>Lombok annotations eliminate ~60 lines of boilerplate.</li>
 *   <li>{@code @JsonIgnoreProperties(ignoreUnknown = true)} future-proofs
 *       deserialization against API additions.</li>
 *   <li>Nested objects ({@link Address}, {@link Company}) are kept in separate
 *       files for reuse and single-responsibility.</li>
 * </ul>
 *
 * @see Address
 * @see Company
 * @see Geo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class JsonPlaceholderUser {

    /** Server-assigned user identifier (1–10 for pre-seeded data). */
    private int id;

    /** Display name (e.g., {@code "Leanne Graham"}). */
    private String name;

    /** Login username (e.g., {@code "Bret"}). */
    private String username;

    /** User email address (e.g., {@code "Sincere@april.biz"}). */
    private String email;

    /** Mailing / physical address. */
    private Address address;

    /** User phone number (may contain extensions). */
    private String phone;

    /** Personal or company website URL. */
    private String website;

    /** Employer information. */
    private Company company;
}
