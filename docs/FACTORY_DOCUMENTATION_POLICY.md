# Immutable Factory Law — Documentation Policy

**Status:** IMMUTABLE FACTORY LAW  
**Scope:** All autonomous development, worker, CI/CD, release and recovery activity in this repository.

## Rule
Documentation is **event-driven**, not continuous overhead.

- Normal, small, non-semantic implementation changes require no separate documentation task.
- A **meaningful change** MUST update the relevant documentation in the same task/PR.
- Meaningful changes include architecture, public contracts/APIs, data models or migrations, security, CI/CD or Factory behavior, release/signing, recovery/rollback, operational procedures, and externally visible behavior.
- Tests, commits, PR descriptions and CI evidence remain the primary evidence for ordinary changes.
- Release/milestone/E2E events require a concise status snapshot when they materially change the operational state.
- Important architectural or operational decisions MUST be recorded in the applicable decision log.
- Documentation work MUST NOT create a permanent worker or recurring bottleneck; it runs with the owning task/PR whenever possible.

## Enforcement
This law is part of the Factory contract. Automation MUST fail closed if a meaningful change is identified and its required documentation update/evidence is missing. No workflow, worker or optimization may weaken, bypass, silently remove, or reinterpret this rule.

## Priority
Speed is optimized by reducing unnecessary documentation, **not by omitting documentation for meaningful changes**.
