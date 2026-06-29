package com.omiinqa.security;

import java.util.List;

/**
 * Curated SQL-injection and XSS payload datasets for negative security probes.
 *
 * <p>These exercise input handling on the systems under test (login forms, API
 * fields) to confirm the application <em>rejects or neutralizes</em> malicious
 * input rather than reflecting or executing it. For authorized, educational
 * targets only (the demo apps). Not an offensive toolkit — every payload is a
 * well-known public test string.</p>
 */
public final class SecurityPayloads {

    private SecurityPayloads() {
    }

    /** Classic SQL-injection probes for auth/search fields. */
    public static final List<String> SQL_INJECTION = List.of(
            "' OR '1'='1",
            "' OR 1=1 --",
            "admin' --",
            "' OR '1'='1' /*",
            "\"; DROP TABLE users; --",
            "' UNION SELECT NULL --",
            "1' AND '1'='2",
            "') OR ('1'='1");

    /** Reflected/stored XSS probes. */
    public static final List<String> XSS = List.of(
            "<script>alert('xss')</script>",
            "\"><script>alert(1)</script>",
            "<img src=x onerror=alert(1)>",
            "javascript:alert(document.cookie)",
            "<svg/onload=alert(1)>",
            "'\"><svg onload=alert(1)>",
            "<body onload=alert('xss')>");

    /** Path-traversal probes. */
    public static final List<String> PATH_TRAVERSAL = List.of(
            "../../../../etc/passwd",
            "..\\..\\..\\windows\\win.ini",
            "%2e%2e%2f%2e%2e%2f");

    /** Security response headers a hardened app should set. */
    public static final List<String> RECOMMENDED_SECURITY_HEADERS = List.of(
            "X-Content-Type-Options",
            "X-Frame-Options",
            "Content-Security-Policy",
            "Strict-Transport-Security",
            "Referrer-Policy");
}
