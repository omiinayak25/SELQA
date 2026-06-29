package com.omiinqa.api.models.booking;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Check-in / check-out date range within a Restful-Booker {@link Booking}.
 *
 * <p>Restful-Booker stores dates as ISO-8601 date strings (YYYY-MM-DD).
 * This class is a nested object within {@link Booking} and is serialized
 * as the {@code bookingdates} field.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BookingDates {

    /** ISO-8601 check-in date string, e.g., {@code "2024-01-15"}. */
    private String checkin;

    /** ISO-8601 check-out date string, e.g., {@code "2024-01-20"}. */
    private String checkout;
}
