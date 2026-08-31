# ADR-001: Central Server + Local Agent Architecture

## Status
Proposed as next architecture baseline for NetworkCenterMonitor.

## Decision
NetworkCenterMonitor will use a Central Server + Local Agent + Multi Platform Client architecture.

## Goals
- Central management of all authorized centers.
- Local discovery and monitoring inside each center.
- Support APN/NAT environments without requiring inbound LAN access from the central site.
- Allow multiple network administrators to view and manage authorized information.

## Components

### Central Server
Responsible for:
- Canonical database
- Users and permissions
- Reports
- Incident management
- History and audit
- Backup and restore

### Local Agent
Installed in a center network and responsible for:
- LAN discovery
- Device health checks
- Camera/NVR discovery
- PC inventory collection
- Sending metadata and status to central server

Agent scope:
- Only its assigned center/network.
- No independent organizational database.
- No permanent live streaming.

### Clients
Supported clients:
- Windows
- Android Tablet
- Future Web Dashboard

## Security Rules
- Agent identity is based on Instance ID, not IP address.
- Pairing requires approval.
- Sensitive operations require secure authentication.
- Passwords are never stored as plain text.

## Development Rules
- No parallel data models.
- All features use the canonical Device model.
- Idea -> Design -> Development -> Test -> Release.
