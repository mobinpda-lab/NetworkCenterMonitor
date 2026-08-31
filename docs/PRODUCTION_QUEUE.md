# Production Queue
Canonical product scope: `docs/MASTER_SPEC_V1.md`. Execution rules: `docs/AUTONOMOUS_FACTORY.md`.
## Active queue
| Task | Issue | Branch | Worker | State | Dependency | Current evidence |
|---|---:|---|---|---|---|---|
| BOOTSTRAP-001 | #1 | `feature/1-android-foundation` | Code + Test | TESTING | none | PR #5 head `b33f088abb79e50b86979d0e2c51e4b27117b37c`; exact-head Run #9 in progress |
| CI-002 | #2 | `test/2-android-ci` → integrated into `feature/1-android-foundation` | Test + Review | INTEGRATED / AWAITING MAIN | #1 foundation | PR #7 merged as `b33f088abb79e50b86979d0e2c51e4b27117b37c`; Run `33396936292` passed unit tests, lint, debug APK build and artifact upload on exact head `b3095ed9e6b31b7648ad4353c29311e49a872040` |
| GOV-003 | #3 | `docs/3-autonomous-factory` | Docs + Review | IN_PROGRESS | none | PR #6; operating protocol + queue committed |
| SEC-004 | #4 | admin configuration | Review | HUMAN_ACTION_REQUIRED | #2 for required checks | repository still public; `main` still unprotected; blocks autonomous promotion to main |
| DOMAIN-005 | #8 | `feature/8-domain-model` | Code + Test | TESTING | stacked on #1/#2 | PR #9 head `0a312a9a943f4399f998042e93d804f423ebba92`; exact-head Run #8 in progress |
## Next product queues after bootstrap
Duplicate-prevention is required before creation: persistence/Room; monitoring engine; outage incidents; follow-ups; dashboard/navigation; search/filter/sort; backup/restore; reports/PDF/print/share; security/login; presets/custom fields; integration/device smoke.
## Audit trail fields
Every completed task records Task ID, Issue, Branch, Worker, start/end timestamps when available, Commit SHA, PR, tests, build, device/integration evidence, review, merge SHA and release.
## Queue rules
One task may have only one active implementation branch. Dependent tasks stay blocked until prerequisite evidence exists. Any HEAD change returns evidence-bound states to TESTING. Failed work enters FAILED → ANALYZED → FIX_TASK → QUEUED → RETRY with at most 3 automatic fix attempts per failure class. `main` promotion is blocked while #4 remains unresolved.