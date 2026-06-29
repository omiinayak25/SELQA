package com.omiinqa.intelligence;

import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.List;

/**
 * Rule-based failure categorizer for test exceptions.
 *
 * <h3>Role</h3>
 * <p>When a test fails, the root cause often falls into a well-known category
 * (locator is wrong, the element disappeared, the network was slow, etc.).
 * Categorization enables smarter retry policies, better dashboards, and
 * automatic routing to the right team.</p>
 *
 * <h3>Purely rule-based, offline</h3>
 * <p>All classification is performed by matching exception types and message
 * patterns against a fixed, ordered rule set. No machine learning, no network
 * calls, no external service. Output is deterministic for identical inputs.</p>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * FailureCategory cat = FailureCategorizer.categorize(exception);
 * // → FailureCategory.LOCATOR, TIMEOUT, STALE_ELEMENT, …
 * }</pre>
 *
 * <h3>Thread safety</h3>
 * <p>All methods are stateless; safe for concurrent use.</p>
 */
public final class FailureCategorizer {

    private static final Logger log = LoggerFactory.getLogger(FailureCategorizer.class);

    private FailureCategorizer() {}

    /**
     * Classification categories for test failures.
     * The ordinal order reflects descending specificity — categories listed
     * earlier are more specific and take precedence in rule matching.
     */
    public enum FailureCategory {

        /** Element could not be found; locator is outdated or wrong. */
        LOCATOR,

        /** Operation exceeded the configured wait/timeout threshold. */
        TIMEOUT,

        /**
         * Assertion failed — the element was found but its state or value did
         * not match the expected condition.
         */
        ASSERTION,

        /**
         * Network error — connection refused, unreachable host, socket timeout,
         * HTTP 5xx from the driver or AUT.
         */
        NETWORK,

        /**
         * A previously located element has become detached from the DOM
         * ({@link StaleElementReferenceException}).
         */
        STALE_ELEMENT,

        /** WebDriver-level error: session lost, browser crashed, driver OOM. */
        DRIVER,

        /** Test data setup or data-provider failure. */
        DATA,

        /** Could not be classified by any known rule. */
        UNKNOWN
    }

    // -------------------------------------------------------------------------
    // Message-pattern lists
    // -------------------------------------------------------------------------

    private static final List<String> LOCATOR_PATTERNS = Arrays.asList(
            "no such element",
            "unable to locate element",
            "element not found",
            "cannot locate",
            "locator",
            "by\\.",
            "cssSelector",
            "xpath"
    );

    private static final List<String> TIMEOUT_PATTERNS = Arrays.asList(
            "timeout",
            "timed out",
            "wait.*exceeded",
            "expected condition",
            "fluent wait"
    );

    private static final List<String> ASSERTION_PATTERNS = Arrays.asList(
            "assertionerror",
            "expected:",
            "but was:",
            "to be equal",
            "to contain",
            "expected \\[",
            "junit",
            "testng.*assertion",
            "assertj"
    );

    private static final List<String> NETWORK_PATTERNS = Arrays.asList(
            "connection refused",
            "connection reset",
            "unreachable",
            "socket timeout",
            "network",
            "eof",
            "broken pipe",
            "http.*50[0-9]",
            "service unavailable",
            "bad gateway"
    );

    private static final List<String> STALE_PATTERNS = Arrays.asList(
            "stale element",
            "staleelementreference",
            "element is not attached",
            "element has been detached"
    );

    private static final List<String> DRIVER_PATTERNS = Arrays.asList(
            "webdriver",
            "session.*not.*created",
            "invalid session",
            "unknown error.*chrome",
            "out of memory",
            "browser.*crash",
            "driver",
            "chromedriver",
            "geckodriver"
    );

