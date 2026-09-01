# Epic #10 — Core Implementation Guide

This document records the stable implementation boundaries for **#10 Device Discovery, Camera Monitoring, PC Inventory & LAN Remote Access**. Product requirements remain canonical in `docs/MASTER_SPEC_V1.md` (v1.1). This file documents how the current codebase implements those requirements without creating parallel architecture.

## Canonical hierarchy

`Province / Group -> Center -> Network / VLAN / Range -> Device / IP -> Service / Port -> Incident -> Follow-up`

Specialized equipment extends the canonical Device identity:

- Camera = `Device` + `CameraProfile`
- NVR/DVR = `Device` + `RecorderProfile`
- PC/Workstation = `Device` + `PcProfile`
- Local Scanner Agent = transport/collection bridge into canonical Device/Discovery state; it does not own a parallel Device database or Incident engine.

## Shared Device identity

The codebase has one canonical `DeviceId`. Devices may have multiple interfaces, IP addresses and MAC addresses. Device relations represent parent/child, connected-to and recorder/channel relationships. Specialized profiles reference `DeviceId`; they never create a second identity for the same physical device.

## Data-source precedence

Discoverable fields use `Auto`, `Manual` or `Imported` source semantics. Auto discovery may refresh an existing Auto value. It must not silently overwrite Manual or Imported values. A conflicting discovery result is represented as a proposed update requiring user confirmation. `Last Discovery` and `Last Update` are retained where relevant.

## Shared Discovery Engine

There is one vendor-neutral discovery path for Router, Switch, Camera, Recorder, PC and other devices. Discovery supports CIDR and From-To scopes, configurable speed, timeout, retry, rate limits and bounded concurrency. Probe adapters extend the shared engine for ARP, ICMP, TCP, UDP, HTTP, HTTPS, RTSP, ONVIF, hostname, MAC vendor, service fingerprint, vendor APIs, authenticated inventory and Local Agent facts.

Current executable low-cost network adapters include OS reachability/ICMP, bounded explicit TCP connect probes, and HTTP/HTTPS HEAD probes. HTTP/HTTPS discovery honors explicit preset/user ports or falls back to 80/443, does not follow redirects, and treats valid authentication/error HTTP responses as positive service detection because the remote HTTP stack answered. HTTPS keeps platform certificate validation enabled rather than weakening trust checks for discovery.

Discovery results are preview candidates first. Duplicate detection occurs before import. Import is explicit and selective.

## Camera / Recorder profile boundary

Camera and Recorder code stores specialized metadata only. It reuses canonical monitoring, incident, history, follow-up, reporting and backup paths. ONVIF capabilities include Profile T and legacy S/G/M compatibility representation. RTSP/HTTP(S), codec, resolution, FPS, bitrate, streams, PTZ, audio, recording and recorder/channel relationships belong to the profile layer.

### Bandwidth policy

- Health checks may run continuously and must be low bandwidth.
- Snapshot is on-demand or explicitly scheduled at a limited cadence.
- Live view is user initiated only.
- Sub Stream is the default live path.
- Main Stream requires explicit user choice.
- Leaving live view stops the stream immediately.
- Per-center concurrent stream limits and Low/Normal/High bandwidth profiles are mandatory integration constraints.

## PC / Remote profile boundary

`PcProfile` stores sourced inventory metadata while reusing the canonical Device. Remote access supports multiple profiles per device/IP. Initial methods are RDP, Radmin, RustDesk Direct/LAN, VNC, TeamViewer LAN Mode and Custom.

Each remote profile keeps Default Port, optional Custom Port and derived Effective Port. Effective Port is the value used for service health checks, connection tests and launch. Manual override wins over discovery.

Remote credentials must not be stored as plaintext. Profiles reference secure credential storage or use Ask-on-Connect. Public Internet is not a core dependency; LAN/private network is the primary operating mode.

## Local Scanner Agent boundary

The Agent is optional and intended for centers behind NAT/APN or where the central application cannot directly route into the LAN. It may perform LAN discovery, ONVIF discovery, camera discovery, device health, PC inventory and remote software detection.

The Agent transmits compact metadata/status and references canonical Center, Network and Device identities. It must not create an independent Device database, Incident engine, reporting system or permanent video stream.

## Incident integration

All device types use the same lifecycle:

`Failure -> Suspected -> Retry -> Confirmed -> Incident -> Recovery -> Close`

Camera, PC, remote services and Agent-observed health never create parallel Incident engines.

## Backup / Restore gate

Every new persisted entity introduced by Epic #10 must be added to full Backup/Restore before that persistence work is considered complete. Coverage includes Networks/VLANs, Devices, Interfaces, Relations, Discovery profiles/results needed for restoration, Camera/Recorder profiles, channels, PC inventory, Remote profiles and custom ports, manual overrides, Agent configuration, vendor adapter settings, history, incidents and relevant settings.

No Epic #10 data may remain outside the canonical backup package.

## Documentation gate

A material change is not complete until the following agree:

1. Product requirement in `MASTER_SPEC_V1.md` when product behavior changes.
2. Architecture documentation when boundaries or ownership change.
3. Code and tests.
4. GitHub Issue/PR evidence and exact-head CI results.
5. Backup/Restore and reporting documentation when persisted/output data changes.

Volatile execution status belongs in GitHub Issues and PRs, not in stable architecture files.

## Current integration strategy

Independent tracks are implemented and tested on isolated branches, then combined on `integration/10-epic-core`. Individual green CI is necessary but not sufficient. The combined integration head must pass unit tests, lint, APK build and artifact generation before promotion to the Foundation branch. Promotion to `main` remains subject to the repository production gates and current canonical Master Spec.
