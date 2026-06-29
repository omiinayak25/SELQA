package com.omiinqa.bdd.steps.domain;

import com.omiinqa.reference.catalog.Product;

import java.math.BigDecimal;
import java.util.List;

/**
 * Canonical product set used by all catalog/search/filter/pagination BDD features.
 * Defined once here so every feature shares the exact same data.
 *
 * <p>12 products spread across 4 categories (Electronics, Clothing, Home, Sports),
 * 4 brands, prices from $9.99 to $999.99, ratings 2.5–5.0, 6 in-stock / 6 out-of-stock.</p>
 */
public final class CatalogFixtures {

    private CatalogFixtures() { }

    public static List<Product.ProductBuilder> standardProducts() {
        return List.of(
            // Electronics
            Product.builder().name("Wireless Headphones").category("Electronics").brand("SoundMax")
                    .price(new BigDecimal("79.99")).rating(4.5).inStock(true)
                    .tags(List.of("audio", "wireless", "headphones")),
            Product.builder().name("Bluetooth Speaker").category("Electronics").brand("SoundMax")
                    .price(new BigDecimal("49.99")).rating(4.2).inStock(true)
                    .tags(List.of("audio", "wireless", "speaker", "bluetooth")),
            Product.builder().name("Smart Watch").category("Electronics").brand("TechWear")
                    .price(new BigDecimal("249.99")).rating(4.7).inStock(true)
                    .tags(List.of("wearable", "smart", "watch")),
            Product.builder().name("Laptop Stand").category("Electronics").brand("DeskPro")
                    .price(new BigDecimal("39.99")).rating(3.8).inStock(false)
                    .tags(List.of("desk", "stand", "laptop", "ergonomic")),
            // Clothing
            Product.builder().name("Running Shoes").category("Clothing").brand("SprintGear")
                    .price(new BigDecimal("89.99")).rating(4.3).inStock(true)
                    .tags(List.of("shoes", "running", "sport")),
            Product.builder().name("Sports T-Shirt").category("Clothing").brand("SprintGear")
                    .price(new BigDecimal("24.99")).rating(3.9).inStock(false)
                    .tags(List.of("shirt", "sport", "clothing")),
            Product.builder().name("Yoga Pants").category("Clothing").brand("FlexFit")
                    .price(new BigDecimal("54.99")).rating(4.6).inStock(true)
                    .tags(List.of("yoga", "pants", "fitness", "sport")),
            // Home
            Product.builder().name("Coffee Maker").category("Home").brand("BrewMaster")
                    .price(new BigDecimal("129.99")).rating(4.4).inStock(false)
                    .tags(List.of("coffee", "kitchen", "appliance")),
            Product.builder().name("Air Purifier").category("Home").brand("BrewMaster")
                    .price(new BigDecimal("199.99")).rating(4.8).inStock(false)
                    .tags(List.of("air", "purifier", "home", "health")),
            Product.builder().name("Desk Lamp").category("Home").brand("DeskPro")
                    .price(new BigDecimal("34.99")).rating(3.5).inStock(true)
                    .tags(List.of("lamp", "desk", "lighting")),
            // Sports
            Product.builder().name("Fitness Tracker").category("Sports").brand("TechWear")
                    .price(new BigDecimal("149.99")).rating(4.1).inStock(false)
                    .tags(List.of("fitness", "tracker", "wearable", "sport")),
            Product.builder().name("Resistance Bands").category("Sports").brand("FlexFit")
                    .price(new BigDecimal("19.99")).rating(4.0).inStock(true)
                    .tags(List.of("resistance", "bands", "fitness", "sport"))
        );
    }
}
