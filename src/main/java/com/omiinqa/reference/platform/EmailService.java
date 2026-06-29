package com.omiinqa.reference.platform;

import com.omiinqa.reference.core.DomainException;
import com.omiinqa.reference.core.Validations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * In-memory email-sending service that validates recipients, enforces subject
 * and body constraints, and supports bulk delivery with per-recipient failure
 * isolation. State is per-instance so each BDD scenario receives an isolated,
 * clean outbox.
 *
 * <h2>Error codes</h2>
 * <ul>
 *   <li>{@code EMAIL_BAD_RECIPIENT} — the {@code to} address failed RFC-style
 *       email validation ({@link com.omiinqa.reference.core.Validations#isValidEmail}).</li>
 *   <li>{@code EMAIL_BLANK_SUBJECT} — the subject was {@code null} or blank.</li>
 *   <li>{@code EMAIL_BODY_TOO_LONG} — the body exceeded
 *       {@link #MAX_BODY_LENGTH} characters.</li>
 * </ul>
 */
public class EmailService {

    // -------------------------------------------------------------------------
    // Supporting types
    // -------------------------------------------------------------------------

    /**
     * Immutable record of a successfully sent email.
     */
    public static final class Email {

        private final long id;
        private final String to;
        private final String subject;
        private final String body;
        private final long sentAt;

        Email(final long id, final String to, final String subject, final String body) {
            this.id = id;
            this.to = to;
            this.subject = subject;
            this.body = body;
            this.sentAt = System.currentTimeMillis();
        }

        /** Stable, auto-incremented email identifier. */
        public long getId() { return id; }

        /** Recipient address. */
        public String getTo() { return to; }

        /** Email subject line. */
        public String getSubject() { return subject; }

        /** Email body text. */
        public String getBody() { return body; }

        /** Epoch milliseconds at which the email was recorded as sent. */
        public long getSentAt() { return sentAt; }
    }

    /**
     * Aggregate result returned by {@link EmailService#bulkSend}. Failures are
     * collected rather than aborting the bulk operation so that all valid
     * recipients receive their email regardless of per-entry errors.
     */
    public static final class BulkResult {

        private int successCount;
        private int failureCount;
        private final List<String> errors;

        BulkResult() {
            this.successCount = 0;
            this.failureCount = 0;
            this.errors = new ArrayList<>();
        }

        /** Number of emails that were sent successfully. */
        public int getSuccessCount() { return successCount; }

        /** Number of recipients for which sending failed. */
        public int getFailureCount() { return failureCount; }

        /**
         * Human-readable error messages, one per failed recipient (in encounter
         * order).
         */
        public List<String> getErrors() { return Collections.unmodifiableList(errors); }

        void recordSuccess() { successCount++; }

        void recordFailure(final String error) {
            failureCount++;
            errors.add(error);
        }
    }

    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------

    /**
     * Maximum number of characters allowed in an email body. Attempts to send a
     * longer body raise {@code EMAIL_BODY_TOO_LONG}.
     */
    public static final int MAX_BODY_LENGTH = 5000;

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    /**
     * In-memory outbox; thread-safe for concurrent scenario support. Entries are
     * appended on every successful {@link #send} and cleared by {@link #clear}.
     */
    private final CopyOnWriteArrayList<Email> outbox = new CopyOnWriteArrayList<>();

    /** Monotonically increasing id generator; starts at 1. */
    private final AtomicLong ids = new AtomicLong(0);

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Validates and records an outbound email.
     *
     * <p>Validation is applied in the following order:</p>
     * <ol>
     *   <li>Recipient format — {@code EMAIL_BAD_RECIPIENT}</li>
     *   <li>Subject blank — {@code EMAIL_BLANK_SUBJECT}</li>
     *   <li>Body length — {@code EMAIL_BODY_TOO_LONG}</li>
     * </ol>
     *
     * @param to      recipient email address; must be a valid RFC-style address
     * @param subject email subject; must not be blank
     * @param body    email body; must not exceed {@link #MAX_BODY_LENGTH} chars
     * @return the recorded {@link Email} stored in the outbox
     * @throws DomainException {@code EMAIL_BAD_RECIPIENT} when {@code to} is not
     *                         a valid email address; {@code EMAIL_BLANK_SUBJECT}
     *                         when subject is blank; {@code EMAIL_BODY_TOO_LONG}
     *                         when body exceeds the maximum length
     */
    public Email send(final String to, final String subject, final String body) {
        if (!Validations.isValidEmail(to)) {
            throw new DomainException("EMAIL_BAD_RECIPIENT",
                    "Invalid recipient email address: " + to);
        }
        Validations.requireNotBlank(subject, "subject", "EMAIL_BLANK_SUBJECT");
        if (body != null && body.length() > MAX_BODY_LENGTH) {
            throw new DomainException("EMAIL_BODY_TOO_LONG",
                    "Email body exceeds maximum length of " + MAX_BODY_LENGTH
                            + " characters (was " + body.length() + ")");
        }

        final Email email = new Email(ids.incrementAndGet(), to, subject,
                body == null ? "" : body);
        outbox.add(email);
        return email;
    }

    /**
     * Returns the current number of emails in the outbox.
     *
     * @return non-negative outbox size
     */
    public int outboxSize() {
        return outbox.size();
    }

    /**
     * Returns the recipient address of the most recently sent email.
     *
     * @return the {@code to} address of the last email, or {@code null} if the
     *         outbox is empty
     */
    public String lastTo() {
        if (outbox.isEmpty()) {
            return null;
        }
        return outbox.get(outbox.size() - 1).getTo();
    }

    /**
     * Returns all emails sent to the given recipient address.
     *
     * <p>Comparison is case-sensitive and matches exactly the address supplied to
     * {@link #send}.</p>
     *
     * @param to recipient address to search for
     * @return list of matching emails in send order; never {@code null}
     */
    public List<Email> findByRecipient(final String to) {
        return outbox.stream()
                .filter(e -> e.getTo().equals(to))
                .collect(Collectors.toList());
    }

    /**
     * Sends the same subject and body to multiple recipients, collecting
     * per-recipient successes and failures without aborting on the first error.
     *
     * <p>Each recipient is processed independently through {@link #send}. A
     * {@link DomainException} on any individual attempt increments the failure
     * counter and records the error message; all other recipients continue to be
     * processed.</p>
     *
     * @param recipients list of recipient addresses; {@code null} entries are
     *                   processed as invalid addresses and counted as failures
     * @param subject    subject applied to every email in the batch
     * @param body       body applied to every email in the batch
     * @return a {@link BulkResult} summarising successes, failures and error
     *         messages
     */
    public BulkResult bulkSend(final List<String> recipients, final String subject,
                               final String body) {
        final BulkResult result = new BulkResult();
        if (recipients == null) {
            return result;
        }
        for (final String recipient : recipients) {
            try {
                send(recipient, subject, body);
                result.recordSuccess();
            } catch (final DomainException ex) {
                result.recordFailure(ex.getMessage());
            }
        }
        return result;
    }

    /**
     * Clears all emails from the outbox. Useful for resetting state within a
     * scenario step without constructing a new service instance.
     */
    public void clear() {
        outbox.clear();
    }
}
