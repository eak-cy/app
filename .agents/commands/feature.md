---
description: Quick product brief → engineering handoff → implementation → EM review
argument-hint: <feature description>
---

Handle "$ARGUMENTS" using `.agents/contracts/workflow.md`. This is the shared workflow for Claude Code and Codex. The main conversation orchestrates; roles do not spawn each other. If native agents are unavailable, perform the same roles sequentially in the main conversation and disclose that fallback.

1. Ask `product-owner` to capture feature requirements from the product perspective in `PRODUCT_BRIEF`, including scope, business rules, and acceptance. Resolve missing product intent with the user; no finished epic prerequisite.
2. Ask `engineering-manager` to compare those requirements against relevant code/docs, identify issues and edge cases, and propose a brief approach. EM raises approach questions with the user.
3. Relay any role's `USER_QUESTION` directly to the user, then send the answer to the affected role and update the shared decisions. Consult PO again only for missing product context. Do not create a PO↔EM question loop.
4. Show EM's brief plan and obtain explicit user agreement before implementation. Then have EM update engineering docs outside `pages/` and resume PO to update affected epics and other `pages/` documentation from the agreed scope and approach, clearly marking pending behavior. Coordinate matching indexes; pass only the relevant decisions and changed facts. Require `ENGINEERING_HANDOFF` with requirements, user decisions and plan agreement, approach, concrete edge cases, code/doc paths, acceptance/checks, and LOW/MEDIUM/HIGH Lead selection.
5. Spawn the selected `lead-engineer-low`, `lead-engineer-medium`, or `lead-engineer-high` once with the handoff and relevant user answers. Authorize code, tests, and checks within the agreed plan. Lead never updates docs; PO owns `pages/` documentation and EM owns engineering docs outside it. Do not request another formal plan or drive it one task at a time. Relay Lead↔EM technical questions and new user uncertainties as needed, sending only deltas.
6. Send the Lead's `IMPLEMENTATION_REPORT` and diff scope to the same EM for actual code review and documentation reconciliation, per delivered slice/PR. EM updates engineering docs using the Lead's output and verified code. Relay those findings to the same PO for affected `pages/` updates, then return PO's changed paths to EM for consistency review. EM returns `DONE`, `CHANGES_REQUESTED`, or `BLOCKED`. Send material findings to the same Lead; return fixes and affected check evidence to EM. Require renewed user agreement for material plan changes before affected implementation. Stop once findings are resolved and EM has reconciled documentation.
7. Report delivered behavior, verification, and disclosed limitations. Only mark done after EM accepts the result. Failed or unavailable required checks remain explicit blockers, not a success claim.

Task tracking is optional for larger work; use the host's available tools, never require Claude-specific task APIs. Reuse sessions when supported; otherwise send a compact current handoff. Do not forward full conversation histories.
