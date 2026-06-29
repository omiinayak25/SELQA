package com.omiinqa.ui.saucedemo;

import com.omiinqa.businessflows.AddToCartFlow;
import com.omiinqa.businessflows.LoginFlow;
import com.omiinqa.core.BaseTest;
import com.omiinqa.pages.saucedemo.CartPage;
import com.omiinqa.pages.saucedemo.CheckoutStepOnePage;
import com.omiinqa.pages.saucedemo.CheckoutStepTwoPage;
import com.omiinqa.pages.saucedemo.ProductsPage;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

/**
 * Checkout order-summary (step 2) financial totals validation.
 *
 * <p>All three labels — subtotal, tax, and total — are returned by the page as
 * strings such as {@code "Item total: $29.99"}, {@code "Tax: $2.40"}, and
 * {@code "Total: $32.39"}. This class parses those strings to {@code double}
 * values and validates the mathematical relationships:</p>
 * <ul>
 *   <li>Subtotal equals the sum of individual item prices.</li>
 *   <li>Tax is approximately 8 % of the subtotal (±$0.02 tolerance for rounding).</li>
 *   <li>Total equals subtotal plus tax (±$0.01 tolerance).</li>
 * </ul>
 *
 * <p>The {@code productSubsets} data provider drives multi-product combinations
 * so the arithmetic is verified across several distinct subtotal values, not just
 * one fixed scenario.</p>
 */
@Epic("SauceDemo")
@Feature("Checkout Totals")
public class CheckoutTotalsTest extends BaseTest {

    private static final String FIRST = "Jane";
    private static final String LAST  = "Smith";
    private static final String ZIP   = "90210";

    private static final double TAX_RATE = 0.08;

    // --------------------------------------------------------- helper

    /**
     * Parses a price label string of the form {@code "Item total: $29.99"} or
     * {@code "Tax: $2.40"} or {@code "Total: $32.39"} into a {@code double}.
     *
     * @param label the raw label text returned by the page object
     * @return the numeric value
     * @throws NumberFormatException when the label does not contain a parseable
     *                               dollar amount
     */
    private static double parseDollarAmount(final String label) {
        final int dollarIdx = label.indexOf('$');
        if (dollarIdx < 0) {
            throw new NumberFormatException("No '$' found in label: " + label);
        }
        return Double.parseDouble(label.substring(dollarIdx + 1).trim());
    }

    /**
     * Navigates from a fresh login to the checkout step-two (overview) page for
     * the given list of products.
     *
     * @param productNames products to add to the cart before checking out
     * @return the loaded {@link CheckoutStepTwoPage}
     */
    private CheckoutStepTwoPage reachOrderOverview(final List<String> productNames) {
        final ProductsPage products = LoginFlow.loginAsStandardUser();
        productNames.forEach(products::addToCart);
        final CartPage cart = products.openCart();
        final CheckoutStepOnePage step1 = cart.checkout();
        step1.enterCustomerInfo(FIRST, LAST, ZIP);
        return step1.continueToOverview();
    }

    // --------------------------------------------------------- data providers

    /**
     * Provides product subsets with their known combined item prices.
     * Values are taken from the canonical catalogue in FOUNDATION_CONTRACT.
     *
     * <p>Rows: [productNames, expectedSubtotal]</p>
     *
     * @return matrix of [List&lt;String&gt; productNames, double expectedSubtotal]
     */
    @DataProvider(name = "productSubsets")
    public Object[][] productSubsets() {
        return new Object[][] {
                {List.of("Sauce Labs Backpack"),                                    29.99},
                {List.of("Sauce Labs Bike Light"),                                  9.99},
                {List.of("Sauce Labs Backpack", "Sauce Labs Bike Light"),          39.98},
                {List.of("Sauce Labs Bolt T-Shirt", "Sauce Labs Fleece Jacket"),   65.98},
                {List.of("Sauce Labs Onesie", "Test.allTheThings() T-Shirt (Red)"),23.98},
        };
    }

    // --------------------------------------------------------- test methods

    /**
     * Asserts the displayed subtotal on the overview page equals the expected sum
     * of individual item prices for each product subset.
     *
     * @param productNames     products to purchase
     * @param expectedSubtotal expected combined item price
     */
    @Test(groups = {"ui", "regression"},
            dataProvider = "productSubsets",
            description = "Subtotal on overview equals the sum of individual product prices")
    @Severity(SeverityLevel.CRITICAL)
    @Description("The subtotal label must match the arithmetic sum of all item prices.")
    public void subtotalMatchesItemPriceSum(final List<String> productNames,
                                             final double expectedSubtotal) {
        final CheckoutStepTwoPage overview = reachOrderOverview(productNames);

        final double actualSubtotal = parseDollarAmount(overview.getSummarySubtotal());

        assertThat(actualSubtotal)
                .as("subtotal for products %s", productNames)
                .isCloseTo(expectedSubtotal, offset(0.01));
    }

