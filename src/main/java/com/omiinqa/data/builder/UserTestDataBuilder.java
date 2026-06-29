package com.omiinqa.data.builder;

import com.omiinqa.data.faker.TestDataFaker;
import com.omiinqa.data.model.Address;
import com.omiinqa.data.model.User;

/**
 * Fluent test-data builder for {@link User} instances.
 *
 * <p><strong>Pattern:</strong> Builder — provides a readable, fluent API for
 * constructing {@link User} objects with sensible defaults backed by
 * {@link TestDataFaker}. Unlike the Lombok-generated {@code User.builder()},
 * this class adds <em>test-specific intelligence</em>:
 * <ul>
 *   <li>{@link #random()} fills every field with realistic fake data in one
 *       call, enabling quick object creation with no boilerplate.</li>
 *   <li>Individual {@code with*()} setters allow selective override —
 *       e.g., set a known email while generating a random password.</li>
 *   <li>Default values are applied for any field not explicitly set, so
 *       the built object is always valid unless you intentionally call
 *       {@link #withEmail(String) withEmail(null)}.</li>
 * </ul>
 * </p>
 *
 * <p><strong>Usage:</strong>
 * <pre>{@code
 *   // Fully random, valid user
 *   User user = UserTestDataBuilder.aUser().random().build();
 *
 *   // Override specific fields
 *   User adminUser = UserTestDataBuilder.aUser()
 *       .random()
 *       .withEmail("admin@example.com")
 *       .withUsername("admin")
 *       .build();
 *
 *   // Missing-email scenario
 *   User badUser = UserTestDataBuilder.aUser().random().withEmail(null).build();
 * }</pre>
 * </p>
 */
public final class UserTestDataBuilder {

    private final TestDataFaker faker;

    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private String username;
    private String password;
    private String phone;
    private Address address;

    private UserTestDataBuilder() {
        this.faker = new TestDataFaker();
    }

    // -------------------------------------------------------------------------
    // Entry point
    // -------------------------------------------------------------------------

    /**
     * Creates a new builder instance with no pre-set values.
     * Call {@link #random()} to populate all fields, then override as needed.
     *
     * @return fresh builder instance
     */
    public static UserTestDataBuilder aUser() {
        return new UserTestDataBuilder();
    }

    // -------------------------------------------------------------------------
    // Bulk fill
    // -------------------------------------------------------------------------

    /**
     * Fills every field with realistic, randomly-generated data using
     * {@link TestDataFaker}. Any field may be overridden after this call.
     *
     * @return {@code this} for chaining
     */
    public UserTestDataBuilder random() {
        this.id        = faker.randomUuid();
        this.firstName = faker.randomFirstName();
        this.lastName  = faker.randomLastName();
        this.email     = faker.randomEmail();
        this.username  = faker.randomUsername();
        this.password  = faker.randomPassword();
        this.phone     = faker.randomPhone();
        this.address   = AddressTestDataBuilder.anAddress().random().build();
        return this;
    }

    // -------------------------------------------------------------------------
    // Individual field setters
    // -------------------------------------------------------------------------

    /** @return {@code this} */
    public UserTestDataBuilder withId(final String id) {
        this.id = id;
        return this;
    }

    /** @return {@code this} */
    public UserTestDataBuilder withFirstName(final String firstName) {
        this.firstName = firstName;
        return this;
    }

    /** @return {@code this} */
    public UserTestDataBuilder withLastName(final String lastName) {
        this.lastName = lastName;
        return this;
    }

    /** @return {@code this} */
    public UserTestDataBuilder withEmail(final String email) {
        this.email = email;
        return this;
    }

    /** @return {@code this} */
    public UserTestDataBuilder withUsername(final String username) {
        this.username = username;
        return this;
    }

    /** @return {@code this} */
    public UserTestDataBuilder withPassword(final String password) {
        this.password = password;
        return this;
    }

    /** @return {@code this} */
    public UserTestDataBuilder withPhone(final String phone) {
        this.phone = phone;
        return this;
    }

    /** @return {@code this} */
    public UserTestDataBuilder withAddress(final Address address) {
        this.address = address;
        return this;
    }

    // -------------------------------------------------------------------------
    // Terminal operation
    // -------------------------------------------------------------------------

    /**
     * Builds and returns the configured {@link User} instance.
     *
     * @return a new {@link User} populated from this builder's fields
     */
    public User build() {
        return User.builder()
                .id(id)
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .username(username)
                .password(password)
                .phone(phone)
                .address(address)
                .build();
    }
}
