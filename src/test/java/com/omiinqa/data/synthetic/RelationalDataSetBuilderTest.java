package com.omiinqa.data.synthetic;

import com.omiinqa.data.model.Product;
import com.omiinqa.data.model.User;
import com.omiinqa.data.synthetic.RelationalDataSetBuilder.OrderRecord;
import com.omiinqa.data.synthetic.RelationalDataSetBuilder.RelationalDataSet;
import com.omiinqa.exceptions.DataException;
import org.assertj.core.api.SoftAssertions;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Offline unit tests for {@link RelationalDataSetBuilder} and the inner types
 * {@link RelationalDataSet} and {@link OrderRecord}.
 *
 * <p>The key invariant tested here is <em>referential integrity</em>: every
 * {@link OrderRecord#getUserId()} must correspond to a valid user in the dataset,
 * and every {@link OrderRecord#getProductId()} must correspond to a valid product.
 * This mirrors what a foreign-key constraint would enforce in a real database.</p>
 *
 * <p>Test groups: {@code data}, {@code unit}.</p>
 */
public class RelationalDataSetBuilderTest {

    private static final long SEED = 77777L;

    // -------------------------------------------------------------------------
    // Basic size assertions
    // -------------------------------------------------------------------------

    @Test(groups = {"data", "unit"},
          description = "build() generates exactly the configured number of users")
    public void buildGeneratesCorrectUserCount() {
        RelationalDataSet ds = RelationalDataSetBuilder.withSeed(SEED)
                .users(4)
                .products(6)
                .ordersPerUser(2)
                .build();
        assertThat(ds.getUsers()).hasSize(4);
    }

    @Test(groups = {"data", "unit"},
          description = "build() generates exactly the configured number of products")
    public void buildGeneratesCorrectProductCount() {
        RelationalDataSet ds = RelationalDataSetBuilder.withSeed(SEED)
                .users(3)
                .products(8)
                .ordersPerUser(1)
                .build();
        assertThat(ds.getProducts()).hasSize(8);
    }

    @Test(groups = {"data", "unit"},
          description = "build() generates user × ordersPerUser total orders")
    public void buildGeneratesCorrectOrderCount() {
        int users = 3;
        int ordersPerUser = 4;
        RelationalDataSet ds = RelationalDataSetBuilder.withSeed(SEED)
                .users(users)
                .products(5)
                .ordersPerUser(ordersPerUser)
                .build();
        assertThat(ds.getOrders()).hasSize(users * ordersPerUser);
    }

    // -------------------------------------------------------------------------
    // Referential integrity
    // -------------------------------------------------------------------------

    @Test(groups = {"data", "unit"},
          description = "Every order.userId exists in the user list (FK integrity)")
    public void everyOrderUserIdExistsInUserList() {
        RelationalDataSet ds = RelationalDataSetBuilder.withSeed(SEED)
                .users(5)
                .products(10)
                .ordersPerUser(3)
                .build();

        Set<String> userIds = ds.getUsers().stream()
                .map(User::getId)
                .collect(Collectors.toSet());

        for (OrderRecord order : ds.getOrders()) {
            assertThat(userIds)
                    .as("order %s has unknown userId=%s", order.getOrderId(), order.getUserId())
                    .contains(order.getUserId());
        }
    }

    @Test(groups = {"data", "unit"},
          description = "Every order.productId exists in the product list (FK integrity)")
    public void everyOrderProductIdExistsInProductList() {
        RelationalDataSet ds = RelationalDataSetBuilder.withSeed(SEED)
                .users(5)
                .products(10)
                .ordersPerUser(3)
                .build();

        Set<String> productIds = ds.getProducts().stream()
                .map(Product::getId)
                .collect(Collectors.toSet());

        for (OrderRecord order : ds.getOrders()) {
            assertThat(productIds)
                    .as("order %s has unknown productId=%s", order.getOrderId(), order.getProductId())
                    .contains(order.getProductId());
        }
    }

    // -------------------------------------------------------------------------
    // OrderRecord field completeness
    // -------------------------------------------------------------------------

    @Test(groups = {"data", "unit"},
          description = "Every OrderRecord has all required fields populated")
    public void orderRecordFieldsAreFullyPopulated() {
        RelationalDataSet ds = RelationalDataSetBuilder.withSeed(SEED)
                .users(2)
                .products(4)
                .ordersPerUser(2)
                .build();

        SoftAssertions softly = new SoftAssertions();
        for (OrderRecord o : ds.getOrders()) {
            softly.assertThat(o.getOrderId()).as("orderId").isNotBlank();
            softly.assertThat(o.getUserId()).as("userId").isNotBlank();
            softly.assertThat(o.getProductId()).as("productId").isNotBlank();
            softly.assertThat(o.getQuantity()).as("quantity").isGreaterThan(0);
            softly.assertThat(o.getUnitPrice()).as("unitPrice").isNotNull().isPositive();
            softly.assertThat(o.getTotalPrice()).as("totalPrice").isNotNull().isPositive();
            softly.assertThat(o.getStatus()).as("status").isNotBlank();
            softly.assertThat(o.getCreatedAt()).as("createdAt").isNotNull();
        }
        softly.assertAll();
    }

    @Test(groups = {"data", "unit"},
          description = "totalPrice = unitPrice × quantity for every order")
    public void totalPriceEqualsUnitPriceTimesQuantity() {
        RelationalDataSet ds = RelationalDataSetBuilder.withSeed(SEED)
                .users(3)
                .products(5)
                .ordersPerUser(3)
                .build();

        for (OrderRecord o : ds.getOrders()) {
            java.math.BigDecimal expected = o.getUnitPrice()
                    .multiply(java.math.BigDecimal.valueOf(o.getQuantity()))
                    .setScale(2, java.math.RoundingMode.HALF_UP);
            assertThat(o.getTotalPrice())
                    .as("totalPrice for order " + o.getOrderId())
                    .isEqualByComparingTo(expected);
        }
    }

    // -------------------------------------------------------------------------
    // Determinism
    // -------------------------------------------------------------------------

    @Test(groups = {"data", "unit"},
          description = "Same seed produces identical dataset on two consecutive builds")
    public void sameSeednProducesIdenticalDataset() {
        RelationalDataSet ds1 = RelationalDataSetBuilder.withSeed(SEED)
                .users(3).products(5).ordersPerUser(2).build();
        RelationalDataSet ds2 = RelationalDataSetBuilder.withSeed(SEED)
                .users(3).products(5).ordersPerUser(2).build();

        List<String> userIds1 = ds1.getUsers().stream().map(User::getId).toList();
        List<String> userIds2 = ds2.getUsers().stream().map(User::getId).toList();
        assertThat(userIds1).isEqualTo(userIds2);

        List<String> prodIds1 = ds1.getProducts().stream().map(Product::getId).toList();
        List<String> prodIds2 = ds2.getProducts().stream().map(Product::getId).toList();
        assertThat(prodIds1).isEqualTo(prodIds2);
    }

    // -------------------------------------------------------------------------
    // Guard-rail validation
    // -------------------------------------------------------------------------

    @Test(groups = {"data", "unit"},
          description = "users(0) throws DataException")
    public void usersZeroThrowsDataException() {
        assertThatThrownBy(() -> RelationalDataSetBuilder.withSeed(SEED).users(0))
                .isInstanceOf(DataException.class);
    }

    @Test(groups = {"data", "unit"},
          description = "products(0) throws DataException")
    public void productsZeroThrowsDataException() {
        assertThatThrownBy(() -> RelationalDataSetBuilder.withSeed(SEED).products(0))
                .isInstanceOf(DataException.class);
    }

    @Test(groups = {"data", "unit"},
          description = "ordersPerUser(0) generates zero orders without throwing")
    public void ordersPerUserZeroProducesZeroOrders() {
        RelationalDataSet ds = RelationalDataSetBuilder.withSeed(SEED)
                .users(3).products(4).ordersPerUser(0).build();
        assertThat(ds.getOrders()).isEmpty();
    }

    // -------------------------------------------------------------------------
    // toString / immutability
    // -------------------------------------------------------------------------

    @Test(groups = {"data", "unit"},
          description = "RelationalDataSet.toString() contains dimension information")
    public void toStringContainsDimensions() {
        RelationalDataSet ds = RelationalDataSetBuilder.withSeed(SEED)
                .users(2).products(3).ordersPerUser(1).build();
        String s = ds.toString();
        assertThat(s)
                .contains("users=2")
                .contains("products=3")
                .contains("orders=2");
    }

    @Test(groups = {"data", "unit"},
          description = "RelationalDataSet lists are unmodifiable")
    public void datasetListsAreUnmodifiable() {
        RelationalDataSet ds = RelationalDataSetBuilder.withSeed(SEED)
                .users(2).products(2).ordersPerUser(1).build();

        org.assertj.core.api.Assertions.assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> ds.getUsers().clear());
        org.assertj.core.api.Assertions.assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> ds.getProducts().clear());
        org.assertj.core.api.Assertions.assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> ds.getOrders().clear());
    }
}
