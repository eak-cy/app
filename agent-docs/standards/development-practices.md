# Development practices

Apply behavior-driven development (BDD), domain-driven design (DDD), and test-driven development (TDD) to feature implementation and bug fixes. Use the existing architecture and test tools; these practices do not require extra agents, documents, frameworks, or approval rounds.

## BDD: agree on observable behavior

- PO captures product requirements as concrete examples; EM checks them against the code and identifies material edge cases. For clear requests, EM can use the supplied examples directly.
- Describe relevant scenarios as **Given** an initial state, **When** an action occurs, **Then** the observable outcome follows. Include meaningful rejection cases and whether state or external side effects change.
- Resolve unclear outcomes with the user before implementing them. Carry the agreed examples into the existing epic/feature doc and appropriate functional or acceptance tests; do not create a duplicate specification.
- Plain English and descriptive test names are sufficient. Gherkin files, Cucumber, and Given/When/Then code comments are not required. Keep the existing epic layout and Scala test conventions.

## DDD: model the business faithfully

- Use the same business vocabulary in requirements, domain types, services, and documentation. Respect the feature boundaries documented in `agent-docs/features/`.
- Express business concepts and invariants through the repository's domain types, newtypes, validators, and service rules. Preserve identity, ownership, and lifecycle semantics rather than passing unrelated primitives or flags.
- Keep transport concerns at endpoints/mappers, business orchestration in services, and persistence mechanics in repositories. Keep domain rules independent of HTTP and storage details, using existing patterns.
- Identify which changes must be atomic and enforce their invariants at the appropriate domain, transaction, and database boundaries. Use entities, value objects, and aggregate boundaries where the business needs them; do not introduce event sourcing, CQRS, extra layers, or a domain-wide redesign merely to claim DDD compliance.

## TDD: prove the change in small steps

0. **Skeleton:** the first slice declares the interfaces, signatures, types, and wiring the agreed behavior needs, leaves every new body unimplemented, and states the expected behavior as tests. On a feature that already ships, it instead updates the docs to the agreed behavior and pins the new behavior with a failing test against the existing code, naming the existing tests the change will invalidate without touching them yet. Report the real red result and stop for approval before implementing anything.
1. **Red:** before changing production behavior, add or adapt a focused test for the next agreed behavior or reproduce the bug with an existing test. Run it and confirm it fails for the intended missing behavior, not an unrelated build, fixture, or environment problem.
2. **Green:** implement the smallest correct change that makes that test pass. Keep required validation and security protections intact.
3. **Refactor:** improve the touched code within the agreed scope while keeping relevant tests passing. Refactoring is optional when no improvement is needed.

Use the lowest test layer that proves the behavior; add integration or acceptance coverage when the changed boundary requires it. Work incrementally rather than writing the entire feature before testing. Existing regression tests can supply the red step; duplicate tests are unnecessary.

Changing an existing feature follows the same cycle as a delta: state it as "today X, after this Y", slice by kind of edit (docs, then test, then the new function, then the existing function, then data), and prove that behavior you did not mean to change still passes. Adapt an existing test only because the agreed behavior changed, and say which assertions moved and why; never delete, skip, or weaken a test to reach green.

Each red/green/refactor step is one reviewable slice of **at most 3 hand-written files**, ending with a stop for the user's approval, so changes are read like pair programming. Wide multi-file drops are prohibited; when the next behavior cannot fit, propose a split instead of widening the slice. See `.agents/contracts/workflow.md` for the stages and gates around this cycle.

Documentation/configuration-only edits and behavior-preserving mechanical changes do not need an artificial failing test. For migrations, generated contracts, or infrastructure changes, use the applicable slice's meaningful validation rather than a contrived unit test. If the environment blocks the red/green cycle, report the gap; never claim to have observed a failure or pass that was not run.

EM reviews the implementation against the agreed scenarios, domain rules, and actual test evidence. A concise report of the relevant red/green result is enough; no extra ceremony. Follow AGENTS.md for final verification, lint, and same-PR documentation updates.
