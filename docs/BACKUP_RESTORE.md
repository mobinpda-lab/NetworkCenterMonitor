# Full Backup / Restore

NetworkCenterMonitor uses one versioned backup package for canonical state. The backup contract is intentionally broader than the current persistence schema so new persistent areas cannot silently fall outside Backup/Restore.

## Package
- ZIP-based package with explicit manifest and one payload per canonical `BackupSection`
- package version, application version, schema version, backup ID, canonical timestamp, Jalali display timestamp and entity counts
- missing or duplicate canonical sections fail validation
- newer unsupported schema/package versions fail closed
- Jalali-safe backup filename generation

## Security
Backup archives are encrypted with AES-GCM. The encryption key is supplied by secure platform storage; it is never embedded in the backup or repository. Credential sections contain secure references only, never plaintext passwords/secrets.

## Restore safety
`FullRestoreCoordinator` always creates an encrypted emergency backup of the current canonical state before applying the requested restore. The concrete persistence adapter must apply restore transactionally so a failed restore does not leave partial state.

## Coverage gate
Every new durable entity must update both the persistence registry and canonical backup coverage in the same implementation cycle. Backup/Restore is part of Definition of Done, not a later export feature.

`BackupSection.CUSTOM_FIELDS` is the single canonical section for both custom-field definitions and custom-field values. Schema v2 adds `custom_field_definitions`; its definition metadata and all `custom_field_values` must be serialized/restored together in this section. No second custom-field export path is permitted.

Database integration is completed against the canonical Room schema from DATA-033; no second database or backup path is allowed.