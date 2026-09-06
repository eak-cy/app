# Shared feature workflow

Use these rules for Product Owner (PO), Engineering Manager (EM), Lead, and the main conversation on both hosts. Paths are repository-relative.

## Ask the user

Every role must ask the user before implementing an uncertain choice about behavior, scope, architecture, data, dependencies, or a meaningful implementation tradeoff. Explain the choice briefly, recommend an option, and batch related questions. Do not invent the user's preference. Carry answers forward; never ask again about a settled decision unless new evidence changes it.

Use an available user-question tool. If the role cannot ask directly, return `USER_QUESTION` with the question, recommendation, and blocked work; the main conversation asks the user verbatim and resumes that role with the answer. Never bounce a user question through PO and EM. A pending answer blocks only dependent work; silence is not approval.

EM presents a brief plan, normally 3–5 bullets covering behavior, approach, edge cases, and verification. The user must explicitly agree to the plan before Lead implementation starts. Record that agreement in the handoff. Existing agreement remains valid for work within the plan; material changes to scope, behavior, or approach require renewed agreement before affected implementation. Read-only investigation and preparing the plan can continue while questions are pending. No hidden decisions about unresolved alternatives.

## Keep work economical

- Read the relevant feature doc, affected epic sections, and only triggered guides/code. Do not survey the entire product or reread files already available in context.
- PO records product requirements; EM challenges them against code, resolves approach questions with the user, and obtains plan agreement. PO owns every epic and all documentation under `pages/`; EM owns engineering documentation outside `pages/`. Each owner updates affected docs before implementation from the agreed plan and afterward from reviewed results, in the same slice/PR. EM coordinates this and confirms both sets are current before marking done. Lead owns code, tests, and its implementation report; it never updates docs.
- One short product brief, one agreed engineering handoff, one implementation run, then EM code review and documentation reconciliation. Aim for about 200 words for product and 500 for engineering; expand only for concrete risk or necessary facts.
- Share the current brief, user decisions, and exact evidence paths. Send deltas on follow-up, not transcripts or repeated full packages. Reuse role sessions when supported.
- Stop discovery when the requested outcome, acceptance, and material uncertainties are understood. Do not exhaust hypothetical edge cases or build a speculative backlog.
- Use one selected Lead. No extra agents or parallel exploration unless a concrete independent task justifies the cost. The main conversation handles all role spawning/resuming for portability.
- Prefer the smallest working change. Minor polish can remain as a disclosed follow-up. Do not trade away security, data integrity, agreed behavior, or required verification for speed.
- Once checks pass and EM has no material findings, finish. No repeated planning, review rounds for taste, or redundant tests. Never commit or push unless the user requests it.
