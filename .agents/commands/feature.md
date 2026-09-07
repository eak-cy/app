---
description: EM-led clarification → agreed plan → implementation → review
argument-hint: <feature description>
---

Handle "$ARGUMENTS". The main conversation acts as EM: read `.agents/agents/engineering-manager.md` (ignore YAML) and follow `.agents/contracts/workflow.md`. Do not spawn an EM just to relay messages.

1. For new or unclear product requirements, ask `product-owner` once for a compact `PRODUCT_BRIEF`. For an explicit correction or already-defined story, use the user's requirements directly. Resume PO only if product meaning remains unclear.
2. Inspect relevant code, resolve material questions with the user, and agree on a brief plan. Apply `.agents/contracts/complexity.md`. Reuse prior explicit agreement; no second plan approval or mandatory pre-implementation docs round.
3. Send one `ENGINEERING_HANDOFF` to the selected `lead-engineer-low`, `lead-engineer-medium`, or `lead-engineer-high`, authorizing code, relevant checks, and factual docs together. Do not request another formal plan. If agents are unavailable, implement in the main conversation and disclose that the review is a self-review.
4. Answer Lead questions directly or ask the user when a preference is needed. Send only the resolution and changed scope. Let the Lead complete the agreed work without per-task dispatch.
5. Review the actual code/doc diff and `IMPLEMENTATION_REPORT`. Fix trivial factual docs directly; return concrete code findings to the same Lead. Recheck affected changes only. Consult PO only for product ambiguity, not routine documentation approval.
6. Report delivered behavior, checks, and limitations. Mark done when relevant verification, agreed scope, and docs are satisfied. Required checks that could not run remain explicit verification gaps.

Use task tracking only when it helps larger work. No mandatory status tokens or host-specific task APIs. The EM custom-agent profile remains available for an explicitly requested separate review, not a required stage.
