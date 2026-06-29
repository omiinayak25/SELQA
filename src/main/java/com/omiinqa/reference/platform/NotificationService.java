package com.omiinqa.reference.platform;

import com.omiinqa.reference.core.DomainException;
import com.omiinqa.reference.core.Validations;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * In-memory notification service with channel-based routing, per-user read
 * state, and mutable channel preferences. State is per-instance so each BDD
 * scenario receives an isolated, clean world.
 *
 * <h2>Delivery model</h2>
 * <p>When a caller enqueues a notification for a channel the target user has
 * muted, the notification is <em>silently dropped</em> (the method returns
 * {@code null}) rather than raising an error. This keeps the caller decoupled
 * from individual preference decisions.</p>
 *
 * <h2>Error codes</h2>
 * <ul>
 *   <li>{@code NOTIF_BLANK} — a required string field (userId, type, message)
 *       was null or blank.</li>
 *   <li>{@code NOTIF_BAD_CHANNEL} — the supplied {@link Channel} was
 *       {@code null}.</li>
 *   <li>{@code NOTIF_NOT_FOUND} — no notification exists with the requested
 *       id.</li>
 *   <li>{@code NOTIF_PREF_MUTED} — <em>documented for history only</em>; was
 *       used in an earlier version. Muting now silently drops the notification
 *       and this code is never thrown.</li>
 * </ul>
 */
public class NotificationService {

    // -------------------------------------------------------------------------
    // Supporting types
    // -------------------------------------------------------------------------

    /**
     * Delivery channel over which a notification travels.
     */
    public enum Channel {
        /** In-application notification centre. */
        IN_APP,
        /** Outbound email. */
        EMAIL,
        /** SMS text message. */
        SMS,
        /** Mobile push notification. */
        PUSH
    }

    /**
     * Immutable snapshot of a single notification. Mutability is limited to the
     * {@code read} flag so that {@link NotificationService#markRead} and
     * {@link NotificationService#markAllRead} can update state in-place without
     * replacing the stored object.
     */
    public static final class Notification {

        private final long id;
        private final String userId;
        private final Channel channel;
        private final String type;
        private final String message;
        private volatile boolean read;
        private final long createdAt;

        Notification(final long id, final String userId, final Channel channel,
                     final String type, final String message) {
            this.id = id;
            this.userId = userId;
            this.channel = channel;
            this.type = type;
            this.message = message;
            this.read = false;
            this.createdAt = System.currentTimeMillis();
        }

        /** Stable, auto-incremented identifier. */
        public long getId() { return id; }

        /** Owner of this notification. */
        public String getUserId() { return userId; }

        /** Channel on which the notification was delivered. */
        public Channel getChannel() { return channel; }

        /** Application-defined notification type, e.g. {@code ORDER_SHIPPED}. */
        public String getType() { return type; }

        /** Human-readable notification body. */
        public String getMessage() { return message; }

        /** {@code true} if the user has read this notification. */
        public boolean isRead() { return read; }

        /** Epoch milliseconds at which the notification was created. */
        public long getCreatedAt() { return createdAt; }

        void setRead(final boolean read) { this.read = read; }
    }

    /**
     * Per-user, per-channel delivery preference. When no preference record exists
     * for a given (userId, channel) pair the default is <em>enabled</em>.
     */
    public static final class Preference {

        private final String userId;
        private final Channel channel;
        private volatile boolean enabled;

        Preference(final String userId, final Channel channel, final boolean enabled) {
            this.userId = userId;
            this.channel = channel;
            this.enabled = enabled;
        }

        /** The user this preference belongs to. */
        public String getUserId() { return userId; }

        /** The channel this preference governs. */
        public Channel getChannel() { return channel; }

        /** {@code true} when delivery over {@link #getChannel()} is allowed. */
        public boolean isEnabled() { return enabled; }

        void setEnabled(final boolean enabled) { this.enabled = enabled; }
    }

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    /** All stored notifications, keyed by notification id. */
    private final ConcurrentHashMap<Long, Notification> store = new ConcurrentHashMap<>();

    /**
     * Preference registry, keyed by a compound {@code "userId|CHANNEL"} key for
     * O(1) look-ups.
     */
    private final ConcurrentHashMap<String, Preference> preferences = new ConcurrentHashMap<>();

    /** Monotonically increasing id generator; starts at 1. */
    private final AtomicLong ids = new AtomicLong(0);

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Enqueues a new notification for the given user and channel.
     *
     * <p>If the user has disabled the target channel the notification is silently
     * dropped and {@code null} is returned — no exception is raised. This allows
     * producers to fire-and-forget without knowing individual preferences.</p>
     *
     * @param userId  recipient; must not be blank ({@code NOTIF_BLANK})
     * @param channel delivery channel; must not be {@code null}
     *                ({@code NOTIF_BAD_CHANNEL})
     * @param type    application-defined notification category (e.g.
     *                {@code ORDER_SHIPPED}); must not be blank ({@code NOTIF_BLANK})
     * @param message human-readable body; must not be blank ({@code NOTIF_BLANK})
     * @return the stored {@link Notification}, or {@code null} if the channel is
     *         muted for this user
     * @throws DomainException {@code NOTIF_BLANK} when any required string is
     *                         blank; {@code NOTIF_BAD_CHANNEL} when channel is
     *                         {@code null}
     */
    public Notification enqueue(final String userId, final Channel channel,
                                final String type, final String message) {
        Validations.requireNotBlank(userId, "userId", "NOTIF_BLANK");
        if (channel == null) {
            throw new DomainException("NOTIF_BAD_CHANNEL", "channel must not be null");
        }
        Validations.requireNotBlank(type, "type", "NOTIF_BLANK");
        Validations.requireNotBlank(message, "message", "NOTIF_BLANK");

        // Silently drop when the user has muted this channel.
        if (!isChannelEnabled(userId, channel)) {
            return null;
        }

        final long id = ids.incrementAndGet();
        final Notification notif = new Notification(id, userId, channel, type, message);
        store.put(id, notif);
        return notif;
    }

