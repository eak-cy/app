# Complexity contract

EM selects one Lead from concrete implementation risk, not document length or a hypothetical worst case. Classify quickly; reassess only when new evidence materially changes the risk.

- `LOW`: bounded, reversible, established-pattern work with limited impact; can include a small behavior correction with clear expected results and existing coverage.
- `MEDIUM`: contained feature across layers, new external client/config/test infrastructure, schema or compatibility work, or meaningful auth/transaction/integration edge cases.
- `HIGH`: security architecture, destructive data migration, financial/audit correctness, difficult concurrency/distributed consistency, or broad breaking changes.

Process scales too: LOW uses a sentence-sized plan and focused checks; MEDIUM uses a brief plan and the affected feature layers; HIGH adds explicit risk/rollback reasoning and deeper relevant review. Skip PO whenever product requirements are already clear, regardless of tier.

Use the highest material trigger present. A copied external client still needs config, dependency wiring, credentials, and test provisioning, so remains MEDIUM. A changed business outcome is assessed by its actual risk; it does not automatically require an expensive tier. Resolve uncertain user intent with the user instead of treating ambiguity as a reason for unlimited analysis.
