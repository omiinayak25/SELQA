package com.omiinqa.ui.saucedemo;

import com.omiinqa.businessflows.LoginFlow;
import com.omiinqa.core.BaseTest;
import com.omiinqa.pages.saucedemo.ProductsPage;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.testng.annotations.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SauceDemo inventory tests — listing, cart badge and sorting behavior.
 */
@Epic("SauceDemo")
@Feature("Products")
public class ProductsTest extends BaseTest {

    @Test(groups = {"ui", "smoke", "regression"})
    public void inventoryListsAllProducts() {
        final ProductsPage products = LoginFlow.loginAsStandardUser();
        assertThat(products.getProductNames())
                .hasSize(6)
                .contains("Sauce Labs Backpack", "Sauce Labs Bike Light");
    }

    @Test(groups = {"ui", "regression"})
    public void addingItemsUpdatesCartBadge() {
        final ProductsPage products = LoginFlow.loginAsStandardUser();
        assertThat(products.getCartBadgeCount()).isZero();

        products.addToCart("Sauce Labs Backpack");
        products.addToCart("Sauce Labs Bike Light");
        assertThat(products.getCartBadgeCount()).isEqualTo(2);

        products.removeFromCart("Sauce Labs Backpack");
        assertThat(products.getCartBadgeCount()).isEqualTo(1);
    }

    @Test(groups = {"ui", "regression"})
    public void sortByNameDescendingReordersList() {
        final ProductsPage products = LoginFlow.loginAsStandardUser();
        products.sortBy("Name (Z to A)");

        final List<String> names = products.getProductNames();
        final List<String> expected = names.stream().sorted((a, b) -> b.compareTo(a)).toList();
        assertThat(names).containsExactlyElementsOf(expected);
    }
}
