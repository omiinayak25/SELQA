package com.omiinqa.reference.platform;

import com.omiinqa.reference.core.DomainException;
import com.omiinqa.reference.core.Validations;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Append-only, in-memory audit log service for the reference platform domain.
 *
 * <p>Records immutable {@link AuditEntry} items capturing who ({@code actor})
 * did what ({@code action}) to which resource ({@code entity} / {@code entityId})
 * and when ({@code timestamp}). All write operations are append-only — deletion
 * is explicitly forbidden and always raises {@code AUDIT_IMMUTABLE}.</p>
 *
 * <p>State is per-instance so each BDD scenario receives an isolated, clean
 * audit log. Internally backed by a {@link CopyOnWriteArrayList} for
 * thread-safety without external locking on reads.</p>
 *
 * <h2>Error codes</h2>
 * <ul>
 *   <li>{@code AUDIT_BLANK} — any required parameter ({@code actor},
 *       {@code action}, {@code entity}, {@code entityId}) was {@code null} or
 *       blank.</li>
 *   <li>{@code AUDIT_IMMUTABLE} — a deletion was attempted; the audit log is
 *       append-only and records may never be removed.</li>
 * </ul>
 */
public class AuditService {

    // -------------------------------------------------------------------------
    // Supporting types
    // -------------------------------------------------------------------------

    /**
     * Immutable record of a single audited event.
     *
     * <p>Instances are created exclusively by {@link AuditService#record} and
     * carry an auto-incremented {@code id}, the four mandatory context fields,
     * and the {@link Instant} at which the event was recorded.</p>
     */
    public static final class AuditEntry {

        private final long id;
        private final String actor;
        private final String action;
        private final String entity;
        private final String entityId;
        private final Instant timestamp;

        AuditEntry(final long id, final String actor, final String action,
                   final String entity, final String entityId, final Instant timestamp) {
            this.id = id;
            this.actor = actor;
            this.action = action;
            this.entity = entity;
            this.entityId = entityId;
            this.timestamp = timestamp;
        }

        /** Stable, monotonically increasing identifier assigned on creation. */
        public long getId() { return id; }

        /** Identity of the principal that performed the action (e.g. username). */
        public String getActor() { return actor; }

        /** Business verb describing the operation performed (e.g. {@code LOGIN}). */
        public String getAction() { return action; }

        /** Logical resource type that was acted upon (e.g. {@code Order}). */
        public String getEntity() { return entity; }

        /** Identifier of the specific resource instance. */
        public String getEntityId() { return entityId; }

        /** Moment at which the event was recorded; never {@code null}. */
        public Instant getTimestamp() { return timestamp; }
    }

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    /** Append-only store; thread-safe for concurrent reads and writes. */
    private final CopyOnWriteArrayList<AuditEntry> log = new CopyOnWriteArrayList<>();

    /** Monotonically increasing id generator; starts at 1 for the first entry. */
    private final AtomicLong ids = new AtomicLong(0);

    // -------------------------------------------------------------------------
    // Public API — write
    // -------------------------------------------------------------------------

    /**
     * Records a new audit event.
     *
     * <p>All four parameters are mandatory and must be non-{@code null},
     * non-blank strings. Validation failures raise {@code AUDIT_BLANK}.</p>
     *
     * @param actor    identity of the principal performing the action
     * @param action   business verb describing the operation
     * @param entity   logical resource type being acted upon
     * @param entityId identifier of the specific resource instance
     * @throws DomainException {@code AUDIT_BLANK} when any parameter is blank
     */
    public void record(final String actor, final String action,
                       final String entity, final String entityId) {
        Validations.requireNotBlank(actor, "actor", "AUDIT_BLANK");
        Validations.requireNotBlank(action, "action", "AUDIT_BLANK");
        Validations.requireNotBlank(entity, "entity", "AUDIT_BLANK");
        Validations.requireNotBlank(entityId, "entityId", "AUDIT_BLANK");

        log.add(new AuditEntry(ids.incrementAndGet(), actor, action, entity, entityId,
                Instant.now()));
    }

    // -------------------------------------------------------------------------
    // Public API — deletion (always forbidden)
    // -------------------------------------------------------------------------

    /**
     * Deletion is not supported — the audit log is strictly append-only.
     *
     * <p>This method always throws {@code AUDIT_IMMUTABLE} regardless of the
     * arguments supplied. It exists to provide a strongly-typed, documentable
     * rejection point that BDD scenarios can assert against.</p>
     *
     * @param id the entry id the caller attempted to delete (ignored)
     * @throws DomainException {@code AUDIT_IMMUTABLE} unconditionally
     */
    public void delete(final long id) {
        throw new DomainException("AUDIT_IMMUTABLE",
                "Audit log is append-only; entry " + id + " cannot be deleted");
    }

    // -------------------------------------------------------------------------
    // Public API — query
    // -------------------------------------------------------------------------

    /**
     * Returns all audit entries recorded by the specified actor, newest first.
     *
     * @param actor identity to filter on; comparison is case-sensitive
     * @return list of matching entries ordered by descending timestamp; never {@code null}
     */
    public List<AuditEntry> queryByActor(final String actor) {
        return log.stream()
                .filter(e -> e.getActor().equals(actor))
                .sorted(Comparator.comparing(AuditEntry::getTimestamp).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Returns all audit entries for the specified action verb, newest first.
     *
     * @param action the business verb to filter on; comparison is case-sensitive
     * @return list of matching entries ordered by descending timestamp; never {@code null}
     */
    public List<AuditEntry> queryByAction(final String action) {
        return log.stream()
                .filter(e -> e.getAction().equals(action))
                .sorted(Comparator.comparing(AuditEntry::getTimestamp).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Returns all audit entries for the specified entity type, newest first.
     *
     * @param entity the logical resource type to filter on; comparison is case-sensitive
     * @return list of matching entries ordered by descending timestamp; never {@code null}
     */
    public List<AuditEntry> queryByEntity(final String entity) {
        return log.stream()
                .filter(e -> e.getEntity().equals(entity))
                .sorted(Comparator.comparing(AuditEntry::getTimestamp).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Returns all audit entries whose timestamp falls within the given inclusive
     * time range, ordered newest first.
     *
     * @param from lower bound of the range (inclusive)
     * @param to   upper bound of the range (inclusive)
     * @return list of matching entries ordered by descending timestamp; never {@code null}
     * @throws IllegalArgumentException when {@code from} or {@code to} is {@code null}
     */
    public List<AuditEntry> queryByTimeRange(final Instant from, final Instant to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Time-range bounds must not be null");
        }
        return log.stream()
                .filter(e -> !e.getTimestamp().isBefore(from) && !e.getTimestamp().isAfter(to))
                .sorted(Comparator.comparing(AuditEntry::getTimestamp).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Returns the total number of audit entries recorded since this instance was
     * created.
     *
     * @return non-negative record count
     */
    public long count() {
        return log.size();
    }

    /**
     * Returns the most recently recorded audit entry for the given entity type.
     *
     * <p>When multiple entries share an identical timestamp the one with the
     * highest {@code id} (most recently appended) is preferred.</p>
     *
     * @param entity the logical resource type to search; comparison is case-sensitive
     * @return an {@link Optional} containing the latest entry, or empty if none exist
     */
    public Optional<AuditEntry> latestFor(final String entity) {
        return log.stream()
                .filter(e -> e.getEntity().equals(entity))
                .max(Comparator.comparing(AuditEntry::getTimestamp)
                        .thenComparingLong(AuditEntry::getId));
    }
}
