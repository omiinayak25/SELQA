# 0005 — Heuristic-first "AI" features, optional credentialed backend

- Status: Accepted
- Context: "AI" testing features are valuable but must not fabricate capability or require a
  paid LLM to function, and must never call the network silently.
- Decision: Self-healing locators, failure categorization, flaky detection and locator
  suggestion are implemented as deterministic heuristics. An `AiAssistant` hook exists; the
  default is `NoOpAiAssistant`. A credentialed HTTP backend is wired but only used when an API
  key is explicitly configured.
- Consequences: Every feature works offline and is unit-tested; AI is a genuine enhancement,
  not a dependency. Trade-off: heuristic quality < a strong LLM, which is the honest baseline.
