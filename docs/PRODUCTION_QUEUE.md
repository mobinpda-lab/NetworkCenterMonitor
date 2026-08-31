# Production Queue
Canonical product scope: `docs/MASTER_SPEC_V1.md`. Execution rules: `docs/AUTONOMOUS_FACTORY.md`.
## Active queue
| Task | Issue | Branch | Worker | State | Dependency | Current evidence |
|---|---:|---|---|---|---|---|
| BOOTSTRAP-001 | #1 | `feature/1-android-foundation` | Code + Test | IN_PROGRESS | none | commit `687aafcaf0e9837eb2d17eb922d4198b4ee80cd8`; PR #5; CI pending |
| CI-002 | #2 | `test/2-android-ci` | Test + Review | PLANNED | #1 foundation | no workflow run yet |
| GOV-003 | #3 | `docs/3-autonomous-factory` | Docs + Review | IN_PROGRESS | none | operating protocol committed |
| SEC-004 | #4 | admin configuration | Review | HUMAN_ACTION_REQUIRED | #2 for required checks | repository public; main not protected |
## Next product queues after bootstrap
These are not created until duplicate-prevention audit confirms no equivalent implementation/issue exists: canonical data model; persistence/Room; monitoring engine; outage incidents; follow-ups; dashboard/navigation; search/filter/sort; backup/restore; reports/PDF/print/share; security/login; presets/custom fields; integration/device smoke.
## Audit trail fields
Every completed task records Task ID, Issue, Branch, Worker, start/end timestamps when available, Commit SHA, PR, tests, build, device/integration evidence, review, merge SHA and release.
## Queue rules
One task may have only one active implementation branch. Dependent tasks stay blocked until prerequisite evidence exists. Any HEAD change returns evidence-bound states to TESTING. Failed work enters FAILED → ANALYZED → FIX_TASK → QUEUED → RETRY with at most 3 automatic fix attempts per failure class.