# Monitoring & Incident Engine

`docs/MASTER_SPEC_V1.md` v1.1 remains the product contract. This document records the implemented technical behavior of the shared monitoring state machine.

## One shared lifecycle
Ping, TCP/UDP/service health, Camera health and Remote health must feed the same monitoring/incident state machine. Specialized modules must not create their own outage or Incident engines.

Implemented lifecycle:

`Probe failure -> SUSPECTED -> confirmation window -> DISCONNECTED + IncidentOpened -> confirmed recovery -> CONNECTED + IncidentRecovered`

A single failed probe does not create an Incident unless the effective outage confirmation duration is zero. Repeated failures while already disconnected reuse the same active Incident.

## Independent targets
Ping and each monitored Service are separate `MonitoringTarget` values. A reachable IP can therefore remain connected while an individual service is disconnected.

## Policy inheritance
`MonitoringPolicy` is resolved by applying overrides from broadest to most specific scope. This supports the canonical hierarchy Global -> Group -> Center -> Network -> Device/IP -> Service without a parallel settings system.

## Maintenance
A failure observed during an active `MaintenanceWindow` is recorded as a maintenance observation and does not open a normal outage Incident. Monitoring may continue while normal outage classification/alerting remains suppressed.

## Recovery confirmation
An active Incident is not closed on the first transient success when recovery confirmation is configured. Successful probes must span the configured recovery confirmation duration. The same Incident ID is then closed and its duration is calculated from its original suspected/outage start.

## Flapping
Confirmed connectivity transitions are retained within a bounded rolling time window. When the configured transition threshold is reached, a single `FlappingDetected` event is emitted. The flag clears after old transitions expire, avoiding repeated alert storms.

## Persistence boundary
`MonitoringStateRepository` is the only persistence boundary used by `PersistentMonitoringProcessor`. The Room track implements this boundary. Persistence and notifications consume state/events; they must not duplicate the state machine.

## Test gate
Unit tests cover suspected outage, one-Incident semantics, confirmed recovery, Maintenance suppression, independent Ping/Service state, hierarchy override order, flapping detection and flapping clearing. Exact-head Android CI is required before integration.