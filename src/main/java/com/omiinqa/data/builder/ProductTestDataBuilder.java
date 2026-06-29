package com.omiinqa.data.builder;

import com.omiinqa.data.faker.TestDataFaker;
import com.omiinqa.data.model.Product;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Fluent test-data builder for {@link Product} instances.
 *
 * <p><strong>Pattern:</strong> Builder — provides a {@link #random()} bulk-fill
 * method backed by {@link TestDataFaker} plus individual {@code with*()}
 * overrides. Useful for cart, search, and checkout test scenarios where a
 * realistic product object is needed without hitting the database.</p>
 *
 * <p><strong>Usage:</strong>
 * <pre>{@code
 *   Product p = ProductTestDataBuilder.aProduct().random().build();
 *
 *   Product expensive = ProductTestDataBuilder.aProduct()
 *       .random()
 *       .withPrice(new BigDecimal("999.99"))
 *       .build();
 * }</pre>
 * </p>
 */
public final class ProductTestDataBuilder {

    private final TestDataFaker faker;

    private String id;
    private String name;
    private BigDecimal price;
    private String category;
    private String description;
    private String imageUrl;

    private ProductTestDataBuilder() {
        this.faker = new TestDataFaker();
    }

    /**
     * Entry point — creates a new builder with no pre-set values.
     *
     * @return fresh builder instance
     */
    public static ProductTestDataBuilder aProduct() {
        return new ProductTestDataBuilder();
    }

    /**
     * Fills every field with realistic, randomly-generated product data.
     *
     * @return {@code this} for chaining
     */
    public ProductTestDataBuilder random() {
        this.id          = faker.randomUuid();
        this.name        = faker.randomProductName();
        this.price       = BigDecimal.valueOf(
                Double.parseDouble(faker.randomPrice(5.00, 499.99)))
                .setScale(2, RoundingMode.HALF_UP);
        this.category    = faker.randomCategory();
        this.description = faker.raw().lorem().sentence();
        this.imageUrl    = faker.raw().internet().image();
        return this;
    }

    /** @return {@code this} */
    public ProductTestDataBuilder withId(final String id) {
        this.id = id;
        return this;
    }

    /** @return {@code this} */
    public ProductTestDataBuilder withName(final String name) {
        this.name = name;
        return this;
    }

    /** @return {@code this} */
    public ProductTestDataBuilder withPrice(final BigDecimal price) {
        this.price = price;
        return this;
    }

    /** @return {@code this} */
    public ProductTestDataBuilder withCategory(final String category) {
        this.category = category;
        return this;
    }

    /** @return {@code this} */
    public ProductTestDataBuilder withDescription(final String description) {
        this.description = description;
        return this;
    }

    /** @return {@code this} */
    public ProductTestDataBuilder withImageUrl(final String imageUrl) {
        this.imageUrl = imageUrl;
        return this;
    }

    /**
     * Builds and returns the configured {@link Product} instance.
     *
     * @return populated Product
     */
    public Product build() {
        return Product.builder()
                .id(id)
                .name(name)
                .price(price)
                .category(category)
                .description(description)
                .imageUrl(imageUrl)
                .build();
    }
}