    /**
     * Asserts the displayed tax approximates 8 % of the subtotal for each product
     * subset, allowing a ±$0.02 tolerance for banker's rounding.
     *
     * @param productNames     products to purchase
     * @param expectedSubtotal subtotal for computing the expected tax
     */
    @Test(groups = {"ui", "regression"},
            dataProvider = "productSubsets",
            description = "Tax on overview is approximately 8% of the subtotal")
    @Severity(SeverityLevel.CRITICAL)
    @Description("The tax label must be approximately 8% of the subtotal, within $0.02 rounding tolerance.")
    public void taxApproximatesEightPercentOfSubtotal(final List<String> productNames,
                                                       final double expectedSubtotal) {
        final CheckoutStepTwoPage overview = reachOrderOverview(productNames);

        final double actualTax = parseDollarAmount(overview.getTax());
        final double expectedTax = expectedSubtotal * TAX_RATE;

        assertThat(actualTax)
                .as("tax for subtotal %.2f", expectedSubtotal)
                .isCloseTo(expectedTax, offset(0.02));
    }

    /**
     * Asserts the displayed total equals subtotal + tax for each product subset,
     * verifying the total arithmetic shown on screen.
     *
     * @param productNames     products to purchase
     * @param expectedSubtotal subtotal for deriving expected total
     */
    @Test(groups = {"ui", "regression"},
            dataProvider = "productSubsets",
            description = "Total on overview equals subtotal plus tax")
    @Severity(SeverityLevel.CRITICAL)
    @Description("The total label must equal the sum of subtotal and tax labels on the overview page.")
    public void totalEqualsSubtotalPlusTax(final List<String> productNames,
                                            final double expectedSubtotal) {
        final CheckoutStepTwoPage overview = reachOrderOverview(productNames);

        final double subtotal = parseDollarAmount(overview.getSummarySubtotal());
        final double tax      = parseDollarAmount(overview.getTax());
        final double total    = parseDollarAmount(overview.getTotal());

        assertThat(total)
                .as("total should equal subtotal + tax for products %s", productNames)
                .isCloseTo(subtotal + tax, offset(0.01));
    }

    /**
     * Asserts the overview page lists the same products that were added to the cart,
     * confirming the order summary does not omit or duplicate any item.
     */
    @Test(groups = {"ui", "regression"},
            description = "Order overview lists all purchased products with no omissions")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Every product added to the cart must appear in the checkout overview item list.")
    public void overviewListsAllPurchasedItems() {
        final List<String> toAdd = List.of(
                "Sauce Labs Backpack",
                "Sauce Labs Fleece Jacket",
                "Sauce Labs Onesie"
        );
        final CheckoutStepTwoPage overview = reachOrderOverview(toAdd);

        assertThat(overview.getItemNames())
                .as("items in order overview")
                .containsExactlyInAnyOrderElementsOf(toAdd);
    }

    /**
     * Asserts the subtotal label string starts with the expected "Item total:" prefix,
     * confirming the label format matches the expected UI pattern.
     */
    @Test(groups = {"ui", "regression"},
            description = "Subtotal label has 'Item total:' prefix")
    @Severity(SeverityLevel.MINOR)
    @Description("The subtotal label text must begin with 'Item total:' as per the SauceDemo UI.")
    public void subtotalLabelHasCorrectPrefix() {
        final CheckoutStepTwoPage overview = reachOrderOverview(
                List.of("Sauce Labs Backpack"));

        assertThat(overview.getSummarySubtotal())
                .as("subtotal label prefix")
                .startsWith("Item total:");
    }

    /**
     * Asserts the tax label string starts with the expected "Tax:" prefix.
     */
    @Test(groups = {"ui", "regression"},
            description = "Tax label has 'Tax:' prefix")
    @Severity(SeverityLevel.MINOR)
    @Description("The tax label text must begin with 'Tax:' as per the SauceDemo UI.")
    public void taxLabelHasCorrectPrefix() {
        final CheckoutStepTwoPage overview = reachOrderOverview(
                List.of("Sauce Labs Bike Light"));

        assertThat(overview.getTax())
                .as("tax label prefix")
                .startsWith("Tax:");
    }

    /**
     * Asserts the total label string starts with the expected "Total:" prefix.
     */
    @Test(groups = {"ui", "regression"},
            description = "Total label has 'Total:' prefix")
    @Severity(SeverityLevel.MINOR)
    @Description("The total label text must begin with 'Total:' as per the SauceDemo UI.")
    public void totalLabelHasCorrectPrefix() {
        final CheckoutStepTwoPage overview = reachOrderOverview(
                List.of("Sauce Labs Onesie"));

        assertThat(overview.getTotal())
                .as("total label prefix")
                .startsWith("Total:");
    }
}
