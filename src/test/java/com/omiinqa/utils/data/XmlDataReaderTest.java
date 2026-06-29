package com.omiinqa.utils.data;

import com.omiinqa.exceptions.DataException;
import org.assertj.core.api.SoftAssertions;
import org.testng.annotations.Test;
import org.w3c.dom.Document;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Offline unit tests for {@link XmlDataReader}.
 *
 * <p>All tests are offline — no network, no database, no Selenium. The test
 * fixture files live under {@code src/test/resources/testdata/}.</p>
 *
 * <p>Test groups: {@code data} (data-layer suite) and {@code unit}.</p>
 */
public class XmlDataReaderTest {

    private static final String SAMPLE_XML     = "testdata/sample-data.xml";
    private static final String XXE_ATTACK_XML = "testdata/xxe-attack.xml";
    private static final String MISSING_XML    = "testdata/does-not-exist.xml";

    // -------------------------------------------------------------------------
    // readDocument tests
    // -------------------------------------------------------------------------

    @Test(groups = {"data", "unit"},
          description = "readDocument returns a non-null, normalised Document for a valid XML file")
    public void readDocumentReturnsNonNullDocument() {
        Document doc = XmlDataReader.readDocument(SAMPLE_XML);
        assertThat(doc).isNotNull();
        assertThat(doc.getDocumentElement().getTagName()).isEqualTo("users");
    }

    @Test(groups = {"data", "unit"},
          description = "readDocument throws DataException for a missing classpath resource")
    public void readDocumentThrowsDataExceptionForMissingFile() {
        assertThatThrownBy(() -> XmlDataReader.readDocument(MISSING_XML))
                .isInstanceOf(DataException.class)
                .hasMessageContaining("not found");
    }

    @Test(groups = {"data", "unit"},
          description = "readDocument root element has the expected child count")
    public void readDocumentRootElementHasFiveUserChildren() {
        Document doc = XmlDataReader.readDocument(SAMPLE_XML);
        int userCount = doc.getElementsByTagName("user").getLength();
        assertThat(userCount).isEqualTo(5);
    }

    // -------------------------------------------------------------------------
    // readRecords tests
    // -------------------------------------------------------------------------

    @Test(groups = {"data", "unit"},
          description = "readRecords returns a list with one Map per matching element")
    public void readRecordsReturnsCorrectNumberOfMaps() {
        List<Map<String, String>> records = XmlDataReader.readRecords(SAMPLE_XML, "user");
        assertThat(records).hasSize(5);
    }

    @Test(groups = {"data", "unit"},
          description = "readRecords maps contain all expected child tag keys")
    public void readRecordsMapsContainExpectedKeys() {
        List<Map<String, String>> records = XmlDataReader.readRecords(SAMPLE_XML, "user");
        for (Map<String, String> record : records) {
            assertThat(record).containsKeys("id", "name", "email", "status");
        }
    }

    @Test(groups = {"data", "unit"},
          description = "readRecords extracts the first record's field values correctly")
    public void readRecordsFirstRecordHasCorrectValues() {
        List<Map<String, String>> records = XmlDataReader.readRecords(SAMPLE_XML, "user");
        Map<String, String> first = records.get(0);

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(first.get("id")).isEqualTo("U001");
        softly.assertThat(first.get("name")).isEqualTo("Alice Johnson");
        softly.assertThat(first.get("email")).isEqualTo("alice.johnson@example.com");
        softly.assertThat(first.get("status")).isEqualTo("ACTIVE");
        softly.assertAll();
    }

    @Test(groups = {"data", "unit"},
          description = "readRecords with a non-existent tag name returns an empty list")
    public void readRecordsReturnsEmptyListForUnknownTag() {
        List<Map<String, String>> records = XmlDataReader.readRecords(SAMPLE_XML, "nonexistent");
        assertThat(records).isEmpty();
    }

    @Test(groups = {"data", "unit"},
          description = "readRecords contains expected status values across all records")
    public void readRecordsStatusValuesAreParsedCorrectly() {
        List<Map<String, String>> records = XmlDataReader.readRecords(SAMPLE_XML, "user");
        List<String> statuses = records.stream()
                .map(r -> r.get("status"))
                .toList();
        assertThat(statuses).containsExactly("ACTIVE", "INACTIVE", "ACTIVE", "SUSPENDED", "ACTIVE");
    }

