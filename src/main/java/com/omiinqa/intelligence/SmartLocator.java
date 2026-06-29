package com.omiinqa.intelligence;

import com.omiinqa.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Self-healing element locator for the OmiinQA framework.
 *
 * <h3>Problem it solves</h3>
 * <p>A single locator failure — caused by a minor DOM change, a generated
 * attribute suffix, or a framework version update — should not break an
 * otherwise valid test. {@code SmartLocator} holds a <em>primary</em>
 * {@link By} and an ordered list of <em>fallback</em> {@code By} candidates.
 * At runtime it tries each candidate in order, logs a "healing" event the
 * first time a fallback wins, and caches the winning locator so subsequent
 * calls go directly to the healed locator without retrying.</p>
 *
 * <h3>Heuristic, NOT AI</h3>
 * <p>Self-healing is purely deterministic: the fallback list is supplied by
 * the caller (via the builder) or by a {@link LocatorHealingStrategy}. No
 * external service is contacted unless you explicitly provide an
 * AI-backed strategy.</p>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * SmartLocator locator = SmartLocator
 *     .primary(By.id("submit-btn"))
 *     .orElse(By.name("submit"))
 *     .orElse(By.cssSelector(".btn-primary[type='submit']"))
 *     .build();
 *
 * WebElement el = locator.resolve(driver);
 * }</pre>
 *
 * <h3>Thread safety</h3>
 * <p>The healing cache uses a {@link ConcurrentHashMap}. Multiple threads may
 * safely call {@link #resolve(WebDriver)} concurrently; in the worst case two
 * threads simultaneously discover the same winning fallback, which is harmless.</p>
 */
public final class SmartLocator {

    private static final Logger log = LoggerFactory.getLogger(SmartLocator.class);

    /**
     * Default probe timeout used in production when checking whether a locator
     * resolves. Short enough to fail fast on broken locators; long enough for
     * typical page-load synchronisation.
     */
    static final Duration DEFAULT_PROBE_TIMEOUT = Duration.ofSeconds(2);

    private final By primary;
    private final List<By> fallbacks;
    private final Duration probeTimeout;

    /**
     * Cache key → winning {@link By}. On healing, the winning locator is stored
     * here so future calls skip the trial sequence entirely.
     * Key = primary.toString() to identify this locator instance uniquely.
     */
    private final ConcurrentMap<String, By> healingCache = new ConcurrentHashMap<>();

    /** Production constructor: uses {@link #DEFAULT_PROBE_TIMEOUT}. */
    private SmartLocator(final By primary, final List<By> fallbacks) {
        this(primary, fallbacks, DEFAULT_PROBE_TIMEOUT);
    }

    /**
     * Full constructor — allows injection of a custom probe timeout.
     * Package-private so tests can set a short timeout without opening a browser.
     */
    SmartLocator(final By primary, final List<By> fallbacks, final Duration probeTimeout) {
        this.primary      = Objects.requireNonNull(primary, "primary By must not be null");
        this.fallbacks    = Collections.unmodifiableList(new ArrayList<>(fallbacks));
        this.probeTimeout = Objects.requireNonNull(probeTimeout, "probeTimeout must not be null");
    }

    // =========================================================================
    // Builder entry point
    // =========================================================================

    /**
     * Starts building a {@code SmartLocator} with the given primary locator.
     *
     * @param primary the first locator to try; must not be {@code null}
     * @return a builder for chaining fallback candidates
     */
    public static Builder primary(final By primary) {
        return new Builder(primary);
    }

    // =========================================================================
    // Core resolution logic
    // =========================================================================

    /**
     * Resolves this locator against the live DOM using the configured probe
     * timeout.
     *
     * <ol>
     *   <li>If a healed locator is cached, that is tried first (fast path).</li>
     *   <li>Otherwise the primary is tried.</li>
     *   <li>On primary failure, each fallback is tried in declaration order.</li>
     *   <li>On a fallback win, the winning {@link By} is cached and a healing
     *       event is logged at {@code WARN} level so engineers are notified.</li>
     * </ol>
     *
     * @param driver an active {@link WebDriver} session
     * @return the first found {@link WebElement}
     * @throws NoSuchElementException if all locators (primary + all fallbacks)
     *                                fail, with a message listing every tried
     *                                locator for easy diagnosis
     */
    public WebElement resolve(final WebDriver driver) {
        return resolve(driver, probeTimeout);
    }

    /**
     * Resolves this locator using an explicit probe timeout. Useful in tests
     * where a short timeout avoids multi-second waits per probe.
     *
     * @param driver       an active {@link WebDriver} session; must not be {@code null}
     * @param probeTimeout per-candidate wait duration; must not be {@code null}
     * @return the first found {@link WebElement}
     * @throws NoSuchElementException if all candidates fail
     */
    public WebElement resolve(final WebDriver driver, final Duration probeTimeout) {
        Objects.requireNonNull(driver, "driver must not be null");
        Objects.requireNonNull(probeTimeout, "probeTimeout must not be null");

        // Fast path — use previously healed locator
        final String cacheKey = primary.toString();
        final By cached = healingCache.get(cacheKey);
        if (cached != null) {
            final WebElement healed = probe(driver, cached, probeTimeout);
            if (healed != null) {
                return healed;
            }
            // Cached locator also broken — fall through to full resolution
            log.warn("SmartLocator: cached healed locator [{}] no longer works; re-healing", cached);
            healingCache.remove(cacheKey);
        }

        // Try primary
        final WebElement primaryEl = probe(driver, primary, probeTimeout);
        if (primaryEl != null) {
            return primaryEl;
        }

        // Try fallbacks in order
        final List<By> tried = new ArrayList<>();
        tried.add(primary);

        for (final By fallback : fallbacks) {
            final WebElement el = probe(driver, fallback, probeTimeout);
            if (el != null) {
                log.warn("SmartLocator: primary [{}] failed — healed with fallback [{}]. "
                        + "Consider updating the primary locator.", primary, fallback);
                healingCache.put(cacheKey, fallback);
                return el;
            }
            tried.add(fallback);
        }

        throw buildNotFoundError(tried);
    }

    /**
     * Returns the currently active (possibly healed) locator without resolving
     * against a live DOM. Useful for diagnostics and reporting.
     *
     * @return the healed locator if healing has occurred, otherwise the primary
     */
    public By activeLocator() {
        return healingCache.getOrDefault(primary.toString(), primary);
    }

    /**
     * Returns {@code true} if a healing event has occurred (i.e., the cached
     * winning locator is different from the primary).
     */
    public boolean isHealed() {
        final By cached = healingCache.get(primary.toString());
        return cached != null && !cached.equals(primary);
    }

    /**
     * Clears the healing cache, forcing the next {@link #resolve(WebDriver)}
     * call to run the full trial sequence again.
     */
    public void resetCache() {
        healingCache.remove(primary.toString());
    }

    /** Returns the primary locator (regardless of healing state). */
    public By getPrimary() {
        return primary;
    }

    /** Returns an unmodifiable view of the declared fallback locators. */
    public List<By> getFallbacks() {
        return fallbacks;
    }

    // =========================================================================
    // Internals
    // =========================================================================

    /**
     * Probes a single {@link By} with a short timeout. Returns the element on
     * success or {@code null} on failure — never throws.
     */
    private static WebElement probe(final WebDriver driver,
                                    final By locator,
                                    final Duration timeout) {
        try {
            return WaitUtils.until(driver,
                    d -> {
                        try {
                            return d.findElement(locator);
                        } catch (final NoSuchElementException e) {
                            return null;
                        }
                    },
                    timeout);
        } catch (final TimeoutException | NoSuchElementException e) {
            return null;
        }
    }

    private static NoSuchElementException buildNotFoundError(final List<By> tried) {
        final StringBuilder sb = new StringBuilder(
                "SmartLocator exhausted all candidates. Tried (in order):\n");
        for (int i = 0; i < tried.size(); i++) {
            sb.append("  [").append(i + 1).append("] ").append(tried.get(i)).append('\n');
        }
        sb.append("Fix: update the primary locator, add new fallbacks, "
                + "or enable a LocatorHealingStrategy.");
        return new NoSuchElementException(sb.toString());
    }

    // =========================================================================
    // Builder
    // =========================================================================

    /**
     * Fluent builder for {@link SmartLocator}.
     *
     * <pre>{@code
     * SmartLocator loc = SmartLocator
     *     .primary(By.id("login-btn"))
     *     .orElse(By.name("login"))
     *     .orElse(By.xpath("//button[text()='Login']"))
     *     .build();
     * }</pre>
     */
    public static final class Builder {

        private final By primary;
        private final List<By> fallbacks = new ArrayList<>();
        private Duration probeTimeout = DEFAULT_PROBE_TIMEOUT;

        private Builder(final By primary) {
            this.primary = Objects.requireNonNull(primary, "primary must not be null");
        }

        /**
         * Appends a fallback locator. Fallbacks are tried in the order they
         * are added.
         *
         * @param fallback a non-null alternative {@link By}
         * @return this builder (fluent)
         */
        public Builder orElse(final By fallback) {
            fallbacks.add(Objects.requireNonNull(fallback, "fallback must not be null"));
            return this;
        }

        /**
         * Overrides the per-candidate probe timeout. The default is
         * {@link SmartLocator#DEFAULT_PROBE_TIMEOUT} (2 seconds).
         *
         * <p>Set a very short timeout (e.g. 50 ms) in unit tests to avoid
         * multi-second waits when simulating failed locators with a fake driver.</p>
         *
         * @param timeout the probe duration; must not be {@code null}
         * @return this builder (fluent)
         */
        public Builder withProbeTimeout(final Duration timeout) {
            this.probeTimeout = Objects.requireNonNull(timeout, "timeout must not be null");
            return this;
        }

        /**
         * Builds the immutable {@link SmartLocator}.
         *
         * @return a new {@code SmartLocator} ready for use
         */
        public SmartLocator build() {
            return new SmartLocator(primary, fallbacks, probeTimeout);
        }
    }
}
