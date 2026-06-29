package com.omiinqa.data.synthetic;

import com.omiinqa.data.model.Product;
import com.omiinqa.data.model.User;
import com.omiinqa.exceptions.DataException;
import org.assertj.core.api.SoftAssertions;
import org.testng.annotations.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Offline unit tests for {@link SyntheticDataGenerator}.
 *
 * <p>All tests are fully offline — no network, no database, no browser.
 * Tests cover: seeded reproducibility, correct count, field completeness,
 * the generic supplier API, and guard-rail validation.</p>
 *
 * <p>Test groups: {@code data}, {@code unit}.</p>
 */
public class SyntheticDataGeneratorTest {

    private static final long SEED = 12345L;

    // -------------------------------------------------------------------------
    // generateUsers
    // -------------------------------------------------------------------------

    @Test(groups = {"data", "unit"},
          description = "generateUsers returns exactly the requested number of User objects")
    public void generateUsersReturnsCorrectCount() {
        SyntheticDataGenerator gen = new SyntheticDataGenerator(SEED);
        List<User> users = gen.generateUsers(7);
        assertThat(users).hasSize(7);
    }

    @Test(groups = {"data", "unit"},
          description = "generateUsers populates key fields on every user")
    public void generateUsersPopulatesKeyFields() {
        SyntheticDataGenerator gen = new SyntheticDataGenerator(SEED);
        List<User> users = gen.generateUsers(3);

        SoftAssertions softly = new SoftAssertions();
        for (User u : users) {
            softly.assertThat(u.getId()).as("id").isNotBlank();
            softly.assertThat(u.getFirstName()).as("firstName").isNotBlank();
            softly.assertThat(u.getLastName()).as("lastName").isNotBlank();
            softly.assertThat(u.getEmail()).as("email").isNotBlank();
            softly.assertThat(u.getUsername()).as("username").isNotBlank();
            softly.assertThat(u.getPassword()).as("password").isNotBlank();
            softly.assertThat(u.getPhone()).as("phone").isNotBlank();
            softly.assertThat(u.getAddress()).as("address").isNotNull();
        }
        softly.assertAll();
    }

    @Test(groups = {"data", "unit"},
          description = "generateUsers with the same seed produces identical results (seeded reproducibility)")
    public void generateUsersIsReproducibleWithSameSeed() {
        List<User> run1 = new SyntheticDataGenerator(SEED).generateUsers(5);
        List<User> run2 = new SyntheticDataGenerator(SEED).generateUsers(5);

        SoftAssertions softly = new SoftAssertions();
        for (int i = 0; i < run1.size(); i++) {
            softly.assertThat(run1.get(i).getId())
                  .as("id at index %d", i)
                  .isEqualTo(run2.get(i).getId());
            softly.assertThat(run1.get(i).getEmail())
                  .as("email at index %d", i)
                  .isEqualTo(run2.get(i).getEmail());
        }
        softly.assertAll();
    }

    @Test(groups = {"data", "unit"},
          description = "generateUsers with count=0 throws DataException")
    public void generateUsersThrowsForZeroCount() {
        assertThatThrownBy(() -> new SyntheticDataGenerator(SEED).generateUsers(0))
                .isInstanceOf(DataException.class)
                .hasMessageContaining("1");
    }

    // -------------------------------------------------------------------------
    // generateProducts
    // -------------------------------------------------------------------------

    @Test(groups = {"data", "unit"},
          description = "generateProducts returns exactly the requested number of Product objects")
    public void generateProductsReturnsCorrectCount() {
        SyntheticDataGenerator gen = new SyntheticDataGenerator(SEED);
        List<Product> products = gen.generateProducts(4);
        assertThat(products).hasSize(4);
    }

    @Test(groups = {"data", "unit"},
          description = "generateProducts populates key fields on every product")
    public void generateProductsPopulatesKeyFields() {
        SyntheticDataGenerator gen = new SyntheticDataGenerator(SEED);
        List<Product> products = gen.generateProducts(3);

        SoftAssertions softly = new SoftAssertions();
        for (Product p : products) {
            softly.assertThat(p.getId()).as("id").isNotBlank();
            softly.assertThat(p.getName()).as("name").isNotBlank();
            softly.assertThat(p.getPrice()).as("price").isNotNull().isPositive();
            softly.assertThat(p.getCategory()).as("category").isNotBlank();
            softly.assertThat(p.getDescription()).as("description").isNotBlank();
        }
        softly.assertAll();
    }

    @Test(groups = {"data", "unit"},
          description = "generateProducts with the same seed produces identical results (seeded reproducibility)")
    public void generateProductsIsReproducibleWithSameSeed() {
        List<Product> run1 = new SyntheticDataGenerator(SEED).generateProducts(3);
        List<Product> run2 = new SyntheticDataGenerator(SEED).generateProducts(3);

        for (int i = 0; i < run1.size(); i++) {
            assertThat(run1.get(i).getId())
                    .as("product id at index " + i)
                    .isEqualTo(run2.get(i).getId());
        }
    }

    // -------------------------------------------------------------------------
    // generic generate
    // -------------------------------------------------------------------------

    @Test(groups = {"data", "unit"},
          description = "generate(count, supplier) returns exactly count elements from the supplier")
    public void generateWithSupplierReturnsCorrectCount() {
        SyntheticDataGenerator gen = new SyntheticDataGenerator(SEED);
        List<String> result = gen.generate(6, () -> "item");
        assertThat(result).hasSize(6).containsOnly("item");
    }

    @Test(groups = {"data", "unit"},
          description = "generate with count=0 throws DataException")
    public void generateWithZeroCountThrows() {
        assertThatThrownBy(() -> new SyntheticDataGenerator(SEED).generate(0, () -> "x"))
                .isInstanceOf(DataException.class);
    }
}
