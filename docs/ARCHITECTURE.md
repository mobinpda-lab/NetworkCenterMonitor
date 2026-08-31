# Architecture Baseline

## Source of Truth
- Canonical product contract: `docs/MASTER_SPEC_V1.md` (MASTER SPEC v1.1)
- GitHub Issues/PRs are the source of truth for implementation status and execution evidence.
- Stable architecture belongs in this document; volatile progress percentages must not be duplicated here.

## Platform
- Android: Kotlin + Jetpack Compose
- Local database: Room
- Async/state: Coroutines + Flow
- Active monitoring: Foreground Service
- Architecture: Local-first, Server-ready

## Canonical hierarchy
`Group/Province -> Center -> Network/VLAN/Range -> Device/IP -> Service/Port -> Incident -> Follow-up`

Camera hierarchy:
`Province -> Center -> Camera Network -> Recorder (NVR/DVR) -> Camera`

PC hierarchy:
`Province -> Center -> Network -> PC/Workstation -> Services/Remote Profiles`

## One shared core, specialized profiles
The application must not create parallel Camera, PC or Agent data silos.

- `Device` is the canonical equipment identity.
- A Device may own multiple interfaces, IPv4 addresses and MAC addresses.
- Device relations cover Parent/Child, Connected-To, Managed-By, Recorded-By, Connected-Via and Network-Parent.
- Camera = `Device` + Camera Profile.
- NVR/DVR = `Device` + Recorder Profile.
- PC/Workstation = `Device` + PC Profile.
- Specialized profiles must reuse the canonical `DeviceId` and must not create a second Device identity.

## Discovery
- Exactly one shared Discovery Engine is used by network, Camera and PC discovery.
- Discovery supports CIDR and From-To ranges, preview/load estimation, bounded concurrency, rate limiting, timeout/retry and pause/resume/stop.
- Probe adapters may cover ARP, ICMP, TCP, UDP, HTTP(S), RTSP, ONVIF, hostname, MAC vendor, service fingerprint, vendor APIs, authenticated inventory and Local Agent.
- Camera Scanner and PC Scanner must be adapters/profiles over the shared engine, never parallel engines.
- Discovery values are sourced as `Auto`; manual/imported values are protected from silent overwrite. Conflicts produce an update proposal for user confirmation.

## Monitoring and incidents
- Probe scheduler: bounded concurrency + Queue/Batch + Timeout/Retry/Backoff.
- Monitoring settings inheritance: Global -> Group -> Center -> Network -> Device/IP -> Service.
- Ping/ICMP and Port Check remain independent.
- All Device, Camera, PC and Remote failures use the single canonical incident lifecycle: Failure -> Suspected -> Retry -> Confirmed -> Incident -> Recovery -> Close.

## Camera / Recorder architecture
- Vendor-neutral first: ONVIF Profile T, legacy S, G, M, plus RTSP and HTTP(S) fallbacks.
- Vendor adapters are extension points, not separate data models.
- Bandwidth policy: Health Check is low-cost and continuous; Snapshot is on-demand/limited; Live is on-demand only.
- Live defaults to Sub Stream. Main Stream requires explicit user request. Streams stop immediately when the view closes and concurrent streams are bounded per center.

## PC / LAN Remote architecture
- PC inventory extends the canonical Device model.
- Multiple Remote Profiles may exist per Device/IP.
- Remote methods include RDP, Radmin, RustDesk Direct/LAN, VNC, TeamViewer LAN Mode and Custom.
- Default Port, Custom Port and Effective Port are distinct; Effective Port is the one used for health check, connection test and launch.
- Credentials are referenced through secure storage or Ask-on-Connect; plaintext remote passwords are forbidden.
- Remote is LAN/private-network first; public Internet is not a core dependency.

## Local Scanner Agent
- Optional bridge for centers behind APN/NAT where direct routing is unavailable.
- Uses canonical Center/Network/Device identifiers.
- May perform LAN discovery, ONVIF discovery, Device health, PC inventory and remote-software detection.
- Transfers metadata/status only with bounded retry/backoff and limited offline buffering.
- Must not create an independent database, Device identity, Incident engine or persistent live stream.

## Backup / Restore
Full Backup/Restore must serialize the complete canonical state, including networks/VLANs, Devices, interfaces, relations, discovery settings/results needed by the product, Camera/Recorder profiles, PC inventory, Remote Profiles/custom ports, manual overrides, Agent configuration, incidents/history/follow-ups, settings, custom fields and future attachments.

No newly introduced persistent entity is considered complete until Backup/Restore coverage is defined and tested.

## Reports and UI invariants
- Reports must display Jalali date/time and use Vazirmatn UI FD with embedded font.
- UI remains Persian RTL and simple at the surface while preserving deep technical detail in drill-down screens.
- Network state colors remain canonical: green=connected, red=disconnected, with amber/gray only for warning/unknown/disabled states as defined by the product contract.

## Documentation Gate
Documentation is part of Definition of Done.
For every material code change, review and update as applicable:
1. `docs/MASTER_SPEC_V1.md` only when the product contract changes.
2. `docs/ARCHITECTURE.md` when architecture/data-flow changes.
3. `README.md` when repository-level capabilities or onboarding information changes.
4. The related Issue/PR with scope, test/build evidence and current exact HEAD.

A feature must not be marked complete if its implementation and its GitHub documentation disagree.

## Production automation architecture
- `Android CI` is the quality/unit/lint/build gate and must validate the exact PR head.
- `Android Device Smoke` is the executable gate: build APK, install on an emulator, launch the real app and retain runtime evidence.
- `Android Security` is an independent security gate for promotable PR heads.
- `NCM Production Orchestrator` is the only GitHub-native automated production-promotion authority.
- The Orchestrator wakes on gate completion, manual dispatch and a five-minute fallback schedule.
- Only explicitly opted-in PRs are eligible (`ncm-auto` label or `NCM-AUTO: TRUE` marker).
- Every required gate must be completed successfully on the exact current PR head and current `main` base; running, failed, cancelled or skipped evidence is not success.
- `main`, PR head, draft state and mergeability are re-read immediately before promotion.
- Production merges are serialized: at most one merge per Orchestrator invocation, forcing remaining work to revalidate against the new `main`.
- Failed/timed-out gates create an idempotent recovery queue issue; recovery work must produce new exact-head evidence before promotion.
- No automation may force-push, rewrite history, bypass a broken gate, create a second production merge authority or store credentials in repository content/logs.

## Non-parallel architecture rule
Avoid parallel Settings, Font, Backup, Monitoring, Incident, Report, Discovery, Device, Camera or PC controllers/data stores. Each concern has one canonical path and specialized behavior is implemented through profiles/adapters/extensions.