    private static final List<String> DATA_PATTERNS = Arrays.asList(
            "dataexception",
            "data provider",
            "dataprovider",
            "nullpointerexception.*data",
            "test data",
            "csv",
            "excel",
            "json.*parse",
            "illegal argument.*data"
    );

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Classifies a {@link Throwable} into a {@link FailureCategory}.
     *
     * <p>Classification proceeds as follows:
     * <ol>
     *   <li>Check the exact exception type (highest confidence).</li>
     *   <li>Walk the cause chain looking for known types.</li>
     *   <li>Match the message (and cause messages) against keyword patterns.</li>
     *   <li>Fall back to {@link FailureCategory#UNKNOWN}.</li>
     * </ol>
     *
     * @param throwable the exception to classify; may be {@code null}
     * @return the best-fit category; never {@code null}
     */
    public static FailureCategory categorize(final Throwable throwable) {
        if (throwable == null) {
            return FailureCategory.UNKNOWN;
        }

        // --- Type-based classification (exact match first) ---
        final FailureCategory byType = classifyByType(throwable);
        if (byType != FailureCategory.UNKNOWN) {
            log.debug("FailureCategorizer: [{}] classified as {} by type",
                    throwable.getClass().getSimpleName(), byType);
            return byType;
        }

        // --- Message-based classification ---
        final String fullMessage = collectMessages(throwable).toLowerCase();
        final FailureCategory byMsg = classifyByMessage(fullMessage);
        log.debug("FailureCategorizer: [{}] classified as {} by message pattern",
                throwable.getClass().getSimpleName(), byMsg);
        return byMsg;
    }

    /**
     * Classifies a raw exception message string (e.g. from a log or report)
     * without requiring an actual {@link Throwable}.
     *
     * @param message the exception message text; may be {@code null}
     * @return the best-fit category; never {@code null}
     */
    public static FailureCategory categorizeMessage(final String message) {
        if (message == null || message.isBlank()) {
            return FailureCategory.UNKNOWN;
        }
        return classifyByMessage(message.toLowerCase());
    }

    // -------------------------------------------------------------------------
    // Internals
    // -------------------------------------------------------------------------

    private static FailureCategory classifyByType(final Throwable t) {
        Throwable cursor = t;
        while (cursor != null) {
            if (cursor instanceof NoSuchElementException) {
                return FailureCategory.LOCATOR;
            }
            if (cursor instanceof StaleElementReferenceException) {
                return FailureCategory.STALE_ELEMENT;
            }
            if (cursor instanceof TimeoutException) {
                return FailureCategory.TIMEOUT;
            }
            if (cursor instanceof SocketTimeoutException) {
                return FailureCategory.NETWORK;
            }
            if (cursor instanceof ConnectException) {
                return FailureCategory.NETWORK;
            }
            if (cursor instanceof AssertionError) {
                return FailureCategory.ASSERTION;
            }
            // Check class name for types we cannot directly import
            final String simpleName = cursor.getClass().getSimpleName().toLowerCase();
            if (simpleName.contains("assertion")) {
                return FailureCategory.ASSERTION;
            }
            if (simpleName.contains("dataexception") || simpleName.contains("dataprovider")) {
                return FailureCategory.DATA;
            }
            if (simpleName.contains("webdriver") || simpleName.contains("driver")
                    || simpleName.contains("session")) {
                return FailureCategory.DRIVER;
            }
            cursor = cursor.getCause();
        }
        return FailureCategory.UNKNOWN;
    }

    private static FailureCategory classifyByMessage(final String lowerMessage) {
        // Check in priority order (most specific first)
        if (matchesAny(lowerMessage, STALE_PATTERNS))    return FailureCategory.STALE_ELEMENT;
        if (matchesAny(lowerMessage, LOCATOR_PATTERNS))  return FailureCategory.LOCATOR;
        if (matchesAny(lowerMessage, TIMEOUT_PATTERNS))  return FailureCategory.TIMEOUT;
        if (matchesAny(lowerMessage, ASSERTION_PATTERNS))return FailureCategory.ASSERTION;
        if (matchesAny(lowerMessage, NETWORK_PATTERNS))  return FailureCategory.NETWORK;
        if (matchesAny(lowerMessage, DRIVER_PATTERNS))   return FailureCategory.DRIVER;
        if (matchesAny(lowerMessage, DATA_PATTERNS))     return FailureCategory.DATA;
        return FailureCategory.UNKNOWN;
    }

    private static boolean matchesAny(final String message, final List<String> patterns) {
        for (final String pattern : patterns) {
            if (message.matches(".*" + pattern + ".*")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Collects the message from the exception and all its causes into a single
     * string, separated by " | ", so pattern matching covers the full chain.
     */
    private static String collectMessages(final Throwable t) {
        final StringBuilder sb = new StringBuilder();
        Throwable cursor = t;
        while (cursor != null) {
            sb.append(cursor.getClass().getName()).append(": ");
            if (cursor.getMessage() != null) {
                sb.append(cursor.getMessage());
            }
            sb.append(" | ");
            cursor = cursor.getCause();
        }
        return sb.toString();
    }
}
