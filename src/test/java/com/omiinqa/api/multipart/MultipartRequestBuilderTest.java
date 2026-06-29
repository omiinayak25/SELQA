package com.omiinqa.api.multipart;

import org.testng.annotations.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Offline unit tests for {@link MultipartRequestBuilder} builder state.
 *
 * <p>Tests verify the fluent API's state accumulation without executing HTTP
 * requests.  Groups: {@code "api"}, {@code "unit"}.</p>
 */
public class MultipartRequestBuilderTest {

    @Test(groups = {"api", "unit"},
          description = "New builder has zero parts")
    public void newBuilderHasZeroParts() {
        final MultipartRequestBuilder builder = new MultipartRequestBuilder();
        assertThat(builder.partCount()).isEqualTo(0);
    }

    @Test(groups = {"api", "unit"},
          description = "Field parts increment the part count")
    public void fieldPartsIncrementPartCount() {
        final MultipartRequestBuilder builder = new MultipartRequestBuilder()
                .endpoint("https://example.com/upload")
                .field("name", "Alice")
                .field("role", "tester");

        assertThat(builder.partCount()).isEqualTo(2);
    }

    @Test(groups = {"api", "unit"},
          description = "hasPart returns true for a registered field name")
    public void hasPartTrueForRegisteredField() {
        final MultipartRequestBuilder builder = new MultipartRequestBuilder()
                .endpoint("https://example.com/upload")
                .field("description", "unit test upload");

        assertThat(builder.hasPart("description")).isTrue();
    }

    @Test(groups = {"api", "unit"},
          description = "hasPart returns false for an unregistered name")
    public void hasPartFalseForUnregisteredName() {
        final MultipartRequestBuilder builder = new MultipartRequestBuilder()
                .endpoint("https://example.com/upload")
                .field("known", "value");

        assertThat(builder.hasPart("unknown")).isFalse();
    }

    @Test(groups = {"api", "unit"},
          description = "Byte parts increment the part count and are discoverable via hasPart")
    public void bytePartsIncrementPartCountAndAreDiscoverable() {
        final byte[] content = "test-content".getBytes();
        final MultipartRequestBuilder builder = new MultipartRequestBuilder()
                .endpoint("https://example.com/upload")
                .bytePart("payload", content, "application/octet-stream", "data.bin");

        assertThat(builder.partCount()).isEqualTo(1);
        assertThat(builder.hasPart("payload")).isTrue();
    }

    @Test(groups = {"api", "unit"},
          description = "File part is discoverable via hasPart even if file does not exist on disk")
    public void filePartIsDiscoverable() {
        // We test builder state only — no actual file I/O
        final File phantomFile = new File("/tmp/phantom.txt");
        final MultipartRequestBuilder builder = new MultipartRequestBuilder()
                .endpoint("https://example.com/upload")
                .filePart("attachment", phantomFile, "text/plain");

        assertThat(builder.hasPart("attachment")).isTrue();
        assertThat(builder.partCount()).isEqualTo(1);
    }

    @Test(groups = {"api", "unit"},
          description = "Mixed field and file parts are all counted")
    public void mixedPartsAreAllCounted() {
        final MultipartRequestBuilder builder = new MultipartRequestBuilder()
                .endpoint("https://example.com/upload")
                .field("category", "documents")
                .field("priority", "high")
                .bytePart("doc", "hello".getBytes(), "text/plain", "hello.txt");

        assertThat(builder.partCount()).isEqualTo(3);
        assertThat(builder.hasPart("category")).isTrue();
        assertThat(builder.hasPart("priority")).isTrue();
        assertThat(builder.hasPart("doc")).isTrue();
    }

    @Test(groups = {"api", "unit"},
          description = "getEndpoint returns the configured URL")
    public void getEndpointReturnsConfiguredUrl() {
        final String url = "https://httpbin.org/post";
        final MultipartRequestBuilder builder = new MultipartRequestBuilder()
                .endpoint(url);

        assertThat(builder.getEndpoint()).isEqualTo(url);
    }

    @Test(groups = {"api", "unit"},
          description = "Calling post() without endpoint throws ApiException")
    public void postWithoutEndpointThrowsException() {
        final MultipartRequestBuilder builder = new MultipartRequestBuilder()
                .field("key", "value");

        assertThatThrownBy(builder::post)
                .isInstanceOf(com.omiinqa.exceptions.ApiException.class)
                .hasMessageContaining("endpoint");
    }
}
