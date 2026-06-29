package com.omiinqa.bdd.steps.domain;

import com.omiinqa.bdd.support.DomainWorld;
import com.omiinqa.reference.commerce.CartService;
import com.omiinqa.reference.commerce.ProductRegistry;
import com.omiinqa.reference.commerce.WishlistService;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for the reference commerce / wishlist domain.
 *
 * <p>The wishlist service shares the same {@link ProductRegistry} as the cart
 * service so stock checks during {@code moveToCart} are consistent. All
 * mutating actions flow through {@link DomainWorld#run} so
 * {@code CommonDomainSteps} can assert outcomes without any extra wiring.</p>
 *
 * <p>Step text is prefixed with wishlist-domain nouns to avoid Cucumber
 * ambiguity with cart or authentication steps.</p>
 */
public class WishlistSteps {

    private static final String REGISTRY_KEY  = "commerce.registry";
    private static final String CART_KEY      = "commerce.cart";
    private static final String WISHLIST_KEY  = "commerce.wishlist";

    // ------------------------------------------------------------------ //
    //  Service accessors                                                   //
    // ------------------------------------------------------------------ //

    private ProductRegistry registry() {
        return DomainWorld.service(REGISTRY_KEY, ProductRegistry::new);
    }

    private CartService cart() {
        final ProductRegistry reg = registry();
        return DomainWorld.service(CART_KEY, () -> new CartService(reg));
    }

    private WishlistService wishlist() {
        final ProductRegistry reg = registry();
        return DomainWorld.service(WISHLIST_KEY, () -> new WishlistService(reg));
    }

    // ------------------------------------------------------------------ //
    //  Given — state setup                                                 //
    // ------------------------------------------------------------------ //

    /** Reset registry, wishlist, and cart for a fresh wishlist scenario. */
    @Given("a clean wishlist, cart and product registry")
    public void cleanWishlistCartAndRegistry() {
        final ProductRegistry reg = new ProductRegistry();
        DomainWorld.put(REGISTRY_KEY, reg);
        DomainWorld.put(CART_KEY,     new CartService(reg));
        DomainWorld.put(WISHLIST_KEY, new WishlistService(reg));
    }

    /** Pre-populate the wishlist with a product without going through the error-capture path. */
    @Given("product {string} is already in the wishlist")
    public void productAlreadyInWishlist(final String productId) {
        wishlist().add(productId);
    }

    /** Adjust stock level for a product to simulate out-of-stock conditions in wishlist scenarios. */
    @Given("product {string} has {int} units remaining in stock")
    public void productStockLevel(final String productId, final int units) {
        registry().stock(productId).setAvailable(units);
    }

    // ------------------------------------------------------------------ //
    //  When — actions                                                      //
    // ------------------------------------------------------------------ //

    @When("I add product {string} to the wishlist")
    public void addProductToWishlist(final String productId) {
        DomainWorld.run(() -> wishlist().add(productId));
    }

    @When("I remove product {string} from the wishlist")
    public void removeProductFromWishlist(final String productId) {
        DomainWorld.run(() -> wishlist().remove(productId));
    }

    @When("I move product {string} from the wishlist to the cart")
    public void moveProductFromWishlistToCart(final String productId) {
        DomainWorld.run(() -> wishlist().moveToCart(productId, cart()));
    }

    // ------------------------------------------------------------------ //
    //  Then — assertions                                                   //
    // ------------------------------------------------------------------ //

    @Then("the wishlist contains product {string}")
    public void wishlistContainsProduct(final String productId) {
        assertThat(wishlist().contains(productId))
                .as("wishlist should contain product %s", productId)
                .isTrue();
    }

    @Then("the wishlist does not contain product {string}")
    public void wishlistDoesNotContainProduct(final String productId) {
        assertThat(wishlist().contains(productId))
                .as("wishlist should NOT contain product %s", productId)
                .isFalse();
    }

    @Then("the wishlist has {int} items")
    public void wishlistHasItems(final int expected) {
        assertThat(wishlist().size())
                .as("wishlist size")
                .isEqualTo(expected);
    }

    @Then("the wishlist is empty")
    public void wishlistIsEmpty() {
        assertThat(wishlist().size())
                .as("wishlist should be empty")
                .isZero();
    }

    @Then("the moved product {string} is now in the cart")
    public void movedProductIsInCart(final String productId) {
        assertThat(cart().lines()).containsKey(productId);
    }

    @Then("the moved product {string} is no longer in the wishlist")
    public void movedProductNotInWishlist(final String productId) {
        assertThat(wishlist().contains(productId))
                .as("product %s should have been removed from wishlist after move", productId)
                .isFalse();
    }
}
