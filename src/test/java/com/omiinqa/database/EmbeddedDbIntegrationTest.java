package com.omiinqa.database;

import com.omiinqa.database.assertions.DatabaseAssertions;
import com.omiinqa.database.model.UserRecord;
import com.omiinqa.database.repositories.AuditRepository;
import com.omiinqa.database.repositories.OrderRepository;
import com.omiinqa.database.repositories.ProductRepository;
import com.omiinqa.database.repositories.UserRepository;
import com.omiinqa.database.support.EmbeddedH2;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * End-to-end integration test for the database layer against an embedded H2
 * database (PostgreSQL-compat). Unlike the {@code database} group (which needs a
 * live PostgreSQL/MySQL), this exercises the <b>real</b> stack —
 * {@link com.omiinqa.database.ConnectionManager} → HikariCP →
 * {@link QueryExecutor}/{@link TransactionManager}/repositories/
 * {@link DatabaseAssertions} — with zero external infrastructure, so it runs in
 * CI as a dependable gate.
 *
 * <p>The {@link DatabaseType#POSTGRESQL} handle is transparently redirected to
 * H2 by {@link EmbeddedH2}. Read-only tests assert the seeded baseline; mutating
 * tests clean up after themselves so the baseline stays stable (suite runs
 * sequentially).</p>
 */
@Epic("Database")
@Feature("Embedded integration (H2)")
public class EmbeddedDbIntegrationTest {

    private static final DatabaseType DB = DatabaseType.POSTGRESQL;

    private final UserRepository users = new UserRepository(DB);
    private final ProductRepository products = new ProductRepository(DB);
    private final OrderRepository orders = new OrderRepository(DB);
    private final AuditRepository audit = new AuditRepository(DB);

    @BeforeClass(alwaysRun = true)
    public void bootstrap() {
        EmbeddedH2.start();
    }

    // ----------------------------------------------------------- repositories

    @Test(groups = "db-embedded")
    public void userRepositorySeesSeededUsers() {
        assertThat(users.count()).isEqualTo(3);
        final Optional<UserRecord> alice = users.findByEmail("alice@omiinqa.test");
        assertThat(alice).isPresent();
        assertThat(alice.get().getStatus()).isEqualTo("active");
        assertThat(alice.get().getId()).isPositive();
    }

    @Test(groups = "db-embedded")
    public void userInsertFindDeleteRoundTrip() {
        final int inserted = users.insert("Temp User", "temp@omiinqa.test", "active");
        assertThat(inserted).isEqualTo(1);
        final Optional<UserRecord> found = users.findByEmail("temp@omiinqa.test");
        assertThat(found).isPresent();
        final int deleted = users.deleteById(found.get().getId());
        assertThat(deleted).isEqualTo(1);
        assertThat(users.findByEmail("temp@omiinqa.test")).isEmpty();
        assertThat(users.count()).isEqualTo(3); // baseline restored
    }

    @Test(groups = "db-embedded")
    public void productRepositoryFiltersByCategoryAndActive() {
        assertThat(products.findAll()).hasSize(4);
        assertThat(products.findByCategory("electronics")).hasSize(2);
        assertThat(products.findAllActive()).hasSize(3);
        assertThat(products.countByCategory("tools")).isEqualTo(1);
    }

    @Test(groups = "db-embedded")
    public void orderRepositoryAggregatesTotals() {
        assertThat(orders.count()).isEqualTo(3);
        assertThat(orders.findByStatus("paid")).hasSize(2);
        // user 1: 19.98 (paid) + 19.99 (pending) = 39.97
        assertThat(orders.sumTotalByUserId(1)).isEqualByComparingTo(new BigDecimal("39.97"));
    }

    @Test(groups = "db-embedded")
    public void auditRepositoryFiltersByAction() {
        assertThat(audit.count()).isEqualTo(3);
        assertThat(audit.findByAction("CREATE")).hasSize(2);
        assertThat(audit.findByActor("system")).hasSize(2);
    }

    // --------------------------------------------------------- query executor

    @Test(groups = "db-embedded")
    public void queryExecutorScalarAndList() {
        final Optional<Object> count = QueryExecutor.instance()
                .queryForScalar(DB, "SELECT COUNT(*) FROM products WHERE active = TRUE");
        assertThat(((Number) count.orElseThrow()).longValue()).isEqualTo(3);

        final List<Map<String, Object>> rows = QueryExecutor.instance()
                .queryForList(DB, "SELECT name, price FROM products WHERE category = ? ORDER BY price", "electronics");
        assertThat(rows).hasSize(2);
        assertThat(rows.get(0)).containsKey("name");
    }

    @Test(groups = "db-embedded")
    public void parameterizedQueryIsInjectionSafe() {
        // A SQL-injection payload passed as a bind parameter is treated as a
        // literal value, never executed — so it simply matches no rows.
        final List<Map<String, Object>> rows = QueryExecutor.instance()
                .queryForList(DB, "SELECT id FROM users WHERE email = ?", "' OR '1'='1");
        assertThat(rows).isEmpty();
        assertThat(users.count()).isEqualTo(3); // table intact, not dropped
    }

    @Test(groups = "db-embedded")
    public void aggregationGroupBy() {
        final List<Map<String, Object>> byStatus = QueryExecutor.instance()
                .queryForList(DB, "SELECT status, COUNT(*) AS cnt FROM orders GROUP BY status ORDER BY status");
        assertThat(byStatus).hasSize(2); // paid, pending
    }

    // --------------------------------------------------------- transactions

    @Test(groups = "db-embedded")
    public void transactionCommitPersists() {
        TransactionManager.instance().executeInTransaction(DB, conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO users (name, email, status) VALUES (?, ?, ?)")) {
                ps.setString(1, "Tx Commit");
                ps.setString(2, "txcommit@omiinqa.test");
                ps.setString(3, "active");
                ps.executeUpdate();
            } catch (final java.sql.SQLException e) {
                throw new RuntimeException(e);
            }
        });
        assertThat(users.findByEmail("txcommit@omiinqa.test")).isPresent();
        // cleanup to restore baseline
        users.findByEmail("txcommit@omiinqa.test").ifPresent(u -> users.deleteById(u.getId()));
        assertThat(users.count()).isEqualTo(3);
    }

    @Test(groups = "db-embedded")
    public void transactionRollbackDiscardsChanges() {
        assertThatThrownBy(() ->
                TransactionManager.instance().executeInTransaction(DB,
                        (java.util.function.Consumer<java.sql.Connection>) conn -> {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO users (name, email, status) VALUES (?, ?, ?)")) {
                        ps.setString(1, "Tx Rollback");
                        ps.setString(2, "txrollback@omiinqa.test");
                        ps.setString(3, "active");
                        ps.executeUpdate();
                    } catch (final java.sql.SQLException e) {
                        throw new RuntimeException(e);
                    }
                    throw new IllegalStateException("force rollback");
                })).isInstanceOf(RuntimeException.class);

        // The insert must have been rolled back.
        assertThat(users.findByEmail("txrollback@omiinqa.test")).isEmpty();
        assertThat(users.count()).isEqualTo(3);
    }

    // --------------------------------------------------------- assertions util

    @Test(groups = "db-embedded")
    public void databaseAssertionsAgainstSeed() {
        DatabaseAssertions.forDatabase(DB)
                .assertRowExists("SELECT 1 FROM users WHERE email = ?", "bob@omiinqa.test")
                .assertNoRowExists("SELECT 1 FROM users WHERE email = ?", "ghost@omiinqa.test")
                .assertRowCount("products", 4)
                .assertColumnValue("SELECT status FROM users WHERE email = ?", "inactive",
                        "carol@omiinqa.test");
    }
}
