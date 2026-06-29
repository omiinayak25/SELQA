package com.omiinqa.bdd.context;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-scenario shared state between step-definition classes (lightweight DI).
 *
 * <p>Cucumber instantiates each glue class separately, so steps in different
 * classes cannot share fields directly. Rather than pull in a DI container, a
 * {@link ThreadLocal} bag carries the "current page" and scenario data across
 * steps — correct even if scenarios are later parallelized. {@link #clear()} is
 * called from the {@code @After} hook so state never leaks between scenarios.</p>
 */
public final class ScenarioContext {

    private static final ThreadLocal<Map<String, Object>> STORE =
            ThreadLocal.withInitial(HashMap::new);

    private ScenarioContext() {
    }

    public static void put(final String key, final Object value) {
        STORE.get().put(key, value);
    }

    @SuppressWarnings("unchecked")
    public static <T> T get(final String key) {
        return (T) STORE.get().get(key);
    }

    public static boolean contains(final String key) {
        return STORE.get().containsKey(key);
    }

    public static void clear() {
        STORE.get().clear();
        STORE.remove();
    }

    // Common typed keys
    public static final String CURRENT_PAGE = "currentPage";
    public static final String LOGIN_PAGE = "loginPage";
    public static final String PRODUCTS_PAGE = "productsPage";
    public static final String CART_PAGE = "cartPage";
    public static final String CHECKOUT_COMPLETE = "checkoutComplete";
}
