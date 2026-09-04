# ASF Runtime Control Plane

Issue #84 is the operational control-plane record for NCM factory execution.

## Runtime contract

The queue and workers emit machine-readable lifecycle events as immutable issue comments on the control-plane issue. Each event is wrapped by an `asf-event <EVENT>` marker and contains JSON with `schema_version`, `event`, `ts`, `actor`, and event-specific fields.

Supported events in this increment:

- `QUEUE_RECONCILED` — current main SHA, eligible work and dispatch capacity.
- `TASK_LEASED` — issue, exact main SHA and worker slot.
- `WORKER_DISPATCHED` — issue and exact main SHA.
- `WORKER_STARTED` — issue, run ID and leased main SHA.
- `WORKER_COMPLETED` — successful worker handoff to guarded PR/promotion flow.
- `WORKER_FAILED` — failed worker run with bounded attempt number.
- `LEASE_RECOVERED` — stale lease returned to the queue.
- `DISPATCH_FAILED` — fail-closed dispatch failure.

## Recovery policy

A lease is considered stale after 1800 seconds without a fresh lifecycle completion signal. Stale `factory:leased` and `factory:in-progress` labels are removed and the issue is returned to the queue. Worker failures are bounded to three attempts; after the bound is exhausted the issue receives `factory:escalated` and remains fail-closed.

## Safety invariants

- `main` is never modified directly by the control-plane workflows.
- Every worker is leased against an exact `main` SHA.
- Existing Production Orchestrator remains the sole promotion/merge authority.
- Control-plane events are append-only issue evidence; they do not grant merge authority.
- No L10 claim is valid until a real end-to-end completion, recovery and release cycle is evidenced.

This file documents only the runtime increment in this branch; it does not claim that the entire ASF-MOC is L10-complete.
