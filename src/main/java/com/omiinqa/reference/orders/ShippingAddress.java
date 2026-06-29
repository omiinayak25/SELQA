package com.omiinqa.reference.orders;

import lombok.Builder;
import lombok.Value;

/**
 * A shipping address required for order checkout.
 *
 * <p>All four fields are mandatory. Missing or blank values cause the
 * checkout service to raise {@code CHK_BAD_ADDRESS} so scenarios can assert
 * the exact validation rule that fired.</p>
 */
@Value
@Builder
public class ShippingAddress {

    /** Recipient full name. */
    String recipientName;

    /** Street line (number + street name). */
    String street;

    /** City or town name. */
    String city;

    /** Postal / ZIP code. */
    String postalCode;
}
