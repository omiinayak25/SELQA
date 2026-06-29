package com.omiinqa.api.models.booking;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Core booking record used for create (POST), update (PUT/PATCH) and read
 * (GET) operations on the Restful-Booker API.
 *
 * <p>The {@code depositpaid} and {@code bookingdates} fields use
 * {@link JsonProperty} to map JSON's snake-case keys to idiomatic Java
 * camelCase names while preserving exact wire-format compatibility.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Booking {

    /** Guest's first name. */
    @JsonProperty("firstname")
    private String firstName;

    /** Guest's last name. */
    @JsonProperty("lastname")
    private String lastName;

    /** Total price for the stay in the API's implicit currency. */
    @JsonProperty("totalprice")
    private int totalPrice;

    /** Whether a deposit has been paid. */
    @JsonProperty("depositpaid")
    private boolean depositPaid;

    /** Check-in and check-out dates. */
    @JsonProperty("bookingdates")
    private BookingDates bookingDates;

    /** Optional additional needs / special requests. */
    @JsonProperty("additionalneeds")
    private String additionalNeeds;
}
