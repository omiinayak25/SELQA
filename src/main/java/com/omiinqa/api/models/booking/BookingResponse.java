package com.omiinqa.api.models.booking;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response envelope returned by {@code POST /booking} on Restful-Booker.
 *
 * <p>Restful-Booker wraps the newly created booking inside an envelope that
 * includes the server-assigned {@code bookingid}.  Tests must store this ID
 * to perform subsequent GET/PUT/PATCH/DELETE operations — the canonical
 * request-chaining scenario covered in {@code BookingCrudApiTest}.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BookingResponse {

    /** Server-assigned unique booking identifier. */
    @JsonProperty("bookingid")
    private int bookingId;

    /** The full booking record that was persisted. */
    private Booking booking;
}
