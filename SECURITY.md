# Security Policy

## Scope
OmiinQA is a **test-automation framework**. It ships no production service, but it
handles credentials for the systems under test and pulls third-party dependencies, so it
applies the same hygiene a production codebase would.

## Secrets handling
- **Never commit credentials.** `config.properties` ships with empty passwords.
- Provide secrets via environment / `-Dkey=value` / an untracked
  `src/test/resources/config/secrets.properties` (git-ignored).
- DB passwords, API tokens and Grid credentials are read at runtime, never logged. The
  logging layer must not emit raw auth headers or passwords.

## Dependency security
- **OWASP Dependency-Check** runs in the `security` profile
  (`mvn -P security verify`); builds fail on CVSS ≥ 8.
- Suppressions require a written justification and review date
  (`config/owasp/suppressions.xml`).
- **Maven Enforcer** pins the JDK/Maven baseline and bans duplicate dependency versions.

## Security testing the SUT
The `security` test layer exercises authentication, authorization, session/cookie
validation, security headers, and basic SQL-injection / XSS payload probes against the
demo applications — for **authorized, educational** targets only (SauceDemo, public APIs).

## Reporting a vulnerability
Open a private security advisory on the GitHub repository, or email the maintainer. Please
do not file public issues for undisclosed vulnerabilities. Include affected version,
impact, and reproduction steps; expect an acknowledgement within a few business days.
