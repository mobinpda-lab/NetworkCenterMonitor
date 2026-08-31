# Canonical Persistence

`NcmDatabase` is the single Room database for persistent application state. Specialized Camera/Recorder/PC/Remote/Agent data extends the canonical Device identity and must not create a second Device store.

## Schema v2
Schema v2 adds durable `custom_field_definitions` while retaining the existing `custom_field_values` table. Definitions carry the user-configurable title, data type, ordering, Required/Enabled flags, description and SELECT options required by MASTER_SPEC v1.1. Values continue to attach to canonical owners; no second custom-field store exists.

The v1 → v2 migration is explicit and non-destructive. Existing custom-field values remain untouched. Definitions may be archived by setting `enabled=false`; hard delete is allowed only when no stored value references the definition key.

The underlying schema also persists Center, Network, Device, Device Interface/IP, IP Endpoint, Service, Device relations, sourced Auto/Manual/Imported fields, tags, Camera/Recorder/PC/Remote profiles, Local Agent metadata, Incident, Follow-up and Monitoring state.

Room schema export is enabled. Any future persistent entity must be registered with the canonical persistence/backup coverage contract in the same change.

## Device aggregate transaction
`CanonicalDao.replaceDeviceAggregate` replaces one canonical Device aggregate transactionally, including interfaces, interface IPs, tags, custom field values and sourced metadata. Manual/imported source metadata remains explicit and therefore cannot be silently replaced by discovery data at the persistence boundary.

## Custom-field lifecycle
`CanonicalDao` provides one definition path: upsert, ordered read, enabled-only read, archive, usage count and guarded hard delete. This implements the Master Spec rule that an unused field may be removed while a field with historical values must be disabled/archived instead of destructively deleting its history.

## Migration rule
Database version changes require an explicit migration and schema evidence. Destructive migration is not a production fallback.

## Backup rule
The persistent schema and Backup/Restore share one coverage contract. A feature that stores new durable state is incomplete until its backup coverage is defined and tested.