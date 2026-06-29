package com.omiinqa.api.multipart;

import com.omiinqa.config.FrameworkConfig;
import io.restassured.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LIVE integration tests for {@link MultipartRequestBuilder} against
 * {@code https://httpbin.org/post}, which echoes the uploaded fields and
 * files back in JSON.
 *
 * <p><strong>Live test — requires network.</strong>  Groups: {@code "api"},
 * {@code "regression"}.  Excludes from offline suites.</p>
 */
public class MultipartUploadTest {

    private static final Logger LOG = LoggerFactory.getLogger(MultipartUploadTest.class);

    private String httpbinBase() {
        return FrameworkConfig.get().apiUrl("httpbin");
    }

    @Test(groups = {"api", "regression"},
          description = "[LIVE] Multipart upload with a plain field echoes the field back")
    public void uploadPlainFieldIsEchoedBack() {
        final Response response = new MultipartRequestBuilder()
                .endpoint(httpbinBase() + "/post")
                .field("greeting", "hello-omiinqa")
                .post();

        LOG.info("Multipart field upload status: {}", response.statusCode());
        assertThat(response.statusCode()).isEqualTo(200);

        final String body = response.body().asString();
        assertThat(body).contains("hello-omiinqa");
    }

    @Test(groups = {"api", "regression"},
          description = "[LIVE] Multipart upload with in-memory byte part echoes file content")
    public void uploadBytePartIsEchoedBack() {
        final byte[] content = "OmiinQA-frame-test-payload-2025".getBytes();

        final Response response = new MultipartRequestBuilder()
                .endpoint(httpbinBase() + "/post")
                .bytePart("report", content, "text/plain", "report.txt")
                .post();

        LOG.info("Multipart byte-part upload status: {}", response.statusCode());
        assertThat(response.statusCode()).isEqualTo(200);

        final String body = response.body().asString();
        assertThat(body).contains("OmiinQA-frame-test-payload-2025");
    }

    @Test(groups = {"api", "regression"},
          description = "[LIVE] Multipart upload with mixed field and byte parts — all parts echoed")
    public void uploadMixedPartsAllEchoedBack() {
        final byte[] csvContent = "id,name\n1,Alice\n2,Bob".getBytes();

        final Response response = new MultipartRequestBuilder()
                .endpoint(httpbinBase() + "/post")
                .field("uploader", "regression-suite")
                .field("version", "1.0")
                .bytePart("dataset", csvContent, "text/csv", "users.csv")
                .post();

        LOG.info("Mixed multipart upload status: {}", response.statusCode());
        assertThat(response.statusCode()).isEqualTo(200);

        final String body = response.body().asString();
        assertThat(body).contains("regression-suite");
        assertThat(body).contains("users.csv");
    }
}