    /**
     * Marks a single notification as read.
     *
     * @param notifId the id of the notification to mark
     * @throws DomainException {@code NOTIF_NOT_FOUND} when no notification
     *                         exists with the given id
     */
    public void markRead(final long notifId) {
        final Notification notif = store.get(notifId);
        if (notif == null) {
            throw new DomainException("NOTIF_NOT_FOUND",
                    "No notification with id: " + notifId);
        }
        notif.setRead(true);
    }

    /**
     * Marks all unread notifications belonging to {@code userId} as read.
     *
     * @param userId the target user; may be any string (no validation — unknown
     *               users simply yield a count of 0)
     * @return the number of notifications that were transitioned from unread to
     *         read by this call
     */
    public int markAllRead(final String userId) {
        int count = 0;
        for (final Notification notif : store.values()) {
            if (notif.getUserId().equals(userId) && !notif.isRead()) {
                notif.setRead(true);
                count++;
            }
        }
        return count;
    }

    /**
     * Returns the number of unread notifications for {@code userId}.
     *
     * @param userId the target user
     * @return unread count; 0 for unknown users
     */
    public int unreadCount(final String userId) {
        int count = 0;
        for (final Notification notif : store.values()) {
            if (notif.getUserId().equals(userId) && !notif.isRead()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Returns a filtered list of notifications for {@code userId}. All filter
     * parameters are optional: passing {@code null} means "do not filter on this
     * dimension".
     *
     * @param userId        the owner whose notifications are listed
     * @param readFilter    {@code Boolean.TRUE} → only read; {@code Boolean.FALSE}
     *                      → only unread; {@code null} → all
     * @param channelFilter restrict to a specific channel, or {@code null} for all
     * @param typeFilter    restrict to a specific type string, or {@code null} for
     *                      all
     * @return matching notifications in insertion order
     */
    public List<Notification> list(final String userId, final Boolean readFilter,
                                   final Channel channelFilter, final String typeFilter) {
        return store.values().stream()
                .filter(n -> n.getUserId().equals(userId))
                .filter(n -> readFilter == null || n.isRead() == readFilter)
                .filter(n -> channelFilter == null || n.getChannel() == channelFilter)
                .filter(n -> typeFilter == null || n.getType().equals(typeFilter))
                .sorted((a, b) -> Long.compare(a.getId(), b.getId()))
                .collect(Collectors.toList());
    }

    /**
     * Sets or updates a delivery preference for the given user and channel.
     *
     * @param userId  the user whose preference is being updated; must not be blank
     *                ({@code NOTIF_BLANK})
     * @param channel the channel to configure; must not be {@code null}
     *                ({@code NOTIF_BAD_CHANNEL})
     * @param enabled {@code true} to allow delivery, {@code false} to mute
     * @throws DomainException {@code NOTIF_BLANK} when userId is blank;
     *                         {@code NOTIF_BAD_CHANNEL} when channel is
     *                         {@code null}
     */
    public void setPreference(final String userId, final Channel channel,
                              final boolean enabled) {
        Validations.requireNotBlank(userId, "userId", "NOTIF_BLANK");
        if (channel == null) {
            throw new DomainException("NOTIF_BAD_CHANNEL", "channel must not be null");
        }
        final String key = prefKey(userId, channel);
        preferences.compute(key, (k, existing) -> {
            if (existing == null) {
                return new Preference(userId, channel, enabled);
            }
            existing.setEnabled(enabled);
            return existing;
        });
    }

    /**
     * Returns whether a channel is currently enabled for the given user.
     *
     * <p>If no explicit preference has been recorded, the default is
     * {@code true} (all channels enabled).</p>
     *
     * @param userId  the user to check
     * @param channel the channel to query
     * @return {@code true} if delivery is allowed; {@code false} if muted
     */
    public boolean isChannelEnabled(final String userId, final Channel channel) {
        final Preference pref = preferences.get(prefKey(userId, channel));
        return pref == null || pref.isEnabled();
    }

    /**
     * Returns the total number of notifications stored for {@code userId},
     * regardless of read status or channel.
     *
     * @param userId the target user
     * @return total notification count; 0 for unknown users
     */
    public int totalCount(final String userId) {
        int count = 0;
        for (final Notification notif : store.values()) {
            if (notif.getUserId().equals(userId)) {
                count++;
            }
        }
        return count;
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private static String prefKey(final String userId, final Channel channel) {
        return userId + "|" + channel.name();
    }
}
