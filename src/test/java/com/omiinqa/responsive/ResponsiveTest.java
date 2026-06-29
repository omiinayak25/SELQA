package com.omiinqa.responsive;

import com.omiinqa.core.BaseTest;
import com.omiinqa.pages.saucedemo.LoginPage;
import com.omiinqa.pages.saucedemo.ProductsPage;
import com.omiinqa.utils.Viewport;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Responsive layer: drives the browser through device viewports and asserts the
 * application remains usable (renders, key elements present) at each breakpoint.
 * One test body, many viewports — the value is in the {@link Viewport} matrix,
 * not duplicated logic.
 */
@Epic("Responsive")
@Feature("Viewport matrix")
public class ResponsiveTest extends BaseTest {

    @DataProvider(name = "viewports")
    public Object[][] viewports() {
        return new Object[][]{
                {Viewport.MOBILE_PORTRAIT},
                {Viewport.MOBILE_LANDSCAPE},
                {Viewport.TABLET_PORTRAIT},
                {Viewport.TABLET_LANDSCAPE},
                {Viewport.LAPTOP},
                {Viewport.DESKTOP},
        };
    }

    @Test(groups = {"responsive", "regression"}, dataProvider = "viewports")
    public void loginPageRendersAtViewport(final Viewport viewport) {
        viewport.applyTo(driver());
        final LoginPage login = new LoginPage().open();
        assertThat(login.pageTitle()).as("title at %s", viewport).isEqualTo("Swag Labs");
    }

    @Test(groups = {"responsive", "regression"}, dataProvider = "viewports")
    public void inventoryUsableAtViewport(final Viewport viewport) {
        viewport.applyTo(driver());
        final ProductsPage products = new LoginPage().open().login("standard_user", "secret_sauce");
        assertThat(products.isLoaded()).as("inventory loaded at %s", viewport).isTrue();
        assertThat(products.getProductCount()).isEqualTo(6);
    }
}
