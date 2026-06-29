package com.omiinqa.reference.platform;

import com.omiinqa.reference.core.DomainException;
import com.omiinqa.reference.core.Validations;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory typed settings service for the reference platform domain.
 *
 * <p>Manages key-value settings backed by a registry of {@link SettingDefinition}s
 * that declare each setting's type, default value, required flag, range constraints,
 * and (for ENUM settings) the set of allowed values. State is per-instance so each
 * BDD scenario receives a fully isolated world with the built-in registry pre-loaded.</p>
 *
 * <h3>Built-in settings</h3>
 * <ul>
 *   <li>{@code app.name} — STRING, required, default {@code "OmiinQA"}</li>
 *   <li>{@code max.retries} — INT, min 1 / max 10, default {@code "3"}</li>
 *   <li>{@code debug.enabled} — BOOLEAN, default {@code "false"}</li>
 *   <li>{@code log.level} — ENUM {@code [DEBUG, INFO, WARN, ERROR]}, default {@code "INFO"}</li>
 *   <li>{@code theme} — ENUM {@code [LIGHT, DARK, HIGH_CONTRAST]}, default {@code "LIGHT"}</li>
 *   <li>{@code session.timeout} — INT, min 5 / max 1440, default {@code "30"}</li>
 *   <li>{@code notifications.email} — BOOLEAN, default {@code "true"}</li>
 *   <li>{@code timezone} — STRING, default {@code "UTC"}</li>
 * </ul>
 *
 * <h3>Error codes (asserted by BDD scenarios)</h3>
 * <ul>
 *   <li>{@code SETTINGS_UNKNOWN_KEY} — key is not registered in the settings registry</li>
 *   <li>{@code SETTINGS_REQUIRED} — a required setting was set to a blank value</li>
 *   <li>{@code SETTINGS_OUT_OF_RANGE} — INT value is non-parseable or outside [min, max]</li>
 *   <li>{@code SETTINGS_BAD_ENUM} — ENUM value is not in the allowed list, or BOOLEAN
 *       value is not {@code "true"} / {@code "false"}</li>
 *   <li>{@code SETTINGS_DUPLICATE_KEY} — {@link #register(SettingDefinition)} called for
 *       an already-registered key</li>
 * </ul>
 */
public class SettingsService {

    // ─── Types ────────────────────────────────────────────────────────────────

    /**
     * The data type of a registered setting, used to drive validation in
     * {@link SettingsService#set(String, String)}.
     */
    public enum SettingType {
        /** Arbitrary text value. Validated only for blank-if-required. */
        STRING,
        /** Integer value optionally bounded by {@link SettingDefinition#getMinValue()} and
         *  {@link SettingDefinition#getMaxValue()}. */
        INT,
        /** Accepts {@code "true"} or {@code "false"} (case-insensitive) only. */
        BOOLEAN,
        /** Value must appear in {@link SettingDefinition#getAllowedEnumValues()}. */
        ENUM
    }

    /**
     * Immutable description of a single registered setting: its key, type, default
     * value, required flag, numeric bounds, and (for ENUM) the accepted values.
     *
     * <p>Construct via {@link SettingDefinition#builder()} or the all-args constructor.</p>
     */
    public static final class SettingDefinition {

        private final String key;
        private final SettingType type;
        private final String defaultValue;
        private final boolean required;
        private final int minValue;
        private final int maxValue;
        private final List<String> allowedEnumValues;

        /**
         * All-args constructor. Prefer {@link #builder()} for readability.
         *
         * @param key               the unique setting key (e.g. {@code "app.name"})
         * @param type              the setting type
         * @param defaultValue      value used when the setting is reset or newly initialized
         * @param required          when {@code true}, blank values are rejected
         * @param minValue          inclusive lower bound for INT settings (ignored otherwise)
         * @param maxValue          inclusive upper bound for INT settings (ignored otherwise)
         * @param allowedEnumValues non-null list of accepted values for ENUM settings
         */
        public SettingDefinition(final String key, final SettingType type,
                                 final String defaultValue, final boolean required,
                                 final int minValue, final int maxValue,
                                 final List<String> allowedEnumValues) {
            this.key = key;
            this.type = type;
            this.defaultValue = defaultValue;
            this.required = required;
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.allowedEnumValues = allowedEnumValues == null
                    ? Collections.emptyList()
                    : Collections.unmodifiableList(allowedEnumValues);
        }

        /** @return the unique key that identifies this setting */
        public String getKey() { return key; }

        /** @return the declared {@link SettingType} */
        public SettingType getType() { return type; }

        /** @return the value to restore on {@link SettingsService#reset(String)} */
        public String getDefaultValue() { return defaultValue; }

        /** @return {@code true} if a blank value must be rejected */
        public boolean isRequired() { return required; }

        /** @return inclusive minimum for INT settings */
        public int getMinValue() { return minValue; }

        /** @return inclusive maximum for INT settings */
        public int getMaxValue() { return maxValue; }

        /** @return unmodifiable list of accepted values for ENUM settings */
        public List<String> getAllowedEnumValues() { return allowedEnumValues; }

        // ─── Builder ─────────────────────────────────────────────────────────

        /** Creates a fresh {@link Builder} for constructing a {@link SettingDefinition}. */
        public static Builder builder() { return new Builder(); }

        /**
         * Fluent builder for {@link SettingDefinition}.
         *
         * <p>Defaults: {@code required=false}, {@code minValue=Integer.MIN_VALUE},
         * {@code maxValue=Integer.MAX_VALUE}, {@code allowedEnumValues=[]}.</p>
         */
        public static final class Builder {
            private String key;
            private SettingType type;
            private String defaultValue;
            private boolean required = false;
            private int minValue = Integer.MIN_VALUE;
            private int maxValue = Integer.MAX_VALUE;
            private List<String> allowedEnumValues = Collections.emptyList();

            private Builder() {}

            /** @param key the unique setting key */
            public Builder key(final String key) { this.key = key; return this; }

            /** @param type the setting type */
            public Builder type(final SettingType type) { this.type = type; return this; }

            /** @param defaultValue initial and reset value */
            public Builder defaultValue(final String defaultValue) {
                this.defaultValue = defaultValue; return this;
            }

            /** @param required whether blank values are forbidden */
            public Builder required(final boolean required) {
                this.required = required; return this;
            }

            /** @param minValue inclusive lower bound (for INT settings) */
            public Builder minValue(final int minValue) {
                this.minValue = minValue; return this;
            }

            /** @param maxValue inclusive upper bound (for INT settings) */
            public Builder maxValue(final int maxValue) {
                this.maxValue = maxValue; return this;
            }

            /** @param allowedEnumValues accepted values (for ENUM settings) */
            public Builder allowedEnumValues(final List<String> allowedEnumValues) {
                this.allowedEnumValues = allowedEnumValues; return this;
            }

            /** Builds and returns the {@link SettingDefinition}. */
            public SettingDefinition build() {
                return new SettingDefinition(key, type, defaultValue, required,
                        minValue, maxValue, allowedEnumValues);
            }
        }
    }

    // ─── State ────────────────────────────────────────────────────────────────

    /** Registry of declared settings: key → definition. */
    private final ConcurrentHashMap<String, SettingDefinition> registry = new ConcurrentHashMap<>();

    /** Live values: key → current value (starts at defaultValue). */
    private final ConcurrentHashMap<String, String> values = new ConcurrentHashMap<>();

    // ─── Construction ─────────────────────────────────────────────────────────

    /**
     * Creates a new {@code SettingsService} with the eight built-in settings
     * pre-registered and set to their default values.
     */
    public SettingsService() {
        registerBuiltins();
    }

    private void registerBuiltins() {
        registerInternal(SettingDefinition.builder()
                .key("app.name").type(SettingType.STRING)
                .defaultValue("OmiinQA").required(true)
                .build());

        registerInternal(SettingDefinition.builder()
                .key("max.retries").type(SettingType.INT)
                .defaultValue("3").required(false)
                .minValue(1).maxValue(10)
                .build());

        registerInternal(SettingDefinition.builder()
                .key("debug.enabled").type(SettingType.BOOLEAN)
                .defaultValue("false").required(false)
                .build());

        registerInternal(SettingDefinition.builder()
                .key("log.level").type(SettingType.ENUM)
                .defaultValue("INFO").required(false)
                .allowedEnumValues(Arrays.asList("DEBUG", "INFO", "WARN", "ERROR"))
                .build());

        registerInternal(SettingDefinition.builder()
                .key("theme").type(SettingType.ENUM)
                .defaultValue("LIGHT").required(false)
                .allowedEnumValues(Arrays.asList("LIGHT", "DARK", "HIGH_CONTRAST"))
                .build());

        registerInternal(SettingDefinition.builder()
                .key("session.timeout").type(SettingType.INT)
                .defaultValue("30").required(false)
                .minValue(5).maxValue(1440)
                .build());

        registerInternal(SettingDefinition.builder()
                .key("notifications.email").type(SettingType.BOOLEAN)
                .defaultValue("true").required(false)
                .build());

        registerInternal(SettingDefinition.builder()
                .key("timezone").type(SettingType.STRING)
                .defaultValue("UTC").required(false)
                .build());
    }

    /** Registers a definition and sets its initial live value to the default. */
    private void registerInternal(final SettingDefinition def) {
        registry.put(def.getKey(), def);
        values.put(def.getKey(), def.getDefaultValue());
    }

    // ─── Public API ───────────────────────────────────────────────────────────

    /**
     * Returns the current value of the setting identified by {@code key}.
     *
     * @param key the setting key
     * @return current value (never {@code null} for registered keys)
     * @throws DomainException {@code SETTINGS_UNKNOWN_KEY} if {@code key} is not registered
     */
    public String get(final String key) {
        requireRegistered(key);
        return values.get(key);
    }

    /**
     * Updates the setting identified by {@code key} to {@code value} after
     * passing all type-specific validations.
     *
     * <p>Validation order:</p>
     * <ol>
     *   <li>Key must be registered ({@code SETTINGS_UNKNOWN_KEY}).</li>
     *   <li>If required, value must not be blank ({@code SETTINGS_REQUIRED}).</li>
     *   <li>Type-specific validation — {@code SETTINGS_OUT_OF_RANGE} for INT
     *       that is non-parseable or out of [min, max]; {@code SETTINGS_BAD_ENUM}
     *       for ENUM not in the allowed list or BOOLEAN not {@code "true"/"false"}.</li>
     * </ol>
     *
     * @param key   the setting key
     * @param value the new value to assign
     * @throws DomainException {@code SETTINGS_UNKNOWN_KEY} if not registered
     * @throws DomainException {@code SETTINGS_REQUIRED} if required and value is blank
     * @throws DomainException {@code SETTINGS_OUT_OF_RANGE} if INT is invalid or out of range
     * @throws DomainException {@code SETTINGS_BAD_ENUM} if ENUM or BOOLEAN value is invalid
     */
    public void set(final String key, final String value) {
        final SettingDefinition def = requireRegistered(key);

        if (def.isRequired() && Validations.isBlank(value)) {
            throw new DomainException("SETTINGS_REQUIRED",
                    "Setting '" + key + "' is required and must not be blank");
        }

        switch (def.getType()) {
            case INT:
                validateInt(key, value, def);
                break;
            case BOOLEAN:
                validateBoolean(key, value);
                break;
            case ENUM:
                validateEnum(key, value, def);
                break;
            case STRING:
            default:
                // STRING: required-blank already checked above; no further constraint
                break;
        }

        values.put(key, value);
    }

    /**
     * Resets the setting identified by {@code key} to its registered default value.
     *
     * @param key the setting key
     * @throws DomainException {@code SETTINGS_UNKNOWN_KEY} if {@code key} is not registered
     */
    public void reset(final String key) {
        final SettingDefinition def = requireRegistered(key);
        values.put(key, def.getDefaultValue());
    }

    /**
     * Resets every registered setting to its declared default value atomically
     * from the caller's perspective (iterates all registry entries).
     */
    public void resetAll() {
        registry.forEach((k, def) -> values.put(k, def.getDefaultValue()));
    }

    /**
     * Returns a snapshot of all current key→value pairs.
     *
     * @return unmodifiable map of current setting values
     */
    public Map<String, String> getAll() {
        return Collections.unmodifiableMap(new HashMap<>(values));
    }

    /**
     * Returns {@code true} if {@code key} is present in the settings registry
     * (i.e., a {@link SettingDefinition} has been registered for it).
     *
     * @param key the setting key to test
     * @return {@code true} if registered, {@code false} otherwise
     */
    public boolean isDefined(final String key) {
        return registry.containsKey(key);
    }

    /**
     * Registers a new {@link SettingDefinition} at runtime and initialises its
     * live value to its declared default.
     *
     * @param def the definition to register
     * @throws DomainException {@code SETTINGS_DUPLICATE_KEY} if a definition for
     *                         {@code def.getKey()} already exists
     */
    public void register(final SettingDefinition def) {
        if (registry.containsKey(def.getKey())) {
            throw new DomainException("SETTINGS_DUPLICATE_KEY",
                    "Setting '" + def.getKey() + "' is already registered");
        }
        registerInternal(def);
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    /**
     * Looks up and returns the {@link SettingDefinition} for {@code key}, throwing
     * {@code SETTINGS_UNKNOWN_KEY} if it is not registered.
     */
    private SettingDefinition requireRegistered(final String key) {
        final SettingDefinition def = registry.get(key);
        if (def == null) {
            throw new DomainException("SETTINGS_UNKNOWN_KEY",
                    "Unknown setting key: '" + key + "'");
        }
        return def;
    }

    /**
     * Validates that {@code value} is a parseable integer within the definition's
     * [{@code minValue}, {@code maxValue}] range.
     */
    private void validateInt(final String key, final String value, final SettingDefinition def) {
        final int parsed;
        try {
            parsed = Integer.parseInt(value);
        } catch (final NumberFormatException e) {
            throw new DomainException("SETTINGS_OUT_OF_RANGE",
                    "Setting '" + key + "' requires an integer value, got: '" + value + "'");
        }
        if (parsed < def.getMinValue() || parsed > def.getMaxValue()) {
            throw new DomainException("SETTINGS_OUT_OF_RANGE",
                    "Setting '" + key + "' value " + parsed
                            + " is out of range [" + def.getMinValue() + ", " + def.getMaxValue() + "]");
        }
    }

    /**
     * Validates that {@code value} is {@code "true"} or {@code "false"}
     * (case-insensitive); otherwise raises {@code SETTINGS_BAD_ENUM}.
     */
    private void validateBoolean(final String key, final String value) {
        if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
            throw new DomainException("SETTINGS_BAD_ENUM",
                    "Setting '" + key + "' is a boolean; only true/false allowed, got: '" + value + "'");
        }
    }

    /**
     * Validates that {@code value} appears in the definition's allowed-values list;
     * otherwise raises {@code SETTINGS_BAD_ENUM}.
     */
    private void validateEnum(final String key, final String value, final SettingDefinition def) {
        if (!def.getAllowedEnumValues().contains(value)) {
            throw new DomainException("SETTINGS_BAD_ENUM",
                    "Setting '" + key + "' value '" + value
                            + "' is not one of " + def.getAllowedEnumValues());
        }
    }
}