    @Test(groups = {"data", "unit"},
          description = "readRecords extracts all unique IDs from the fixture")
    public void readRecordsAllIdsArePresent() {
        List<Map<String, String>> records = XmlDataReader.readRecords(SAMPLE_XML, "user");
        List<String> ids = records.stream().map(r -> r.get("id")).toList();
        assertThat(ids).containsExactly("U001", "U002", "U003", "U004", "U005");
    }

    // -------------------------------------------------------------------------
    // valueByXPath tests
    // -------------------------------------------------------------------------

    @Test(groups = {"data", "unit"},
          description = "valueByXPath retrieves the correct text for a specific predicate query")
    public void valueByXPathReturnsCorrectValueForPredicateQuery() {
        String name = XmlDataReader.valueByXPath(SAMPLE_XML, "//user[id='U003']/name");
        assertThat(name).isEqualTo("Carol White");
    }

    @Test(groups = {"data", "unit"},
          description = "valueByXPath returns the correct email for a given id")
    public void valueByXPathReturnsEmailForId() {
        String email = XmlDataReader.valueByXPath(SAMPLE_XML, "//user[id='U005']/email");
        assertThat(email).isEqualTo("eve.torres@example.com");
    }

    @Test(groups = {"data", "unit"},
          description = "valueByXPath returns an empty string when the XPath matches nothing")
    public void valueByXPathReturnsEmptyStringForNoMatch() {
        String result = XmlDataReader.valueByXPath(SAMPLE_XML, "//user[id='ZZZZ']/name");
        assertThat(result).isEmpty();
    }

    @Test(groups = {"data", "unit"},
          description = "valueByXPath can count matching elements via count()")
    public void valueByXPathCountFunctionReturnsCorrectTotal() {
        // count(...) returns an XPath number; STRING coercion renders it as "5"
        // (XPath 1.0) on most JDK Xerces builds, but a few render "5.0". Assert
        // numerically so the test is robust to that formatting difference.
        String count = XmlDataReader.valueByXPath(SAMPLE_XML, "count(//user)");
        assertThat(Double.parseDouble(count)).isEqualTo(5.0);
    }

    @Test(groups = {"data", "unit"},
          description = "valueByXPath retrieves status of a SUSPENDED user")
    public void valueByXPathRetrievesSuspendedUserStatus() {
        String status = XmlDataReader.valueByXPath(SAMPLE_XML, "//user[id='U004']/status");
        assertThat(status).isEqualTo("SUSPENDED");
    }

    // -------------------------------------------------------------------------
    // XXE safety tests
    // -------------------------------------------------------------------------

    /**
     * Verifies that the XXE-hardened parser rejects documents containing a
     * {@code <!DOCTYPE>} declaration.
     *
     * <p><strong>Why this test exists:</strong> An XML file with a DOCTYPE and an
     * external entity reference ({@code <!ENTITY xxe SYSTEM "file:///etc/passwd">})
     * can exfiltrate local files when parsed by an unhardened parser. The parser
     * must throw <em>before</em> any entity resolution occurs. We assert that
     * {@link XmlDataReader#readDocument} throws {@link DataException} — this
     * confirms that {@code disallow-doctype-decl} is active.</p>
     */
    @Test(groups = {"data", "unit"},
          description = "XXE safety: parsing a DOCTYPE-containing XML document is rejected with DataException")
    public void xxeSafetyDocTypeRejected() {
        assertThatThrownBy(() -> XmlDataReader.readDocument(XXE_ATTACK_XML))
                .isInstanceOf(DataException.class)
                .hasMessageContaining("xxe-attack.xml");
    }

    /**
     * Confirms that even {@code readRecords} (which calls {@code readDocument}
     * internally) rejects an XXE document, verifying the defence is end-to-end.
     */
    @Test(groups = {"data", "unit"},
          description = "XXE safety: readRecords also rejects a DOCTYPE-containing XML document")
    public void xxeSafetyReadRecordsAlsoRejectsDocType() {
        assertThatThrownBy(() -> XmlDataReader.readRecords(XXE_ATTACK_XML, "user"))
                .isInstanceOf(DataException.class);
    }

    /**
     * Verifies the clean XML fixture does NOT trigger the XXE guard — ensuring
     * the hardening does not break legitimate parsing.
     */
    @Test(groups = {"data", "unit"},
          description = "XXE safety: legitimate XML without DOCTYPE parses successfully")
    public void xxeSafetyLegitimateXmlParsesSuccessfully() {
        // Must NOT throw
        Document doc = XmlDataReader.readDocument(SAMPLE_XML);
        assertThat(doc).isNotNull();
    }
}
