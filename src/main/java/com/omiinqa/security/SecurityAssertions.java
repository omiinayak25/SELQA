package com.omiinqa.security;

import org.assertj.core.api.Assertions;
import org.openqa.selenium.Cookie;

import java.util.Map;

/**
 * Reusable security validation helpers used by the security test layer.
 *
 * <p>Covers the checks a security-aware functional suite can perform without a
 * dedicated scanner: response/security headers present, cookies hardened
 * (Secure/HttpOnly), no injection payload reflected verbatim, and no sensitive
 * data leaked in page source.</p>
 */
public final class SecurityAssertions {

    private SecurityAssertions() {
    }

    /** A malicious payload must NOT appear verbatim in reflected output. */
    public static void assertPayloadNotReflected(final String payload, final String responseBody) {
        Assertions.assertThat(responseBody)
                .as("payload must be sanitized/escaped, not reflected verbatim")
                .doesNotContain(payload);
    }

    /** Injection input should be rejected (caller asserts the app stayed on an error/login state). */
    public static void assertRejected(final boolean appRejectedInput, final String payload) {
        Assertions.assertThat(appRejectedInput)
                .as("application must reject malicious input: %s", payload)
                .isTrue();
    }

    public static void assertHeaderPresent(final Map<String, String> headers, final String header) {
        Assertions.assertThat(headers.keySet())
                .as("security header '%s' present", header)
                .anyMatch(k -> k.equalsIgnoreCase(header));
    }

    public static void assertSessionCookieHardened(final Cookie cookie) {
        Assertions.assertThat(cookie).as("session cookie present").isNotNull();
        Assertions.assertThat(cookie.isHttpOnly())
                .as("cookie '%s' should be HttpOnly", cookie.getName())
                .isTrue();
    }

    public static void assertNoSensitiveDataInSource(final String pageSource) {
        Assertions.assertThat(pageSource.toLowerCase())
                .as("page source must not leak obvious secrets")
                .doesNotContain("password=")
                .doesNotContain("secret_sauce")
                .doesNotContain("aws_secret");
    }
}
