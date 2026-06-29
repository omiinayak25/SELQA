# 0003 — Convergence enforced in the `quality` profile, not the default build

- Status: Accepted
- Context: REST Assured's legacy json-tools tree and axe-core drag in conflicting transitive
  versions. Strict `dependencyConvergence` on every build would demand dozens of pins for no
  runtime benefit and slow day-to-day work.
- Decision: Keep always-on enforcement for Java/Maven version and duplicate dependencies; move
  `dependencyConvergence` into the opt-in `quality` profile (CI).
- Consequences: Fast, green local builds; convergence still enforced before merge. Trade-off:
  a convergence regression is caught at CI, not at `mvn validate`.
