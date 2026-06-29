package com.omiinqa.data.builder;

import com.omiinqa.data.faker.TestDataFaker;
import com.omiinqa.data.model.Address;

/**
 * Fluent test-data builder for {@link Address} instances.
 *
 * <p><strong>Pattern:</strong> Builder — mirrors {@link UserTestDataBuilder}
 * in philosophy. Provides a {@link #random()} bulk-fill convenience alongside
 * individual {@code with*()} overrides. Commonly used by
 * {@link UserTestDataBuilder} to populate the embedded address field, but also
 * usable standalone for checkout / shipping tests.</p>
 *
 * <p><strong>Usage:</strong>
 * <pre>{@code
 *   Address addr = AddressTestDataBuilder.anAddress().random().build();
 *
 *   Address specific = AddressTestDataBuilder.anAddress()
 *       .random()
 *       .withZipCode("90210")
 *       .build();
 * }</pre>
 * </p>
 */
public final class AddressTestDataBuilder {

    private final TestDataFaker faker;

    private String street;
    private String city;
    private String state;
    private String zipCode;
    private String country;

    private AddressTestDataBuilder() {
        this.faker = new TestDataFaker();
    }

    /**
     * Entry point — creates a new builder with no pre-set values.
     *
     * @return fresh builder instance
     */
    public static AddressTestDataBuilder anAddress() {
        return new AddressTestDataBuilder();
    }

    /**
     * Fills every field with realistic, randomly-generated address data.
     *
     * @return {@code this} for chaining
     */
    public AddressTestDataBuilder random() {
        this.street  = faker.randomStreetAddress();
        this.city    = faker.randomCity();
        this.state   = faker.randomState();
        this.zipCode = faker.randomZipCode();
        this.country = "US";
        return this;
    }

    /** @return {@code this} */
    public AddressTestDataBuilder withStreet(final String street) {
        this.street = street;
        return this;
    }

    /** @return {@code this} */
    public AddressTestDataBuilder withCity(final String city) {
        this.city = city;
        return this;
    }

    /** @return {@code this} */
    public AddressTestDataBuilder withState(final String state) {
        this.state = state;
        return this;
    }

    /** @return {@code this} */
    public AddressTestDataBuilder withZipCode(final String zipCode) {
        this.zipCode = zipCode;
        return this;
    }

    /** @return {@code this} */
    public AddressTestDataBuilder withCountry(final String country) {
        this.country = country;
        return this;
    }

    /**
     * Builds and returns the configured {@link Address}.
     *
     * @return populated Address instance
     */
    public Address build() {
        return Address.builder()
                .street(street)
                .city(city)
                .state(state)
                .zipCode(zipCode)
                .country(country)
                .build();
    }
}
