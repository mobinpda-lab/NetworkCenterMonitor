# ASF Control Plane Foundation

Machine-readable contract for queue, lease, worker identity, lifecycle state and evidence. GitHub Actions remains the execution fabric while the factory contract becomes independent of any single workflow.

Workers are issue-scoped and idempotent. Leases prevent duplicate active execution. Self-fix is bounded to three attempts. Production Orchestrator is the only merge authority. L10 is not claimed without E2E evidence.
