package com.omiinqa.database.repositories;

import com.omiinqa.database.DatabaseType;
import com.omiinqa.database.RowMapper;
import com.omiinqa.database.model.AuditRecord;

import java.util.List;
import java.util.Optional;

/**
 * Repository for the {@code audit_log} table (Repository + Template Method patterns).
 *
 * <p><b>Design pattern — Repository:</b>
 * Encapsulates all SQL targeting the {@code audit_log} table behind a
 * domain-specific API. Test code that verifies compliance or security
 * requirements calls {@link #findByEntityAndAction} or {@link #findByActor}
 * rather than writing its own SQL. Benefits:</p>
 * <ol>
 *   <li>The audit schema can change (e.g. adding a {@code tenant_id} column)
 *       without touching every test that checks audit entries.</li>
 *   <li>Consistent parameterised-query discipline prevents SQL injection across
 *       all audit-related assertions.</li>
 *   <li>The API surface communicates audit concepts ({@code entity}, {@code
 *       action}, {@code actor}) rather than SQL mechanics.</li>
 * </ol>
 *
 * <p><b>Design pattern — Template Method:</b>
 * The algorithm skeleton — acquire connection, prepare statement, bind params,
 * iterate results, close resources — resides in {@link AbstractRepository}.
 * This subclass contributes only the SQL constants, the {@link RowMapper}
 * strategy, and the domain-specific finder and DML methods.</p>
 *
 * <p><b>Audit-log write contract:</b>
 * Application code is responsible for inserting audit entries; this repository
 * provides the {@link #insert} method for test scenarios that need to seed audit
 * data and for integration tests that verify the application writes correct
 * entries. All inserts are parameterised — never string-concatenated.</p>
 *
 * <p><b>SQL injection prevention:</b>
 * Every method binds values through {@link com.omiinqa.database.QueryExecutor}'s
 * {@link java.sql.PreparedStatement} path. No user-supplied value is ever
 * embedded in a SQL string by this class.</p>
 *
 * <p><b>Thread safety:</b> Stateless beyond the immutable {@code dbType} field
 * inherited from {@link AbstractRepository}. Safe for concurrent TestNG threads.</p>
 *
 * <p><b>Schema</b> (table: {@code audit_log}):</p>
 * <pre>
 *   id      BIGINT PRIMARY KEY AUTO_INCREMENT / SERIAL
 *   entity  VARCHAR(100) NOT NULL
 *   action  VARCHAR(50)  NOT NULL
 *   actor   VARCHAR(255) NOT NULL
 *   at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
 * </pre>
 *
 * <p><b>Usage example:</b></p>
 * <pre>{@code
 *   AuditRepository auditRepo = new AuditRepository(DatabaseType.POSTGRESQL);
 *   List<AuditRecord> deletes = auditRepo.findByAction("DELETE");
 *   assertThat(deletes).isNotEmpty();
 * }</pre>
 */
public class AuditRepository extends AbstractRepository<AuditRecord> {

    // ---------------------------------------------------------------- SQL constants

    private static final String SQL_FIND_BY_ID =
            "SELECT id, entity, action, actor, at FROM audit_log WHERE id = ?";

    private static final String SQL_FIND_ALL =
            "SELECT id, entity, action, actor, at FROM audit_log ORDER BY at DESC";

    private static final String SQL_FIND_BY_ENTITY =
            "SELECT id, entity, action, actor, at FROM audit_log"
            + " WHERE entity = ? ORDER BY at DESC";

    private static final String SQL_FIND_BY_ACTION =
            "SELECT id, entity, action, actor, at FROM audit_log"
            + " WHERE action = ? ORDER BY at DESC";

    private static final String SQL_FIND_BY_ACTOR =
            "SELECT id, entity, action, actor, at FROM audit_log"
            + " WHERE actor = ? ORDER BY at DESC";

    private static final String SQL_FIND_BY_ENTITY_AND_ACTION =
            "SELECT id, entity, action, actor, at FROM audit_log"
            + " WHERE entity = ? AND action = ? ORDER BY at DESC";

    private static final String SQL_FIND_LATEST_BY_ENTITY =
            "SELECT id, entity, action, actor, at FROM audit_log"
            + " WHERE entity = ? ORDER BY at DESC LIMIT 1";

