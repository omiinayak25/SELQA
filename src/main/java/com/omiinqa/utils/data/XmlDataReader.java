package com.omiinqa.utils.data;

import com.omiinqa.exceptions.DataException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Classpath-based XML test-data reader backed by the JDK's built-in
 * {@link javax.xml.parsers.DocumentBuilder} — <strong>zero extra dependencies</strong>.
 *
 * <p><strong>Pattern:</strong> Utility / Static Factory — mirrors the design of
 * {@link JsonDataReader}: all methods are static, the class is not instantiable,
 * and errors surface as {@link DataException} (unchecked) so tests fail fast with
 * a clear data-layer message.</p>
 *
 * <h2>XXE Hardening — why this matters</h2>
 * <p>XML External Entity (XXE) injection is <em>OWASP Top 10 A05 (Security
 * Misconfiguration)</em>. An attacker-controlled XML file containing a DOCTYPE
 * declaration that references an external entity (e.g., {@code file:///etc/passwd})
 * can cause the parser to read arbitrary local files or trigger SSRF when running
 * in a CI/CD environment with network access.</p>
 *
 * <p>This class hardens the parser by:
 * <ol>
 *   <li>Enabling {@code FEATURE_SECURE_PROCESSING} — a JDK-standard flag that
 *       disables many insecure parser features in one call (limits entity
 *       expansion, disables access to external schemas, etc.).</li>
 *   <li>Explicitly disallowing DOCTYPE declarations via
 *       {@code http://apache.org/xml/features/disallow-doctype-decl} — if a
 *       test-data file contains a {@code <!DOCTYPE ...>} the parser throws
 *       immediately, preventing entity expansion entirely.</li>
 *   <li>Disabling external general entities and external parameter entities
 *       as defence-in-depth, in case the DOCTYPE feature flag is not honoured
 *       by a non-Xerces JDK implementation.</li>
 * </ol>
 * </p>
 *
 * <p><strong>Usage:</strong>
 * <pre>{@code
 *   // Parse the whole document
 *   Document doc = XmlDataReader.readDocument("testdata/sample-data.xml");
 *
 *   // Read all &lt;user&gt; records into Map&lt;String,String&gt; per record
 *   List&lt;Map&lt;String,String&gt;&gt; users = XmlDataReader.readRecords("testdata/sample-data.xml", "user");
 *
 *   // XPath query
 *   String status = XmlDataReader.valueByXPath("testdata/sample-data.xml", "//user[id='1']/status");
 * }</pre>
 * </p>
 */
public final class XmlDataReader {

    private static final Logger log = LoggerFactory.getLogger(XmlDataReader.class);

    /**
     * Apache Xerces feature URI that prohibits DOCTYPE declarations entirely.
     * This is the primary XXE defence: if a DOCTYPE is present the parser throws,
     * so external entities can never be resolved.
     */
    private static final String FEATURE_DISALLOW_DOCTYPE =
            "http://apache.org/xml/features/disallow-doctype-decl";

    /**
     * JDK/Xerces feature URI to disable external general entity expansion.
     * Defence-in-depth: guards against parsers that ignore DISALLOW_DOCTYPE.
     */
    private static final String FEATURE_EXTERNAL_GENERAL_ENTITIES =
            "http://xml.org/sax/features/external-general-entities";

    /**
     * JDK/Xerces feature URI to disable external parameter entity expansion.
     * Defence-in-depth: guards against parsers that ignore DISALLOW_DOCTYPE.
     */
    private static final String FEATURE_EXTERNAL_PARAMETER_ENTITIES =
            "http://xml.org/sax/features/external-parameter-entities";

    private XmlDataReader() {
        // utility — not instantiable
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Parses an XML file from the classpath and returns the raw DOM
     * {@link Document}. Use this when you need direct DOM traversal or
     * namespace-aware processing beyond what {@link #readRecords} provides.
     *
     * @param classpath classpath-relative path, e.g. {@code "testdata/sample-data.xml"}
     * @return parsed DOM document; never {@code null}
     * @throws DataException if the resource is missing, malformed, or the
     *                       document contains a DOCTYPE (XXE guard)
     */
    public static Document readDocument(final String classpath) {
        log.debug("Parsing XML document from classpath: '{}'", classpath);
        try (InputStream is = openStream(classpath)) {
            DocumentBuilder builder = createSecureDocumentBuilder();
            Document doc = builder.parse(is);
            doc.getDocumentElement().normalize();
            log.debug("Successfully parsed XML document from '{}'", classpath);
            return doc;
        } catch (DataException e) {
            throw e; // already wrapped
        } catch (Exception e) {
            throw new DataException("Failed to parse XML document from '" + classpath + "'", e);
        }
    }

    /**
     * Reads all elements matching {@code recordTag} from the XML document and
     * converts each one into a {@code Map<String, String>} where each key is a
     * child element's tag name and the value is its text content.
     *
     * <p>Example — given:
     * <pre>{@code
     *   <users>
     *     <user><id>1</id><name>Alice</name></user>
     *     <user><id>2</id><name>Bob</name></user>
     *   </users>
     * }</pre>
     * {@code readRecords("testdata/users.xml", "user")} returns:
     * <pre>{@code
     *   [ {"id": "1", "name": "Alice"}, {"id": "2", "name": "Bob"} ]
     * }</pre>
     * </p>
     *
     * <p>Only direct child element nodes are included; attributes and nested
     * elements are ignored at this level of the API.</p>
     *
     * @param classpath classpath-relative path to the XML file
     * @param recordTag local name of the element that represents one record
     *                  (case-sensitive)
     * @return ordered list of maps; empty list if no matching elements found
     * @throws DataException if the resource is missing, malformed, or DOCTYPE is present
     */
    public static List<Map<String, String>> readRecords(final String classpath,
                                                        final String recordTag) {
        log.debug("Reading records with tag '{}' from '{}'", recordTag, classpath);
        Document doc = readDocument(classpath);
        NodeList nodes = doc.getElementsByTagName(recordTag);
        List<Map<String, String>> records = new ArrayList<>(nodes.getLength());

        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                records.add(elementToMap((Element) node));
            }
        }

        log.debug("Found {} record(s) for tag '{}' in '{}'", records.size(), recordTag, classpath);
        return records;
    }

    /**
     * Evaluates an XPath expression against the given XML document and returns the
     * result as a {@link String}.
     *
     * <p>The XPath engine is the JDK's built-in {@link XPathFactory} — no extra
     * dependency is required.</p>
     *
     * <p>Example:
     * <pre>{@code
     *   String status = XmlDataReader.valueByXPath(
     *       "testdata/sample-data.xml",
     *       "//user[id='U001']/status");
     * }</pre>
     * </p>
     *
     * @param classpath  classpath-relative path to the XML file
     * @param xpathExpr  XPath 1.0 expression; must select a node or text value
     * @return string result of the XPath evaluation; empty string if no match
     * @throws DataException if the resource is missing, the XPath is invalid,
     *                       or the document contains a DOCTYPE (XXE guard)
     */
    public static String valueByXPath(final String classpath, final String xpathExpr) {
        log.debug("Evaluating XPath '{}' on '{}'", xpathExpr, classpath);
        Document doc = readDocument(classpath);
        try {
            XPath xpath = XPathFactory.newInstance().newXPath();
            XPathExpression expr = xpath.compile(xpathExpr);
            String result = (String) expr.evaluate(doc, XPathConstants.STRING);
            log.debug("XPath '{}' on '{}' => '{}'", xpathExpr, classpath, result);
            return result != null ? result : "";
        } catch (Exception e) {
            throw new DataException(
                    "Failed to evaluate XPath '" + xpathExpr + "' on '" + classpath + "'", e);
        }
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Creates an XXE-hardened {@link DocumentBuilder}.
     *
     * <p><strong>Security rationale (see class-level Javadoc for full detail):</strong>
     * <ul>
     *   <li>{@code FEATURE_SECURE_PROCESSING} — JDK standard flag; disables
     *       many insecure features in one call.</li>
     *   <li>{@code FEATURE_DISALLOW_DOCTYPE} — rejects any document containing a
     *       {@code <!DOCTYPE>}, making external entity injection impossible.</li>
     *   <li>{@code FEATURE_EXTERNAL_GENERAL_ENTITIES} set {@code false} — defence-in-depth.</li>
     *   <li>{@code FEATURE_EXTERNAL_PARAMETER_ENTITIES} set {@code false} — defence-in-depth.</li>
     *   <li>{@code setXIncludeAware(false)} and {@code setExpandEntityReferences(false)}
     *       — further reduces the attack surface for entity-based attacks.</li>
     * </ul>
     * </p>
     *
     * @return hardened builder
     * @throws DataException if the JDK XML subsystem cannot be configured
     */
    private static DocumentBuilder createSecureDocumentBuilder() {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

            // Primary XXE defence: JDK standard secure processing mode
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

            // Primary XXE defence: prohibit DOCTYPE declarations entirely.
            // Any XML file with <!DOCTYPE ...> will throw during parse(),
            // preventing ALL entity expansion before it can start.
            factory.setFeature(FEATURE_DISALLOW_DOCTYPE, true);

            // Defence-in-depth: disable external entity loading for implementations
            // that may not enforce DISALLOW_DOCTYPE (e.g., non-Xerces parsers).
            factory.setFeature(FEATURE_EXTERNAL_GENERAL_ENTITIES, false);
            factory.setFeature(FEATURE_EXTERNAL_PARAMETER_ENTITIES, false);

            // Disable XInclude processing (another expansion vector)
            factory.setXIncludeAware(false);

            // Do not expand entity references — return them as EntityReference nodes
            // so they are visible but inert
            factory.setExpandEntityReferences(false);

            return factory.newDocumentBuilder();
        } catch (Exception e) {
            throw new DataException("Could not create secure XML DocumentBuilder", e);
        }
    }

    /**
     * Converts a DOM {@link Element}'s direct child elements into a
     * {@code Map<String, String>}. For each child element the local tag name is
     * the key and the text content ({@link Node#getTextContent()}) is the value.
     *
     * @param element the record element to flatten
     * @return ordered map of child tag → text content
     */
    private static Map<String, String> elementToMap(final Element element) {
        Map<String, String> map = new LinkedHashMap<>();
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                map.put(child.getNodeName(), child.getTextContent().trim());
            }
        }
        return map;
    }

    /**
     * Opens a classpath resource as an {@link InputStream}.
     * Tries the context class loader first, then falls back to
     * {@link XmlDataReader}'s own class loader with a leading slash.
     *
     * @param classpath classpath-relative resource path
     * @return open stream; never {@code null}
     * @throws DataException if the resource cannot be found
     */
    private static InputStream openStream(final String classpath) {
        InputStream is = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(classpath);
        if (is == null) {
            is = XmlDataReader.class.getResourceAsStream("/" + classpath);
        }
        if (is == null) {
            throw new DataException("Classpath resource not found: '" + classpath + "'");
        }
        return is;
    }
}
