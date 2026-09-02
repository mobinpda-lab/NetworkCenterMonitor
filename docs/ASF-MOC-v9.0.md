# ASF-MOC v9.0 — Autonomous Software Factory Execution Contract

This repository adopts ASF-MOC v9 as its factory operating contract. GitHub remains the source of truth.

## Execution

`DISCOVER → ANALYZE → RESEARCH → PRIORITIZE → PLAN → ARCHITECT → BUILD → TEST → SECURE → REVIEW → MERGE → RELEASE → MONITOR → OPTIMIZE → REPEAT`

## Rules

- Inspect before change and preserve existing assets.
- Never write directly to `main`.
- Use isolated branch → commit → PR → quality gates → exact-head promotion.
- Run independent work in parallel and avoid duplicate tasks.
- Never manufacture work merely to keep workers busy.
- RC is a checkpoint, not an endpoint.
- Automate routine technical decisions; require approval for configured high-risk actions.
- Never trade security, correctness or reliability for speed.

## Queue

Executable work must have a stable identity, project, source, type, priority, dependencies, owner, status, risk, expected output, validation, retry count, blocker, evidence and next action.

States:
`INCOMING → PRIORITY → READY → RUNNING → VALIDATING → COMPLETED`

Recovery:
`FAILED → RECOVERY → READY`

Unsafe/permanent failure:
`FAILED → BLOCKED/DEAD_LETTER`

## Worker Contract

Workers are specialized execution roles and never independent merge authorities. Every result must be traceable to the originating task and candidate head, be idempotent, fail closed on unsafe scope, and return evidence.

## Evidence

Promotion evidence must match the exact candidate SHA and cover tests, security, build and required product validation. Skipped, stale, failed or mismatched evidence is not PASS.

## Recovery

`Detect → Classify → RootCause → Patch → Test → Revalidate`

Automatic repair is bounded to three attempts unless a stricter repository policy applies.

## L10

L10 is proven only by repeated operational evidence of:

`Queue → Event/Watchdog → Worker → Code → Test → Security → Build → PR → Exact-Head Gate → Merge → Release → Monitor → Recovery → Resume`

Documentation alone cannot establish L10.