    private static final String SQL_COUNT =
            "SELECT COUNT(*) FROM audit_log";

    private static final String SQL_COUNT_BY_ENTITY =
            "SELECT COUNT(*) FROM audit_log WHERE entity = ?";

    private static final String SQL_COUNT_BY_ACTOR =
            "SELECT COUNT(*) FROM audit_log WHERE actor = ?";

    private static final String SQL_COUNT_BY_ENTITY_AND_ACTION =
            "SELECT COUNT(*) FROM audit_log WHERE entity = ? AND action = ?";

    private static final String SQL_INSERT =
            "INSERT INTO audit_log (entity, action, actor) VALUES (?, ?, ?)";

    private static final String SQL_DELETE_BY_ID =
            "DELETE FROM audit_log WHERE id = ?";

    private static final String SQL_DELETE_BY_ACTOR =
            "DELETE FROM audit_log WHERE actor = ?";

    // ---------------------------------------------------------------- row mapper

    /**
     * Converts a single JDBC row into an {@link AuditRecord}.
     *
     * <p>Declared as a static field so it is allocated once and reused across
     * all queries in this repository — consistent with the pattern in
     * {@link UserRepository} and {@link ProductRepository}.</p>
     */
    private static final RowMapper<AuditRecord> AUDIT_MAPPER = (rs, rowNum) ->
            AuditRecord.builder()
                    .id(rs.getLong("id"))
                    .entity(rs.getString("entity"))
                    .action(rs.getString("action"))
                    .actor(rs.getString("actor"))
                    .at(rs.getObject("at"))
                    .build();

    // ---------------------------------------------------------------- constructor

    /**
     * Creates an {@code AuditRepository} targeting the specified database vendor.
     *
     * @param dbType the database vendor ({@link DatabaseType#POSTGRESQL} or
     *               {@link DatabaseType#MYSQL})
     */
    public AuditRepository(final DatabaseType dbType) {
        super(dbType);
    }

    // ---------------------------------------------------------------- query methods

    /**
     * Looks up a single audit entry by its surrogate primary key.
     *
     * @param id the database-generated primary key
     * @return an {@link Optional} containing the {@link AuditRecord}, or
     *         {@link Optional#empty()} if no row with that id exists
     */
    public Optional<AuditRecord> findById(final long id) {
        return one(SQL_FIND_BY_ID, AUDIT_MAPPER, id);
    }

    /**
     * Returns all audit entries ordered by timestamp descending (newest first).
     *
     * <p>Intended for small test data sets or post-test diagnostic dumps.
     * For production-size audit logs, prefer a filtered query with a date
     * range predicate.</p>
     *
     * @return immutable list of all audit log entries
     */
    public List<AuditRecord> findAll() {
        return list(SQL_FIND_ALL, AUDIT_MAPPER);
    }

    /**
     * Returns all audit entries for a given entity type, newest first.
     *
     * <p>Useful for asserting that all expected audit events were generated
     * for a particular domain object type during a test scenario.</p>
     *
     * @param entity the entity type string (e.g. {@code "order"}, {@code "user"})
     * @return matching audit records; empty list if none found
     */
    public List<AuditRecord> findByEntity(final String entity) {
        return list(SQL_FIND_BY_ENTITY, AUDIT_MAPPER, entity);
    }

    /**
     * Returns all audit entries for a specific action type, newest first.
     *
     * @param action the action string (e.g. {@code "CREATE"}, {@code "DELETE"})
     * @return matching audit records; empty list if none found
     */
    public List<AuditRecord> findByAction(final String action) {
        return list(SQL_FIND_BY_ACTION, AUDIT_MAPPER, action);
    }

    /**
     * Returns all audit entries attributed to a specific actor, newest first.
     *
     * <p>Useful for verifying that a particular user's actions were logged
     * correctly during a session-level integration test.</p>
     *
     * @param actor the actor identifier (email, username, or system id)
     * @return matching audit records; empty list if that actor has no entries
     */
    public List<AuditRecord> findByActor(final String actor) {
        return list(SQL_FIND_BY_ACTOR, AUDIT_MAPPER, actor);
    }

