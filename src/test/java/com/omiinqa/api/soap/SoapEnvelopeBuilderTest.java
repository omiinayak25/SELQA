package com.omiinqa.api.soap;

import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Offline unit tests for {@link SoapEnvelopeBuilder}.
 *
 * <p>All tests validate the structure of the generated XML without any network
 * calls.  They use groups {@code "api"} and {@code "unit"} so CI pipelines can
 * select or exclude them independently of live-endpoint tests.</p>
 */
public class SoapEnvelopeBuilderTest {

    // -----------------------------------------------------------------------
    //  SOAP 1.1 envelope tests
    // -----------------------------------------------------------------------

    @Test(groups = {"api", "unit"},
          description = "SOAP 1.1 envelope has correct namespace URI on the root element")
    public void soap11EnvelopeHasCorrectNamespace() {
        final String xml = new SoapEnvelopeBuilder()
                .version(SoapEnvelopeBuilder.SoapVersion.SOAP_11)
                .bodyElement("<dummy/>")
                .build();

        assertThat(xml)
                .contains("http://schemas.xmlsoap.org/soap/envelope/")
                .contains("soapenv:Envelope")
                .contains("soapenv:Body");
    }

    @Test(groups = {"api", "unit"},
          description = "SOAP 1.2 envelope has correct namespace URI on the root element")
    public void soap12EnvelopeHasCorrectNamespace() {
        final String xml = new SoapEnvelopeBuilder()
                .version(SoapEnvelopeBuilder.SoapVersion.SOAP_12)
                .bodyElement("<dummy/>")
                .build();

        assertThat(xml)
                .contains("http://www.w3.org/2003/05/soap-envelope")
                .contains("soap:Envelope")
                .contains("soap:Body");
    }

    @Test(groups = {"api", "unit"},
          description = "Default version is SOAP 1.1")
    public void defaultVersionIsSoap11() {
        final String xml = new SoapEnvelopeBuilder()
                .bodyElement("<noop/>")
                .build();

        // SOAP 1.1 uses the soapenv prefix
        assertThat(xml).contains("soapenv:Envelope");
        assertThat(xml).doesNotContain("soap:Envelope");
    }

    @Test(groups = {"api", "unit"},
          description = "Body element content is embedded inside the Body element")
    public void bodyElementIsEmbeddedInBody() {
        final String bodyContent = "<tem:Add><tem:a>3</tem:a><tem:b>4</tem:b></tem:Add>";
        final String xml = new SoapEnvelopeBuilder()
                .version(SoapEnvelopeBuilder.SoapVersion.SOAP_11)
                .bodyElement(bodyContent)
                .build();

        assertThat(xml)
                .contains("<soapenv:Body>")
                .contains(bodyContent)
                .contains("</soapenv:Body>");
    }

    @Test(groups = {"api", "unit"},
          description = "Extra namespace declarations appear on the root Envelope element")
    public void extraNamespacesAppearedOnEnvelope() {
        final String xml = new SoapEnvelopeBuilder()
                .namespace("tem", "http://tempuri.org/")
                .namespace("wsse", "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd")
                .bodyElement("<tem:Ping/>")
                .build();

        assertThat(xml)
                .contains("xmlns:tem=\"http://tempuri.org/\"")
                .contains("xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\"");
    }

    @Test(groups = {"api", "unit"},
          description = "Header block is emitted inside the Header element when at least one is added")
    public void headerBlockIsEmittedWhenPresent() {
        final String securityBlock = "<wsse:Security><wsse:UsernameToken><wsse:Username>test</wsse:Username></wsse:UsernameToken></wsse:Security>";
        final String xml = new SoapEnvelopeBuilder()
                .headerBlock(securityBlock)
                .bodyElement("<noop/>")
                .build();

        assertThat(xml)
                .contains("soapenv:Header")
                .contains(securityBlock);
    }

    @Test(groups = {"api", "unit"},
          description = "Header element is omitted entirely when no header blocks are added")
    public void headerElementOmittedWhenNoHeaderBlocks() {
        final String xml = new SoapEnvelopeBuilder()
                .bodyElement("<noop/>")
                .build();

        assertThat(xml).doesNotContain("Header");
    }

    @Test(groups = {"api", "unit"},
          description = "Multiple header blocks are all present inside the single Header element")
    public void multipleHeaderBlocksAreAllPresent() {
        final String xml = new SoapEnvelopeBuilder()
                .headerBlock("<block1/>")
                .headerBlock("<block2/>")
                .bodyElement("<body/>")
                .build();

        assertThat(xml)
                .contains("<block1/>")
                .contains("<block2/>");
    }

    @Test(groups = {"api", "unit"},
          description = "contentType() returns text/xml for SOAP 1.1")
    public void contentTypeSoap11IsTextXml() {
        final SoapEnvelopeBuilder builder = new SoapEnvelopeBuilder()
                .version(SoapEnvelopeBuilder.SoapVersion.SOAP_11);

        assertThat(builder.contentType()).isEqualTo("text/xml; charset=utf-8");
    }

    @Test(groups = {"api", "unit"},
          description = "contentType() returns application/soap+xml for SOAP 1.2")
    public void contentTypeSoap12IsApplicationSoapXml() {
        final SoapEnvelopeBuilder builder = new SoapEnvelopeBuilder()
                .version(SoapEnvelopeBuilder.SoapVersion.SOAP_12);

        assertThat(builder.contentType()).isEqualTo("application/soap+xml; charset=utf-8");
    }

    @Test(groups = {"api", "unit"},
          description = "Envelope element opens and closes correctly (well-formed XML structure)")
    public void envelopeIsWellFormed() {
        final String xml = new SoapEnvelopeBuilder()
                .namespace("tem", "http://tempuri.org/")
                .bodyElement("<tem:HelloWorld/>")
                .build();

        // Outer element must open and close
        assertThat(xml).startsWith("<soapenv:Envelope");
        assertThat(xml).endsWith("</soapenv:Envelope>");
    }
}
