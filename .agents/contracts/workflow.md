# Shared workflow rules

Apply to all roles on both hosts. Paths are repository-relative. The main conversation acts as Engineering Manager (EM); do not spawn another EM for routine orchestration.

## Questions and agreement

Any role can raise a question. Ask only when an unresolved answer changes behavior, scope, approach, or acceptance. Recommend an option and batch related questions. Use existing user answers and established patterns for routine details; do not manufacture questions or re-confirm settled decisions.

EM talks directly to the user. A subagent without a user-question tool returns `USER_QUESTION` with the decision, recommendation, and blocked work; EM asks and resumes it with the answer. No PO↔EM question relay loop. Pause only dependent work; silence is not agreement.

The user must agree to the brief implementation plan before coding. Existing explicit agreement to a proposed approach counts; do not request it again in another format. A small correction needs only a sentence; larger work normally needs 3–5 bullets. Material changes require renewed agreement before affected implementation.

## Documentation

PO is accountable for product requirements and `pages/`; EM for engineering docs and final consistency. Every role may edit factual documentation within the agreed scope, including Lead. Ownership is accountability, not an exclusive write permission. Assign one writer per file at a time and preserve other edits.

Record the agreed plan once in the conversation or existing feature doc. Do not require finished epics or duplicate documentation rounds before coding. Create/link a new feature doc in the first implementation slice. The implementing agent updates affected reference docs and indexes with verified behavior in the same PR; EM reviews code and docs together. Consult PO again only for unclear product meaning. Never change an agreed requirement to excuse incorrect code.

## Cost and completion

- Read relevant feature docs, affected epic sections, and triggered guides/code only. Reuse available evidence; do not survey the product or reload full files after each handoff.
- One compact handoff, one implementation run, one EM review. Send changed facts on follow-up, not transcripts or repeated packages. Reuse sessions; use a focused context rather than full-history forks when supported.
- Use PO only for new/unclear product requirements. One Lead normally suffices; extra agents require a concrete independent task that justifies their overhead.
- Scale discovery and review to risk. Stop exploring once acceptance, the approach, and material unknowns are clear. No speculative backlog, formal requirement IDs, or task-by-task dispatch for small work.
- Verification follows AGENTS.md's change-specific rules. Reuse passing evidence for unchanged code; rerun only checks affected by new edits or unresolved failures. Do not remove application validation or security/data-integrity checks to save time.
- Give brief updates for findings, decisions, blockers, or meaningful progress; omit handoff transcripts and routine status messages. No repeated review for taste. Finish when agreed behavior, relevant checks, and docs are satisfied; disclose minor follow-ups.
- Never commit or push unless requested. Report real evidence and limitations, not invented success or token savings.
