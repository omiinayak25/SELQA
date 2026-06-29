package com.omiinqa.reference.orders;

/**
 * Accepted payment methods in the reference orders domain.
 *
 * <p>Checkout validates the supplied token against this enum. An unrecognised
 * payment method token causes {@link CheckoutService} to raise
 * {@code CHK_BAD_PAYMENT}.</p>
 */
public enum PaymentMethod {

    /** Credit or debit card (Visa, Mastercard, etc.). */
    CREDIT_CARD,

    /** PayPal digital wallet. */
    PAYPAL,

    /** Apple Pay digital wallet. */
    APPLE_PAY,

    /** Bank transfer / ACH. */
    BANK_TRANSFER
}
