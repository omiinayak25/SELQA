package com.omiinqa.bdd.steps.domain;

import com.omiinqa.bdd.support.DomainWorld;
import com.omiinqa.reference.platform.NotificationService;
import com.omiinqa.reference.platform.NotificationService.Channel;
import com.omiinqa.reference.platform.NotificationService.Notification;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for the reference platform / notification domain.
 *
 * <p>Drives the real {@link NotificationService} — no fakes, no browser. Outcomes
 * are recorded via {@link DomainWorld} so shared assertions from
 * {@code CommonDomainSteps} work unchanged. All step text is prefixed with
 * "notification" to avoid Cucumber ambiguity with other domain steps.</p>
 *
 * <p>Domain-specific behaviour asserted here:</p>
 * <ul>
 *   <li>Enqueue / unread count / total count</li>
 *   <li>markRead / markAllRead</li>
 *   <li>List filtering by channel, type, read status</li>
 *   <li>Per-user preference: muting a channel silently drops notifications
 *       (no error raised; total count remains unchanged)</li>
 *   <li>Validation: NOTIF_BLANK, NOTIF_BAD_CHANNEL, NOTIF_NOT_FOUND</li>
 * </ul>
 */
public class NotificationSteps {

    private static final String SVC = "notificationService";
    private static final String LAST_NOTIF = "notification.lastNotif";
    private static final String MARK_ALL_COUNT = "notification.markAllCount";

    private NotificationService service() {
        return DomainWorld.service(SVC, NotificationService::new);
    }

    // ─── Background ──────────────────────────────────────────────────────────

    @Given("a clean notification service")
    public void cleanNotificationService() {
        DomainWorld.put(SVC, new NotificationService());
    }

    // ─── When — enqueue ──────────────────────────────────────────────────────

    /**
     * Enqueues a notification; captures success or DomainException. The last
     * enqueued notification (may be null if muted) is stored for subsequent steps.
     */
    @When("I enqueue a notification for user {string} on channel {string} of type {string} with message {string}")
    public void enqueueNotification(final String userId,
                                    final String channelStr,
                                    final String type,
                                    final String message) {
        DomainWorld.run(() -> {
            final Channel channel = parseChannel(channelStr);
            final Notification notif = service().enqueue(userId, channel, type, message);
            DomainWorld.put(LAST_NOTIF, notif);
        });
    }

    // ─── When — markRead / markAllRead ───────────────────────────────────────

    @When("I mark the last notification as read for user {string}")
    public void markLastNotifRead(final String userId) {
        final Notification last = DomainWorld.get(LAST_NOTIF);
        assertThat(last)
                .as("expected a stored notification to mark as read, but none was found for user '%s'", userId)
                .isNotNull();
        DomainWorld.run(() -> service().markRead(last.getId()));
    }

    @When("I mark notification id {long} as read")
    public void markNotifById(final long id) {
        DomainWorld.run(() -> service().markRead(id));
    }

    @When("I mark all notifications as read for user {string}")
    public void markAllRead(final String userId) {
        DomainWorld.run(() -> {
            final int count = service().markAllRead(userId);
            DomainWorld.put(MARK_ALL_COUNT, count);
        });
    }

    // ─── When — preferences ──────────────────────────────────────────────────

    @When("I disable notification channel {string} for user {string}")
    public void disableChannel(final String channelStr, final String userId) {
        DomainWorld.run(() -> service().setPreference(userId, parseChannel(channelStr), false));
    }

    @When("I enable notification channel {string} for user {string}")
    public void enableChannel(final String channelStr, final String userId) {
        DomainWorld.run(() -> service().setPreference(userId, parseChannel(channelStr), true));
    }

    @When("I set notification preference for user {string} on channel {string} to enabled {string}")
    public void setPreference(final String userId, final String channelStr, final String enabledStr) {
        DomainWorld.run(() -> {
            final Channel channel = parseChannel(channelStr);
            service().setPreference(userId, channel, Boolean.parseBoolean(enabledStr));
        });
    }

    // ─── Then — counts ───────────────────────────────────────────────────────

    @Then("the notification unread count for user {string} is {int}")
    public void notifUnreadCount(final String userId, final int expected) {
        assertThat(service().unreadCount(userId))
                .as("unread notification count for user '%s'", userId)
                .isEqualTo(expected);
    }

    @Then("the notification total count for user {string} is {int}")
    public void notifTotalCount(final String userId, final int expected) {
        assertThat(service().totalCount(userId))
                .as("total notification count for user '%s'", userId)
                .isEqualTo(expected);
    }

    @Then("marking all notifications as read for user {string} returns count {int}")
    public void markAllReadReturnsCount(final String userId, final int expected) {
        final int actual = service().markAllRead(userId);
        assertThat(actual)
                .as("markAllRead return value for user '%s'", userId)
                .isEqualTo(expected);
    }

    // ─── Then — list filtering ────────────────────────────────────────────────

    @Then("the notification list for user {string} filtered by channel {string} has {int} item")
    public void notifListByChannelSingular(final String userId, final String channelStr, final int expected) {
        final List<Notification> items = service().list(userId, null, parseChannel(channelStr), null);
        assertThat(items)
                .as("notifications for user '%s' on channel '%s'", userId, channelStr)
                .hasSize(expected);
    }

    @Then("the notification list for user {string} filtered by channel {string} has {int} items")
    public void notifListByChannel(final String userId, final String channelStr, final int expected) {
        notifListByChannelSingular(userId, channelStr, expected);
    }

    @Then("the notification list for user {string} filtered by type {string} has {int} item")
    public void notifListByTypeSingular(final String userId, final String type, final int expected) {
        final List<Notification> items = service().list(userId, null, null, type);
        assertThat(items)
                .as("notifications for user '%s' of type '%s'", userId, type)
                .hasSize(expected);
    }

    @Then("the notification list for user {string} filtered by type {string} has {int} items")
    public void notifListByType(final String userId, final String type, final int expected) {
        notifListByTypeSingular(userId, type, expected);
    }

    @Then("the notification list for user {string} filtered by read {string} has {int} item")
    public void notifListByReadSingular(final String userId, final String readStr, final int expected) {
        final Boolean readFilter = Boolean.parseBoolean(readStr);
        final List<Notification> items = service().list(userId, readFilter, null, null);
        assertThat(items)
                .as("notifications for user '%s' with read=%s", userId, readStr)
                .hasSize(expected);
    }

    @Then("the notification list for user {string} filtered by read {string} has {int} items")
    public void notifListByRead(final String userId, final String readStr, final int expected) {
        notifListByReadSingular(userId, readStr, expected);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    /**
     * Parse a channel name from a Gherkin string. An unrecognised value is
     * deliberately returned as {@code null} so the service can raise
     * {@code NOTIF_BAD_CHANNEL} as required by the contract.
     */
    private Channel parseChannel(final String channelStr) {
        if (channelStr == null || channelStr.isBlank()) {
            return null;
        }
        try {
            return Channel.valueOf(channelStr.toUpperCase());
        } catch (final IllegalArgumentException e) {
            return null; // service will raise NOTIF_BAD_CHANNEL
        }
    }
}
