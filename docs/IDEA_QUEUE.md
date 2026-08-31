# Canonical Idea Queue Policy

This document defines how future product suggestions are handled without interrupting active production.

## Source of truth
- GitHub is the official project state.
- `docs/MASTER_SPEC_V1.md` v1.1 remains the canonical product contract.
- Conversation text alone is never enough to decide that an idea is new, missing, or ready to implement.

## Intake
Every new user suggestion that is not an immediate blocker or an explicit replacement of the Master Spec is recorded as a GitHub issue using:
- title prefix: `[IDEA]`
- body marker: `IDEA-QUEUE: TRUE`
- target area when known
- dependencies when known

The arrival of a new idea must not interrupt active production work.

## Reconciliation
At every verified feature/area completion and during Production Orchestrator reconciliation cycles, relevant queued ideas are compared against:
- Master Spec v1.1
- current `main`
- current validated development/integration head
- existing code
- open/closed Issues and PRs
- branches and documentation
- architecture boundaries
- Backup/Restore coverage

Each idea is classified as one of:
1. `DUPLICATE` — link to the existing implementation/task and do not rebuild.
2. `COMPATIBLE_NOW` — convert the same idea issue into an executable task with acceptance criteria and dependency links.
3. `COMPATIBLE_LATER` — keep queued and record the blocking dependency/reason.
4. `DECISION_REQUIRED` — if it conflicts with the Master Spec, architecture, security, legal constraints, or an irreversible product decision, wait for human input.

## Architecture guard
Idea reconciliation must never create a second or parallel:
- Device identity/model
- Discovery engine
- Monitoring engine
- Incident engine
- Backup/Restore path
- Settings store
- Report pipeline

New specialized functionality must extend the canonical shared core through Profile/Adapter boundaries.

## Completion rule
A queued idea is not considered implemented merely because code or a YAML file exists. Completion requires the applicable combination of:
- implementation
- automated tests
- documentation
- executable build evidence
- integration/device evidence when applicable
- security checks
- Exact-Head validation
- Backup/Restore impact coverage

## Speed rule
Use Maximum Parallel only for independent work. Dependency checks and duplicate checks happen before work starts. The goal is to finish the shared core faster, not to create more disconnected output.

Related governance: GitHub Issue #38.
