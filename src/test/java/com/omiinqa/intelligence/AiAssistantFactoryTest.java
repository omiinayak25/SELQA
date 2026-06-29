package com.omiinqa.intelligence;

import com.omiinqa.intelligence.ai.AiAssistant;
import com.omiinqa.intelligence.ai.AiAssistantFactory;
import com.omiinqa.intelligence.ai.NoOpAiAssistant;
import org.testng.annotations.Test;

import java.util.Optional;

import static org.testng.Assert.*;

/**
 * Offline unit tests for {@link AiAssistantFactory} and {@link NoOpAiAssistant}.
 *
 * <p>These tests do NOT require an AI API key. They assert the factory's
 * no-op default behaviour when credentials are absent. No network calls
 * are ever made.</p>
 *
 * <p>If {@code OMIINQA_AI_API_KEY} happens to be set in the test environment,
 * the factory-default tests are skipped gracefully — they assert on the
 * no-op path only.</p>
 */
@Test(groups = {"intelligence", "unit"})
public class AiAssistantFactoryTest {

    // =========================================================================
    // NoOp factory method
    // =========================================================================

    @Test(groups = {"intelligence", "unit"})
    public void noOpFactoryMethodReturnsNoOpInstance() {
        final AiAssistant ai = AiAssistantFactory.noOp();
        assertNotNull(ai);
        assertTrue(ai instanceof NoOpAiAssistant,
                "noOp() must return a NoOpAiAssistant");
    }

    // =========================================================================
    // NoOpAiAssistant behaviour
    // =========================================================================

    @Test(groups = {"intelligence", "unit"})
    public void noOpSuggestLocatorReturnsEmpty() {
        final AiAssistant ai = AiAssistantFactory.noOp();
        final Optional<String> result = ai.suggestLocator("submit button");
        assertNotNull(result);
        assertFalse(result.isPresent(),
                "NoOpAiAssistant.suggestLocator must always return Optional.empty()");
    }

    @Test(groups = {"intelligence", "unit"})
    public void noOpCategorizeFailureReturnsEmpty() {
        final AiAssistant ai = AiAssistantFactory.noOp();
        final Optional<String> result = ai.categorizeFailure("NoSuchElementException: button not found");
        assertNotNull(result);
        assertFalse(result.isPresent(),
                "NoOpAiAssistant.categorizeFailure must always return Optional.empty()");
    }

    @Test(groups = {"intelligence", "unit"})
    public void noOpSummarizeReturnsEmpty() {
        final AiAssistant ai = AiAssistantFactory.noOp();
        final Optional<String> result = ai.summarize("Test run completed with 3 failures.");
        assertNotNull(result);
        assertFalse(result.isPresent(),
                "NoOpAiAssistant.summarize must always return Optional.empty()");
    }

    @Test(groups = {"intelligence", "unit"})
    public void noOpHandlesNullInputsGracefully() {
        final AiAssistant ai = AiAssistantFactory.noOp();
        // Must not throw on null inputs
        assertFalse(ai.suggestLocator(null).isPresent());
        assertFalse(ai.categorizeFailure(null).isPresent());
        assertFalse(ai.summarize(null).isPresent());
    }

    @Test(groups = {"intelligence", "unit"})
    public void noOpHandlesEmptyStringInputs() {
        final AiAssistant ai = AiAssistantFactory.noOp();
        assertFalse(ai.suggestLocator("").isPresent());
        assertFalse(ai.categorizeFailure("").isPresent());
        assertFalse(ai.summarize("").isPresent());
    }

    // =========================================================================
    // Factory — default behaviour when no key is set
    // =========================================================================

    @Test(groups = {"intelligence", "unit"})
    public void getDefaultReturnsNonNullAssistant() {
        final AiAssistant ai = AiAssistantFactory.getDefault();
        assertNotNull(ai, "getDefault() must never return null");
    }

    @Test(groups = {"intelligence", "unit"})
    public void getDefaultWithoutKeyReturnsNoOp() {
        // Only assert no-op path when the key is genuinely absent
        if (AiAssistantFactory.isAiAvailable()) {
            // Key is set in this environment — skip the no-op assertion
            return;
        }
        final AiAssistant ai = AiAssistantFactory.getDefault();
        assertTrue(ai instanceof NoOpAiAssistant,
                "Without OMIINQA_AI_API_KEY, getDefault() must return NoOpAiAssistant");
    }

    @Test(groups = {"intelligence", "unit"})
    public void getDefaultWithoutKeyNeverMakesNetworkCall() {
        // Regardless of whether AI is available, the noOp path must return empty
        if (AiAssistantFactory.isAiAvailable()) {
            return; // can't test no-op network absence when key is present
        }
        final AiAssistant ai = AiAssistantFactory.getDefault();
        // These calls must complete immediately (no blocking network I/O)
        final long start = System.currentTimeMillis();
        ai.suggestLocator("anything");
        ai.categorizeFailure("anything");
        ai.summarize("anything");
        final long elapsed = System.currentTimeMillis() - start;
        assertTrue(elapsed < 500,
                "NoOp calls must complete in <500ms — no network I/O: elapsed=" + elapsed + "ms");
    }

    // =========================================================================
    // isAiAvailable
    // =========================================================================

    @Test(groups = {"intelligence", "unit"})
    public void isAiAvailableReturnsFalseWhenNoKey() {
        if (System.getenv(AiAssistantFactory.ENV_API_KEY) != null) {
            // Key IS set — skip this assertion
            return;
        }
        assertFalse(AiAssistantFactory.isAiAvailable(),
                "isAiAvailable() must return false when OMIINQA_AI_API_KEY is not set");
    }

    // =========================================================================
    // Constants
    // =========================================================================

    @Test(groups = {"intelligence", "unit"})
    public void envConstantsAreNonBlank() {
        assertNotNull(AiAssistantFactory.ENV_API_KEY);
        assertFalse(AiAssistantFactory.ENV_API_KEY.isBlank());
        assertNotNull(AiAssistantFactory.ENV_API_URL);
        assertFalse(AiAssistantFactory.ENV_API_URL.isBlank());
        assertNotNull(AiAssistantFactory.ENV_MODEL);
        assertFalse(AiAssistantFactory.ENV_MODEL.isBlank());
    }
}