    /**
     * Returns all audit entries matching both entity type and action,
     * newest first.
     *
     * <p>The most targeted finder — typically used to assert that a specific
     * operation ({@code "DELETE"}) was logged for a specific domain object type
     * ({@code "order"}) after a test-driven workflow.</p>
     *
     * @param entity the entity type string
     * @param action the action string
     * @return matching audit records; empty list if none found
     */
    public List<AuditRecord> findByEntityAndAction(final String entity, final String action) {
        return list(SQL_FIND_BY_ENTITY_AND_ACTION, AUDIT_MAPPER, entity, action);
    }

    /**
     * Returns the most-recent audit entry for the given entity type, or empty
     * if no entries exist.
     *
     * <p>Convenience method for "assert the last recorded event for an entity
     * was X" patterns that are common in workflow integration tests.</p>
     *
     * @param entity the entity type string
     * @return optional most-recent {@link AuditRecord} for that entity
     */
    public Optional<AuditRecord> findLatestByEntity(final String entity) {
        return one(SQL_FIND_LATEST_BY_ENTITY, AUDIT_MAPPER, entity);
    }

    /**
     * Returns the total number of rows in the {@code audit_log} table.
     *
     * @return row count (0 if the table is empty)
     */
    public long count() {
        return scalar(SQL_COUNT)
                .map(v -> ((Number) v).longValue())
                .orElse(0L);
    }

    /**
     * Returns the number of audit entries for a specific entity type.
     *
     * @param entity the entity type string
     * @return count of entries for that entity
     */
    public long countByEntity(final String entity) {
        return scalar(SQL_COUNT_BY_ENTITY, entity)
                .map(v -> ((Number) v).longValue())
                .orElse(0L);
    }

    /**
     * Returns the number of audit entries attributed to a specific actor.
     *
     * @param actor the actor identifier
     * @return count of entries for that actor
     */
    public long countByActor(final String actor) {
        return scalar(SQL_COUNT_BY_ACTOR, actor)
                .map(v -> ((Number) v).longValue())
                .orElse(0L);
    }

    /**
     * Returns the number of audit entries matching both entity type and action.
     *
     * <p>Used in tests that count how many times a specific operation was
     * performed on a domain object type within a test scenario.</p>
     *
     * @param entity the entity type string
     * @param action the action string
     * @return count of matching entries
     */
    public long countByEntityAndAction(final String entity, final String action) {
        return scalar(SQL_COUNT_BY_ENTITY_AND_ACTION, entity, action)
                .map(v -> ((Number) v).longValue())
                .orElse(0L);
    }

    // ---------------------------------------------------------------- DML methods

    /**
     * Inserts a new audit log entry with the current database timestamp.
     *
     * <p>The {@code id} and {@code at} columns are database-generated. Callers
     * that need the generated id after insertion should query by actor + entity
     * + action using {@link #findByEntityAndAction} or directly via the
     * {@link com.omiinqa.database.QueryExecutor}.</p>
     *
     * <p><strong>SQL injection prevention:</strong> all three parameters are
     * bound through a {@code PreparedStatement} — never concatenated.</p>
     *
     * @param entity the entity type being audited
     * @param action the action performed
     * @param actor  the identity of the actor
     * @return number of rows inserted (expected: {@code 1})
     */
    public int insert(final String entity, final String action, final String actor) {
        return execute(SQL_INSERT, entity, action, actor);
    }

    /**
     * Deletes an audit entry by primary key.
     *
     * <p>Use only for test-data cleanup. In production, audit logs should be
     * append-only and never deleted (regulatory requirement).</p>
     *
     * @param id the primary key of the audit entry to delete
     * @return number of rows deleted ({@code 0} or {@code 1})
     */
    public int deleteById(final long id) {
        return execute(SQL_DELETE_BY_ID, id);
    }

    /**
     * Deletes all audit entries attributed to a specific actor.
     *
     * <p>Convenience cleanup method for tests that seed multiple audit entries
     * under a test actor identity and need to restore the table state.</p>
     *
     * @param actor the actor identifier whose entries should be removed
     * @return number of rows deleted
     */
    public int deleteByActor(final String actor) {
        return execute(SQL_DELETE_BY_ACTOR, actor);
    }
}
