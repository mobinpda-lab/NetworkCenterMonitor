# Autonomous GitHub Software Factory — NetworkCenterMonitor
This document adapts the ARVIN-style autonomous operating protocol to this repository. The canonical product contract remains `docs/MASTER_SPEC_V1.md`; this file defines execution/governance only and must not duplicate product requirements.
## Source of truth
GitHub is authoritative for project state. Every cycle starts by reading default branch, current HEAD, open PRs, open issues, branches, recent commits, workflows, CI status, architecture, documentation, existing implementation and pending tasks.
## Required cycle
AUDIT → IDENTIFY BOTTLENECK → DECOMPOSE → QUEUE → BRANCH → IMPLEMENT → TEST → REVIEW → PR → CI → EXACT-HEAD → GUARDED MERGE → RELEASE → MONITOR → RECOVERY.
## Branch isolation
No worker writes directly to `main`. Branch naming: `feature/<id>-<name>`, `bugfix/<id>-<name>`, `test/<id>-<name>`, `docs/<id>-<name>`, `automation/<id>-<name>`.
## Workers
Code Worker: implementation/refactoring/fixes. Test Worker: unit/integration/regression/evidence. Documentation Worker: product/architecture/README/changelog. Review Worker: code/architecture/security/regression review. Release Worker: version/build/artifact/release notes/release.
## Queue states
READY → PLANNED → ASSIGNED → IN_PROGRESS → TESTING → REVIEW → READY_TO_MERGE → MERGED. Failure path: FAILED → ANALYZED → FIX_TASK → QUEUED → RETRY. Tasks are idempotent and cannot be active twice.
## Exact-head rule
Evidence is valid only for the exact PR HEAD SHA that produced it. Any new commit invalidates prior CI/build/device evidence and all required gates must be revalidated.
## Evidence-first
Never report running as success or skipped as passed. Test success requires workflow/job/SHA evidence; build success requires artifact/SHA evidence; merge requires merge commit; release requires release plus artifact.
## Auto-fix
Maximum automatic fix attempts per failure class: 3. Each attempt records failure classification, root cause, patch and retest. After three unsuccessful attempts, create/update a decision issue and escalate.
## Merge policy
Merge is allowed only when PR is open, non-draft, based on current main, conflict-free, required checks pass, build passes, applicable integration/device gate passes, no blocking review exists and exact-head is validated. `main` protection is a required governance target; automation must not bypass it.
## Human escalation
Pause only for ambiguous product decisions, security-critical/destructive operations, irreversible migrations, missing credentials, legal/business decisions, architecture conflicts or bounded auto-fix exhaustion.
## Secrets
No token, password, production IP inventory, backup file or secret may appear in source, issues, PRs or logs. Use GitHub Secrets/Environment Secrets/OIDC where needed.
## Mobile quality gates
Formatting/static analysis → unit tests → integration tests where applicable → security checks → debug build → artifact verification → device/smoke gate when device infrastructure exists.
## Automation maturity
Level 0 Manual; 1 CI; 2 Automated Tests; 3 Automated PR; 4 Automated Merge; 5 Automated Release; 6 Autonomous Workers; 7 Self-Fix; 8 Autonomous Production; 9 Continuous Recovery; 10 verified end-to-end autonomous factory. Level 10 is never claimed without execution evidence for the complete chain.
## Continue command
When the user says `ادامه`, audit GitHub first, recover the last active task, inspect workflows/PRs/SHA/failures, continue available work automatically, run auto-fix where needed, and promote only when all gates are proven.