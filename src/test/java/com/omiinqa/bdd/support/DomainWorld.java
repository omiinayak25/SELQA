package com.omiinqa.bdd.support;

import com.omiinqa.bdd.context.ScenarioContext;
import com.omiinqa.reference.core.DomainException;

import java.util.function.Supplier;

/**
 * Shared per-scenario "world" for reference-domain BDD steps.
 *
 * <p>Provides a single, consistent convention for (a) lazily creating and
 * sharing domain services per scenario and (b) capturing the outcome of the
 * last domain operation (success value or {@link DomainException}). Because all
 * domains funnel outcomes through here, the generic assertions in
 * {@code CommonDomainSteps} ("a domain error X is raised", "the operation
 * succeeds") are defined exactly once — eliminating duplicate/ambiguous step
 * definitions across the many domain step classes.</p>
 */
public final class DomainWorld {

    private static final String ERROR = "domain.lastError";
    private static final String RESULT = "domain.lastResult";

    private DomainWorld() {
    }

    /** Lazily create-and-share a service instance under {@code key}. */
    public static <T> T service(final String key, final Supplier<T> factory) {
        if (!ScenarioContext.contains(key)) {
            ScenarioContext.put(key, factory.get());
        }
        return ScenarioContext.get(key);
    }

    /** Run a void domain action, capturing any {@link DomainException}. */
    public static void run(final Runnable action) {
        ScenarioContext.put(ERROR, null);
        try {
            action.run();
        } catch (final DomainException e) {
            ScenarioContext.put(ERROR, e);
        }
    }

    /** Run a value-returning domain action; stores result or error, returns result or null. */
    public static <T> T capture(final Supplier<T> action) {
        ScenarioContext.put(ERROR, null);
        try {
            final T result = action.get();
            ScenarioContext.put(RESULT, result);
            return result;
        } catch (final DomainException e) {
            ScenarioContext.put(ERROR, e);
            return null;
        }
    }

    public static DomainException lastError() {
        return ScenarioContext.get(ERROR);
    }

    public static <T> T lastResult() {
        return ScenarioContext.get(RESULT);
    }

    public static void put(final String key, final Object value) {
        ScenarioContext.put(key, value);
    }

    public static <T> T get(final String key) {
        return ScenarioContext.get(key);
    }
}
