package com.omiinqa.database.repositories;

import com.omiinqa.database.DatabaseType;
import com.omiinqa.database.RowMapper;
import com.omiinqa.database.model.UserRecord;

import java.util.List;
import java.util.Optional;

/**
 * Repository for the {@code users} table (Repository + Active Record -lite patterns).
 *
 * <p><b>Design pattern — Repository:</b>
 * All SQL targeting the {@code users} table lives here. Test classes never
 * construct their own SQL strings for user data — they call intention-revealing
 * methods like {@link #findByEmail} or {@link #count}. This achieves two goals:
 * <ol>
 *   <li>SQL changes require touching only one file, not every test that uses
 *       the users table.</li>
 *   <li>Test readability improves: the assertion reads like a domain statement,
 *       not a JDBC tutorial.</li>
 * </ol>
 * </p>
 *
 * <p><b>SQL injection prevention:</b> All methods use {@code ?} placeholders
 * and delegate binding to {@link com.omiinqa.database.QueryExecutor}, which
 * uses {@link java.sql.PreparedStatement} exclusively. No method ever
 * concatenates user-supplied values into a SQL string.</p>
 *
 * <p><b>Schema</b> (table: {@code users}):</p>
 * <pre>
 *   id      BIGINT PRIMARY KEY AUTO_INCREMENT / SERIAL
 *   name    VARCHAR(255)
 *   email   VARCHAR(255) UNIQUE NOT NULL
 *   status  VARCHAR(50)   DEFAULT 'active'
 *   created TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
 * </pre>
 *
 * <p><b>Usage example:</b></p>
 * <pre>{@code
 *   UserRepository repo = new UserRepository(DatabaseType.POSTGRESQL);
 *   Optional<UserRecord> user = repo.findByEmail("alice@example.com");
 *   assertThat(user).isPresent();
 * }</pre>
 */
public class UserRepository extends AbstractRepository<UserRecord> {

    // ---------------------------------------------------------------- SQL constants

    private static final String SQL_FIND_BY_ID =
            "SELECT id, name, email, status, created FROM users WHERE id = ?";

    private static final String SQL_FIND_BY_EMAIL =
            "SELECT id, name, email, status, created FROM users WHERE email = ?";

    private static final String SQL_FIND_ALL =
            "SELECT id, name, email, status, created FROM users ORDER BY id";

    private static final String SQL_COUNT =
            "SELECT COUNT(*) FROM users";

    private static final String SQL_INSERT =
            "INSERT INTO users (name, email, status) VALUES (?, ?, ?)";

    private static final String SQL_DELETE_BY_ID =
            "DELETE FROM users WHERE id = ?";

    // ---------------------------------------------------------------- row mapper

    /**
     * Converts a single JDBC row into a {@link UserRecord}.
     *
     * <p>Defined as a field (not anonymous class) so it is allocated once and
     * reused across all queries in this repository — a minor but consistent
     * optimisation for high-frequency parallel test runs.</p>
     */
    private static final RowMapper<UserRecord> USER_MAPPER = (rs, rowNum) ->
            UserRecord.builder()
                    .id(rs.getLong("id"))
                    .name(rs.getString("name"))
                    .email(rs.getString("email"))
                    .status(rs.getString("status"))
                    .created(rs.getObject("created"))
                    .build();

    // ---------------------------------------------------------------- constructor

    /**
     * Creates a {@code UserRepository} targeting the specified database vendor.
     *
     * @param dbType the database vendor ({@link DatabaseType#POSTGRESQL} or
     *               {@link DatabaseType#MYSQL})
     */
    public UserRepository(final DatabaseType dbType) {
        super(dbType);
    }

    // ---------------------------------------------------------------- query methods

    /**
     * Looks up a user by primary key.
     *
     * @param id the surrogate primary key
     * @return an {@link Optional} containing the {@link UserRecord}, or empty if
     *         no row with that id exists
     */
    public Optional<UserRecord> findById(final long id) {
        return one(SQL_FIND_BY_ID, USER_MAPPER, id);
    }

    /**
     * Looks up a user by their unique email address.
     *
     * <p>Email is the business key for user records; this method is the most
     * common lookup in end-to-end tests that verify registration/login flows.</p>
     *
     * @param email the email address to search for (case-sensitive)
     * @return an {@link Optional} containing the matching {@link UserRecord},
     *         or empty if no row exists
     */
    public Optional<UserRecord> findByEmail(final String email) {
        return one(SQL_FIND_BY_EMAIL, USER_MAPPER, email);
    }

    /**
     * Returns all rows from the {@code users} table ordered by primary key.
     *
     * <p>Intended for small test data sets. Do not use in production against
     * a table with millions of rows — add a LIMIT if needed.</p>
     *
     * @return immutable list of all user records
     */
    public List<UserRecord> findAll() {
        return list(SQL_FIND_ALL, USER_MAPPER);
    }

    /**
     * Returns the total number of rows in the {@code users} table.
     *
     * <p>Useful for before/after count assertions in insert/delete tests:
     * {@code assertThat(repo.count()).isEqualTo(expectedCount)}.</p>
     *
     * @return the row count (0 if the table is empty)
     */
    public long count() {
        return scalar(SQL_COUNT)
                .map(v -> ((Number) v).longValue())
                .orElse(0L);
    }

    // ---------------------------------------------------------------- DML methods

    /**
     * Inserts a new user with the given name, email, and status.
     *
     * <p>The {@code id} and {@code created} columns are generated by the
     * database. Use {@link #findByEmail} after insertion to retrieve the
     * generated values if needed.</p>
     *
     * @param name   the user's display name
     * @param email  the unique email address (NOT NULL constraint)
     * @param status initial account status (e.g. {@code "active"})
     * @return number of rows inserted (expected: {@code 1})
     */
    public int insert(final String name, final String email, final String status) {
        return execute(SQL_INSERT, name, email, status);
    }

    /**
     * Deletes the user with the specified primary key.
     *
     * @param id the surrogate primary key of the row to delete
     * @return number of rows deleted ({@code 0} if no row with that id exists,
     *         {@code 1} if deleted successfully)
     */
    public int deleteById(final long id) {
        return execute(SQL_DELETE_BY_ID, id);
    }
}
