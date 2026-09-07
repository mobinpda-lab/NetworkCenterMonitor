# Payesh / NetworkCenterMonitor — Canonical Completion & Execution Map

## 0. Authority and independence
- Reference HEAD for this map: `55e4ce27f069fec70d9a43fee936644238c8b1ed` on `main`.
- This document covers **Payesh/NetworkCenterMonitor product only**.
- It remains independent from Arvin, NIRA and YadNegar. Only general engineering patterns may be learned; never transfer project-specific code, business logic, roadmap, scope, issues, bugs, data, secrets, artifacts or dependencies.
- Historical PRs/branches/issues are not current product gaps until reconciled against exact `main`.

## 1. Required lifecycle
Every capability progresses through `Planned → Implemented → Executed → Verified → Proven`.
Counts of PRs, issues, workflows or commits are not completion evidence.

## 2. P0 — Product foundation / release blockers
1. **Canonical persistence** — Room/data model, migrations, DAO/repository behavior, transactions and referential integrity.
2. **Monitoring engine reliability** — probe execution, retry/backoff, timeout/error handling and deterministic state transitions.
3. **Incident lifecycle** — Suspected → Confirmed → Incident → Recovery → Close, including flapping handling and maintenance suppression where contracted.
4. **Release evidence** — exact-head test/CI/security/build/artifact/installability/provenance and release-delivery evidence.

## 3. P1 — Operational product capabilities
5. Equipment/device configuration and canonical presets.
6. Monitoring dashboard and operational status visibility.
7. Incident/history persistence and recovery semantics.
8. Backup/restore and data integrity.
9. Network/probe edge cases, degraded connectivity and restart recovery.

## 4. P1/P2 — Automation is supporting infrastructure, not product completion
10. Watchdog and Production Orchestrator health may continue independently.
11. Event-driven lease fencing must remain exact-head validated.
12. Automation must not be used as a substitute for missing product capabilities or release evidence.

## 5. P2 — Hardening
13. Security validation.
14. Reproducible build/artifact provenance.
15. Performance, accessibility and UI polish after P0/P1 functional gaps.

## 6. Execution order
- P0 persistence/monitoring/release blockers first.
- Independent operational capabilities proceed concurrently.
- Automation health proceeds independently and never blocks product development unless it is a true dependency.
- Never introduce a second persistence or monitoring source of truth merely to bypass an existing contract.

## 7. Evidence ledger
For every capability record exact HEAD, acceptance criteria, implementation source, targeted tests, CI run, security result, build/artifact, runtime/installability evidence and remaining gap. Any HEAD/PR/CI/artifact/evidence change invalidates the previous decision and requires revalidation.

## 8. Anti-forgetting rule
This document is the ordered Payesh completion checklist. Update this canonical document when verified product state changes; do not create competing status ledgers. A capability becomes `Proven` only after its complete acceptance/evidence chain is demonstrated.
