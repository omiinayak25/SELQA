package com.omiinqa.data.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Domain model representing an application user.
 *
 * <p><strong>Pattern:</strong> Immutable-friendly Value Object generated via
 * Lombok {@link Data} + {@link Builder}. Kept as a plain Java bean so it can
 * be deserialised by Jackson ({@code JsonDataReader}), constructed programmatically
 * via {@link com.omiinqa.data.builder.UserTestDataBuilder}, or populated by
 * {@link com.omiinqa.data.factory.UserFactory}.</p>
 *
 * <p>Fields are intentionally nullable — test scenarios frequently need
 * partially-populated objects (e.g., missing-email tests).</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    /** Unique user identifier (may be a numeric string from the SUT or a UUID). */
    private String id;

    /** User's given name. */
    private String firstName;

    /** User's family name. */
    private String lastName;

    /** Primary email address; drives login and notification flows. */
    private String email;

    /** Login username; unique within the system. */
    private String username;

    /**
     * Plain-text password for test fixture purposes only.
     * Never use real credentials in test data files committed to version control.
     */
    private String password;

    /** Contact telephone number in any format accepted by the SUT. */
    private String phone;

    /** Postal address associated with the account. */
    private Address address;
}
