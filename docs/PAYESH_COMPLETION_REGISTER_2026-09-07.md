# NetworkCenterMonitor / Payesh — Permanent Completion Register

Date: 2026-09-07
Baseline HEAD: `55e4ce27f069fec70d9a43fee936644238c8b1ed`
Canonical product contract: `docs/MASTER_SPEC_V1.md` v1.1

## Project boundary
This register belongs ONLY to NetworkCenterMonitor/Payesh. Arvin-clean, YadNegar, Arvin Factory and NIRA/ASF are independent projects/factories. No project-specific code, business logic, storage, data, roadmap, issue or dependency may be copied between them. General engineering patterns may be independently adapted after project-specific audit.

## P0 — Core product and factory proof
- [ ] Reconcile current implementation against Master Spec, architecture, persistence, monitoring, backup and UI contracts.
- [ ] Prove the complete autonomous worker chain on current main: issue → intake/queue → lease → worker → fencing → exact-head → branch/PR → CI → security → build → artifact → provenance → promotion.
- [ ] Prove Recovery and Idempotency with independently reproducible evidence.
- [ ] Verify event-driven lease fencing from current main, including stale/duplicate worker rejection and exact-main protection.
- [ ] Validate monitoring/discovery/incident lifecycle end-to-end on real current code.
- [ ] Validate persistence/canonical schema and migration/recovery behavior.
- [ ] Validate Backup/Restore end-to-end, including integrity and recovery.

## P1 — Canonical product coverage
- [ ] Province/group/center/site hierarchy.
- [ ] Networks/VLAN/ranges and bounded scanning/load protection.
- [ ] Canonical Device model and Device Profiles: Camera, NVR/DVR, PC/Workstation and other equipment.
- [ ] IP/service/port/Ping monitoring with independent controls and hierarchical timing overrides.
- [ ] Discovery and Device Identification.
- [ ] Camera/NVR/DVR monitoring.
- [ ] PC inventory and LAN Remote Access profiles.
- [ ] Incident detection, lifecycle, history and follow-up.
- [ ] Custom Fields with safe disable/archive semantics.
- [ ] Reports/PDF/Print/Share using one report pipeline.
- [ ] Settings using one canonical settings store.
- [ ] UI simplicity/technical depth contract and current style guide.

## P2 — Operational/release readiness
- [ ] Security and dependency/license checks.
- [ ] Android build/installability and device evidence where applicable.
- [ ] Monitoring/alerting/incident response and rollback procedures.
- [ ] Artifact provenance and reproducible release evidence.
- [ ] Central backend/multi-user/Web Dashboard readiness only where explicitly in the approved scope; do not implement speculative infrastructure prematurely.

## Idea queue
`docs/IDEA_QUEUE.md` is canonical for future ideas. Each idea must be reconciled against Master Spec v1.1, current main, code, issues/PRs, architecture and Backup/Restore. Classification: DUPLICATE / COMPATIBLE_NOW / COMPATIBLE_LATER / DECISION_REQUIRED. Ideas never create parallel Device, Discovery, Monitoring, Incident, Backup, Settings or Report systems.

## Permanent anti-forgetting rule
Before Payesh is declared complete, run this register against current main and every canonical document. Every applicable requirement must be proven complete, explicitly deferred with reason, or represented by an active blocker. Historical issue/PR/branch evidence is not current completion evidence unless reproduced or still applicable.
