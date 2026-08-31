# Canonical Persistence

`NcmDatabase` is the single Room database for persistent application state. Specialized Camera/Recorder/PC/Remote/Agent data extends the canonical Device identity and must not create a second Device store.

## Schema v1
The first schema persists Center, Network, Device, Device Interface/IP, IP Endpoint, Service, Device relations, sourced Auto/Manual/Imported fields, tags, custom fields, Camera/Recorder/PC/Remote profiles, Local Agent metadata, Incident, Follow-up and Monitoring state.

Room schema export is enabled. Any future persistent entity must be added to `CanonicalPersistenceTables` and the full Backup/Restore coverage registry in the same change.

## Device aggregate transaction
`CanonicalDao.replaceDeviceAggregate` replaces one canonical Device aggregate transactionally, including interfaces, interface IPs, tags, custom fields and sourced metadata. Manual/imported source metadata remains explicit and therefore cannot be silently replaced by discovery data at the persistence boundary.

## Migration rule
Database version changes require an explicit migration and schema evidence. Destructive migration is not a production fallback.

## Backup rule
The persistent schema and Backup/Restore share one coverage contract. A feature that stores new durable state is incomplete until its backup coverage is defined and tested.
