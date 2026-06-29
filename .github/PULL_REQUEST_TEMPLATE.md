<!-- Thanks for contributing to OmiinQA! Keep PRs focused and small. -->

## Summary
<!-- What does this PR change and why? -->

## Type of change
- [ ] 🐛 Bug fix
- [ ] ✨ Feature
- [ ] ♻️ Refactor
- [ ] 🧪 Tests
- [ ] 📝 Docs
- [ ] 🔧 Build / CI

## Affected layer(s)
<!-- UI / API / Database / BDD / Reporting / Driver / Observability / Cloud / Docs -->

## Checklist
- [ ] `mvn -B clean test-compile` passes
- [ ] `mvn -B test -Dsuite.file=testng-smoke.xml` (offline core) is green
- [ ] `mvn -B test -Dsuite.file=testng-db-embedded.xml` (if DB touched) is green
- [ ] `mvn -B -P quality verify -DskipTests` (Checkstyle/PMD/SpotBugs) is clean
- [ ] Page objects contain **no assertions**; no `Thread.sleep`
- [ ] New public types have Javadoc (role + pattern + WHY)
- [ ] New browser/auth/data-source added as a **Strategy**, not a `switch` edit
- [ ] Tests tagged with the correct `@Test(groups = …)`
- [ ] Docs updated (TEST_CATALOG / traceability / relevant guide)
- [ ] No secrets committed; no new High/Critical CVEs introduced

## How was this verified?
<!-- Commands run + results. Paste the relevant "Tests run: …" lines. -->

## Linked issues
Closes #
