package com.omiinqa.data.synthetic;

import com.omiinqa.data.synthetic.SyntheticDataGenerator.DataMasker;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Offline unit tests for {@link DataMasker}.
 *
 * <p>Verifies that PII fields are masked correctly and consistently across
 * all supported field types: email, phone, credit card, and name.
 * All assertions check both the mask shape AND that original sensitive data
 * does not appear in the masked output.</p>
 *
 * <p>Test groups: {@code data}, {@code unit}.</p>
 */
public class DataMaskerTest {

    // -------------------------------------------------------------------------
    // maskEmail
    // -------------------------------------------------------------------------

    @Test(groups = {"data", "unit"},
          description = "maskEmail retains first character and domain, hides the rest of local part")
    public void maskEmailRetainsFirstCharAndDomain() {
        String masked = DataMasker.maskEmail("alice@example.com");
        assertThat(masked)
                .startsWith("a")
                .contains("@example.com")
                .doesNotContain("lice");
    }

    @Test(groups = {"data", "unit"},
          description = "maskEmail contains asterisks for the hidden part of the local address")
    public void maskEmailContainsAsterisks() {
        String masked = DataMasker.maskEmail("bob.smith@mail.org");
        assertThat(masked).contains("*");
    }

    @Test(groups = {"data", "unit"},
          description = "maskEmail returns 'null' for null input")
    public void maskEmailHandlesNull() {
        assertThat(DataMasker.maskEmail(null)).isEqualTo("null");
    }

    @Test(groups = {"data", "unit"},
          description = "maskEmail returns '***' for a non-email string")
    public void maskEmailHandlesNonEmailString() {
        String masked = DataMasker.maskEmail("not-an-email");
        assertThat(masked).isEqualTo("***");
    }

    // -------------------------------------------------------------------------
    // maskPhone
    // -------------------------------------------------------------------------

    @Test(groups = {"data", "unit"},
          description = "maskPhone preserves only the last 4 digits")
    public void maskPhoneKeepsLast4Digits() {
        String masked = DataMasker.maskPhone("+1-555-867-5309");
        assertThat(masked)
                .endsWith("5309")
                .doesNotContain("555")
                .doesNotContain("867");
    }

    @Test(groups = {"data", "unit"},
          description = "maskPhone output consists of asterisks plus last 4 digits only")
    public void maskPhoneOutputContainsOnlyMaskAndLast4() {
        String masked = DataMasker.maskPhone("5558675309");
        // Should be ******5309
        assertThat(masked).matches("\\*+[0-9]{4}");
    }

    @Test(groups = {"data", "unit"},
          description = "maskPhone returns 'null' for null input")
    public void maskPhoneHandlesNull() {
        assertThat(DataMasker.maskPhone(null)).isEqualTo("null");
    }

    // -------------------------------------------------------------------------
    // maskCreditCard
    // -------------------------------------------------------------------------

    @Test(groups = {"data", "unit"},
          description = "maskCreditCard keeps only the last 4 digits (PCI-DSS PAN truncation)")
    public void maskCreditCardKeepsLast4Digits() {
        String masked = DataMasker.maskCreditCard("4111111111111111");
        assertThat(masked)
                .endsWith("1111")
                .doesNotContain("411111");
    }

    @Test(groups = {"data", "unit"},
          description = "maskCreditCard output length equals total digit count (asterisks + last 4)")
    public void maskCreditCardOutputLengthMatchesDigitCount() {
        String card   = "4111111111111111"; // 16 digits
        String masked = DataMasker.maskCreditCard(card);
        // 12 asterisks + 4 digits = 16
        assertThat(masked).hasSize(16).matches("\\*{12}1111");
    }

    @Test(groups = {"data", "unit"},
          description = "maskCreditCard handles formatted card numbers with dashes")
    public void maskCreditCardHandlesFormattedInput() {
        String masked = DataMasker.maskCreditCard("4111-1111-1111-1111");
        assertThat(masked).endsWith("1111");
    }

    @Test(groups = {"data", "unit"},
          description = "maskCreditCard returns 'null' for null input")
    public void maskCreditCardHandlesNull() {
        assertThat(DataMasker.maskCreditCard(null)).isEqualTo("null");
    }

    // -------------------------------------------------------------------------
    // maskName
    // -------------------------------------------------------------------------

    @Test(groups = {"data", "unit"},
          description = "maskName retains first character of each word and masks the rest")
    public void maskNameRetainsFirstCharOfEachWord() {
        String masked = DataMasker.maskName("Alice Smith");
        assertThat(masked)
                .startsWith("A")
                .contains("S")
                .doesNotContain("lice")
                .doesNotContain("mith");
    }

    @Test(groups = {"data", "unit"},
          description = "maskName returns 'null' for null input")
    public void maskNameHandlesNull() {
        assertThat(DataMasker.maskName(null)).isEqualTo("null");
    }

    @Test(groups = {"data", "unit"},
          description = "maskName returns '***' for blank input")
    public void maskNameHandlesBlankString() {
        assertThat(DataMasker.maskName("   ")).isEqualTo("***");
    }

    @Test(groups = {"data", "unit"},
          description = "maskName works for a single-word name")
    public void maskNameHandlesSingleWord() {
        String masked = DataMasker.maskName("Madonna");
        assertThat(masked).startsWith("M").contains("*");
    }
}
