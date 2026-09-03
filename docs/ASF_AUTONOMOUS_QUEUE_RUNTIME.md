# ASF Autonomous Queue Runtime

The NCM factory queue is the routing layer for opted-in autonomous production tasks.

## Lifecycle

READY → exact-main lease → autonomous Code Worker → tests/security/device evidence → Draft PR → exact-head gates → Production Orchestrator → merge → observation/recovery.

## Parallelism

A queue cycle may lease up to three independent opted-in tasks. Duplicate active leases are rejected, and merge remains serial through the Production Orchestrator.

## Safety

- Exact `main` SHA is captured at lease time.
- Only explicitly opted-in tasks (`ncm-auto` or `NCM-AUTO: TRUE`) are routed.
- Blocked/escalated/manual/orchestrator-held tasks are not dispatched.
- Dispatch failures fail closed and mark the task blocked.
- No direct `main` modification is performed by the queue.
- L10 requires real end-to-end evidence.

## Evidence

Every autonomous completion must be traceable from Issue through worker output, commit, PR, CI/security/device gates, promotion, merge, and post-release observation.
