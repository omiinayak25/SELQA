package com.omiinqa.database;

import com.omiinqa.exceptions.DatabaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Function;

/**
 * Lightweight JDBC transaction coordinator (Template Method + Command patterns).
 *
 * <p><b>Design pattern — Template Method:</b>
 * {@link #executeInTransaction} provides a fixed algorithm skeleton:
 * <ol>
 *   <li>Obtain a connection from the pool.</li>
 *   <li>Disable auto-commit to start an explicit transaction.</li>
 *   <li>Invoke the caller-supplied unit-of-work ({@link Function}).</li>
 *   <li>Commit on success, or rollback and rethrow on any exception.</li>
 *   <li>Restore auto-commit and return the connection to the pool.</li>
 * </ol>
 * Each step in this skeleton is invariant; the "do the actual work" step is
 * the hook point that callers fill with a lambda — the Command pattern.</p>
 *
 * <p><b>Design pattern — Command:</b> The {@code Function<Connection, T>}
 * parameter is a Command object. Callers encapsulate a unit of transactional
 * work as a lambda and hand it to the manager. The manager executes it inside
 * the transaction boundary without needing to know what the work does.</p>
 *
 * <p><b>Design pattern — Singleton:</b> The manager is stateless; a single
 * instance is sufficient and safe for parallel test threads since each
 * invocation borrows its own {@link Connection}.</p>
 *
 * <p><b>Rollback guarantee:</b> If the unit-of-work lambda throws any
 * {@link RuntimeException} or {@link SQLException}, the transaction is rolled
 * back before the exception propagates. Connection state (auto-commit) is
 * always restored in the {@code finally} block to prevent pool contamination.</p>
 *
 * <p><b>Usage example:</b></p>
 * <pre>{@code
 *   TransactionManager tx = TransactionManager.instance();
 *   int rowsAffected = tx.executeInTransaction(DatabaseType.POSTGRESQL, conn -> {
 *       QueryExecutor exec = QueryExecutor.instance();
 *       exec.update(DatabaseType.POSTGRESQL, "INSERT INTO orders ...", ...);
 *       exec.update(DatabaseType.POSTGRESQL, "UPDATE inventory ...", ...);
 *       return 2; // both DML statements
 *   });
 * }</pre>
 *
 * <p><b>Nested transactions:</b> JDBC does not support true nested transactions.
 * Callers must not call {@code executeInTransaction} recursively with the same
 * {@link Connection}. If nested transactional scopes are required, use
 * savepoints manually via the {@code Connection} parameter inside the lambda.</p>
 */
public final class TransactionManager {

    private static final Logger LOG = LoggerFactory.getLogger(TransactionManager.class);

    /** Stateless singleton — safe for concurrent use. */
    private static final TransactionManager INSTANCE = new TransactionManager();

    private TransactionManager() { }

    /**
     * Returns the shared {@code TransactionManager} instance.
     *
     * @return the singleton manager
     */
    public static TransactionManager instance() {
        return INSTANCE;
    }

    // ---------------------------------------------------------------- public API

    /**
     * Executes the supplied unit-of-work inside a single JDBC transaction,
     * committing on success and rolling back on any failure.
     *
     * <p><b>Template steps:</b></p>
     * <ol>
     *   <li>Acquire a {@link Connection} from the {@link ConnectionManager} pool.</li>
     *   <li>Disable auto-commit ({@code setAutoCommit(false)}).</li>
     *   <li>Invoke {@code work.apply(connection)}.</li>
     *   <li>On success: {@code commit()}, restore auto-commit, return result.</li>
     *   <li>On exception: {@code rollback()}, restore auto-commit, rethrow as
     *       {@link DatabaseException}.</li>
     *   <li>In all cases: return the connection to the pool via {@code close()}.</li>
     * </ol>
     *
     * <p><b>Important:</b> The {@link Connection} passed to {@code work} must
     * only be used within the lambda. It must not be stored and accessed after
     * the lambda returns.</p>
     *
     * @param <T>  the type returned by the unit-of-work
     * @param type the database vendor whose pool supplies the connection
     * @param work a lambda or method reference that performs transactional work
     *             using the provided {@link Connection}; should return a result
     *             of type {@code T} (use {@code Void} / return null if no result
     *             is needed)
     * @return the value returned by {@code work}
     * @throws DatabaseException if any step of the transaction (including the
     *                           work itself) fails; rollback is guaranteed before
     *                           this exception propagates
     */
    public <T> T executeInTransaction(
            final DatabaseType type,
            final Function<Connection, T> work) {
        LOG.debug("TransactionManager: beginning transaction on {}", type);
        Connection conn = null;
        boolean committed = false;
        try {
            conn = ConnectionManager.get().getConnection(type);
            conn.setAutoCommit(false);

            final T result = work.apply(conn);

            conn.commit();
            committed = true;
            LOG.debug("TransactionManager: committed transaction on {}", type);
            return result;

        } catch (final DatabaseException de) {
            rollbackQuietly(conn, type);
            throw de;
        } catch (final SQLException e) {
            rollbackQuietly(conn, type);
            throw new DatabaseException(
                    "Transaction failed on " + type + ": " + e.getMessage(), e);
        } catch (final RuntimeException e) {
            rollbackQuietly(conn, type);
            throw new DatabaseException(
                    "Transaction rolled back due to runtime exception on " + type
                    + ": " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                restoreAutoCommit(conn, committed, type);
                closeQuietly(conn);
            }
        }
    }

    /**
     * Executes the supplied unit-of-work in a transaction and discards the
     * return value. Convenient wrapper for void operations.
     *
     * @param type the database vendor
     * @param work a transactional unit of work (return value ignored)
     * @throws DatabaseException if the transaction fails
     */
    public void executeInTransaction(
            final DatabaseType type,
            final java.util.function.Consumer<Connection> work) {
        executeInTransaction(type, conn -> {
            work.accept(conn);
            return null;
        });
    }

    // ---------------------------------------------------------------- internals

    /** Attempts a rollback; logs a warning but does not rethrow if rollback fails. */
    private void rollbackQuietly(final Connection conn, final DatabaseType type) {
        if (conn == null) return;
        try {
            conn.rollback();
            LOG.info("TransactionManager: rolled back transaction on {}", type);
        } catch (final SQLException e) {
            LOG.warn("TransactionManager: rollback failed on {}: {}", type, e.getMessage());
        }
    }

    /**
     * Restores auto-commit to {@code true} so the connection is in a clean
     * state when returned to the HikariCP pool (pool connections are recycled,
     * not destroyed, so state must be explicitly reset).
     */
    private void restoreAutoCommit(
            final Connection conn,
            final boolean wasCommitted,
            final DatabaseType type) {
        try {
            conn.setAutoCommit(true);
        } catch (final SQLException e) {
            LOG.warn("TransactionManager: could not restore auto-commit on {} (committed={}): {}",
                    type, wasCommitted, e.getMessage());
        }
    }

    /** Closes a connection without propagating any exception (safe in finally blocks). */
    private void closeQuietly(final Connection conn) {
        try {
            conn.close(); // HikariCP proxy — returns to pool
        } catch (final SQLException e) {
            LOG.warn("TransactionManager: failed to return connection to pool: {}", e.getMessage());
        }
    }
}
