package com.omiinqa.data.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Domain model representing a postal address.
 *
 * <p><strong>Pattern:</strong> Value Object — embedded within {@link User}
 * and used as a standalone data carrier for checkout / shipping flows.</p>
 *
 * <p>All fields are nullable to support partial-address test scenarios
 * (e.g., testing validation when {@code zipCode} is blank).</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Address {

    /** Street number and name, e.g. {@code "123 Main St"}. */
    private String street;

    /** City or locality name. */
    private String city;

    /** State, province, or region code. */
    private String state;

    /** Postal / ZIP code. */
    private String zipCode;

    /** ISO 3166-1 alpha-2 country code (e.g. {@code "US"}). */
    private String country;
}
