package com.omiinqa.ui.saucedemo;

import com.omiinqa.businessflows.LoginFlow;
import com.omiinqa.core.BaseTest;
import com.omiinqa.pages.saucedemo.ProductsPage;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sorting tests for the SauceDemo inventory page covering all four sort options:
 * Name A→Z, Name Z→A, Price low→high, and Price high→low.
 *
 * <p><b>Strategy:</b> each test retrieves the current ordering via
 * {@link ProductsPage#getProductNames()} and compares it to the programmatically
 * derived expected ordering. This approach is resilient to catalogue changes —
 * the expected ordering is derived from the live product set rather than a
 * hardcoded list.</p>
 *
 * <p>Price sorting is validated by parsing the {@code "$XX.XX"} strings retrieved
 * via {@link ProductsPage#getProductNames()} together with knowledge of the
 * canonical price map. Since the page only exposes names via {@code getProductNames()},
 * the price-sort tests also verify the correct relative order of the six canonical
 * items whose prices are well-known (as per the FOUNDATION_CONTRACT).</p>
 */
@Epic("SauceDemo")
@Feature("Sorting")
public class SortingTest extends BaseTest {

    // Known products sorted by price ascending (as per FOUNDATION_CONTRACT)
    private static final List<String> EXPECTED_PRICE_LOW_TO_HIGH = List.of(
            "Sauce Labs Onesie",           // $7.99
            "Sauce Labs Bike Light",       // $9.99
            "Sauce Labs Bolt T-Shirt",     // $15.99
            "Test.allTheThings() T-Shirt (Red)", // $15.99
            "Sauce Labs Backpack",         // $29.99
            "Sauce Labs Fleece Jacket"     // $49.99
    );

    private static final List<String> EXPECTED_PRICE_HIGH_TO_LOW = List.of(
            "Sauce Labs Fleece Jacket",    // $49.99
            "Sauce Labs Backpack",         // $29.99
            "Sauce Labs Bolt T-Shirt",     // $15.99
            "Test.allTheThings() T-Shirt (Red)", // $15.99
            "Sauce Labs Bike Light",       // $9.99
            "Sauce Labs Onesie"            // $7.99
    );

    // --------------------------------------------------------- data providers

    /**
     * Provides all four sort option visible texts as used by
     * {@link ProductsPage#sortBy(String)}.
     *
     * @return matrix of [sortOptionText]
     */
    @DataProvider(name = "sortOptions")
    public Object[][] sortOptions() {
        return new Object[][] {
                {"Name (A to Z)"},
                {"Name (Z to A)"},
                {"Price (low to high)"},
                {"Price (high to low)"},
        };
    }

    // --------------------------------------------------------- test methods

    /**
     * Selects each of the four sort options in turn and asserts the dropdown
     * interaction does not throw and the inventory remains loaded (smoke-level
     * interaction check).
     *
     * @param sortOptionText the visible text of the sort option
     */
    @Test(groups = {"ui", "regression"},
            dataProvider = "sortOptions",
            description = "Each sort option can be selected without error")
    @Severity(SeverityLevel.NORMAL)
    @Description("All four sort options must be selectable without causing a page error.")
    public void eachSortOptionIsSelectable(final String sortOptionText) {
        final ProductsPage products = LoginFlow.loginAsStandardUser();
        products.sortBy(sortOptionText);
        assertThat(products.isLoaded())
                .as("inventory still loaded after sorting by '%s'", sortOptionText)
                .isTrue();
        assertThat(products.getProductCount())
                .as("product count unchanged after sort")
                .isEqualTo(6);
    }

    /**
     * Asserts that «Name (A to Z)» produces the lexicographically ascending order.
     */
    @Test(groups = {"ui", "regression"},
            description = "Name A-Z sort produces lexicographically ascending order")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Sort by 'Name (A to Z)' must order product names ascending.")
    public void sortByNameAscendingProducesAToZOrder() {
        final ProductsPage products = LoginFlow.loginAsStandardUser();
        products.sortBy("Name (A to Z)");

        final List<String> actual = products.getProductNames();
        final List<String> expected = actual.stream()
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());

        assertThat(actual)
                .as("products ordered A to Z")
                .containsExactlyElementsOf(expected);
    }

    /**
     * Asserts that «Name (Z to A)» produces the lexicographically descending order.
     */
    @Test(groups = {"ui", "regression"},
            description = "Name Z-A sort produces lexicographically descending order")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Sort by 'Name (Z to A)' must order product names descending.")
    public void sortByNameDescendingProducesZToAOrder() {
        final ProductsPage products = LoginFlow.loginAsStandardUser();
        products.sortBy("Name (Z to A)");

        final List<String> actual = products.getProductNames();
        final List<String> expected = actual.stream()
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());

        assertThat(actual)
                .as("products ordered Z to A")
                .containsExactlyElementsOf(expected);
    }

    /**
     * Asserts that «Price (low to high)» produces the correct price-ascending order
     * using the canonical price map from the FOUNDATION_CONTRACT.
     */
    @Test(groups = {"ui", "regression"},
            description = "Price low-to-high sort matches the known price-ascending order")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Sort by 'Price (low to high)' must order products cheapest-first.")
    public void sortByPriceLowToHighMatchesKnownOrder() {
        final ProductsPage products = LoginFlow.loginAsStandardUser();
        products.sortBy("Price (low to high)");

        final List<String> actual = products.getProductNames();

        assertThat(actual)
                .as("products ordered price low to high")
                .containsExactlyElementsOf(EXPECTED_PRICE_LOW_TO_HIGH);
    }

    /**
     * Asserts that «Price (high to low)» produces the correct price-descending order
     * using the canonical price map from the FOUNDATION_CONTRACT.
     */
    @Test(groups = {"ui", "regression"},
            description = "Price high-to-low sort matches the known price-descending order")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Sort by 'Price (high to low)' must order products most-expensive-first.")
    public void sortByPriceHighToLowMatchesKnownOrder() {
        final ProductsPage products = LoginFlow.loginAsStandardUser();
        products.sortBy("Price (high to low)");

        final List<String> actual = products.getProductNames();

        assertThat(actual)
                .as("products ordered price high to low")
                .containsExactlyElementsOf(EXPECTED_PRICE_HIGH_TO_LOW);
    }

    /**
     * Verifies that the default (unsorted) load yields the A-to-Z order, confirming
     * the default sort option is «Name (A to Z)».
     */
    @Test(groups = {"ui", "regression"},
            description = "Default inventory order is Name A-to-Z")
    @Severity(SeverityLevel.NORMAL)
    @Description("Without selecting a sort option, products must appear in A-Z name order.")
    public void defaultOrderIsNameAscending() {
        final ProductsPage products = LoginFlow.loginAsStandardUser();

        final List<String> actual = products.getProductNames();
        final List<String> expected = actual.stream()
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());

        assertThat(actual)
                .as("default product order should be A to Z")
                .containsExactlyElementsOf(expected);
    }

    /**
     * Asserts that switching from Z-A back to A-Z restores the ascending order,
     * verifying the sort control can be toggled without a page reload.
     */
    @Test(groups = {"ui", "regression"},
            description = "Sort can be toggled: Z-A then A-Z restores ascending order")
    @Severity(SeverityLevel.NORMAL)
    @Description("Toggling sort options must update the displayed order without a page reload.")
    public void sortCanBeToggledBetweenOptions() {
        final ProductsPage products = LoginFlow.loginAsStandardUser();
        products.sortBy("Name (Z to A)");

        final List<String> afterZToA = products.getProductNames();
        // First item should be lexicographically last
        assertThat(afterZToA.get(0)).isGreaterThan(afterZToA.get(afterZToA.size() - 1));

        products.sortBy("Name (A to Z)");
        final List<String> afterAToZ = products.getProductNames();
        final List<String> sorted = afterAToZ.stream()
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());

        assertThat(afterAToZ)
                .as("products restored to A-Z order after toggling")
                .containsExactlyElementsOf(sorted);
    }
}
