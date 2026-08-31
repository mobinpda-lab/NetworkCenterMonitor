# Equipment Presets

Equipment Presets are executable defaults for Discovery and service suggestions. They extend the canonical Device and Discovery models; they are not a second equipment database or scanner.

## Built-in catalog
The first catalog contains operational presets for generic network devices, IP cameras, NVR/DVR recorders, Windows PCs and MikroTik routers. Presets provide bounded probe methods and concrete TCP/UDP ports such as HTTP/HTTPS, RTSP, RDP, VNC, Radmin, WinBox, SSH and SNMP where appropriate.

## Safety
Presets are hints/defaults only. They must never overwrite protected Manual or Imported Device values. Discovery results still use the canonical Auto/Manual/Imported merge policy.

## User presets
User/imported presets use the same model as built-ins and are archived rather than destructively deleted. Archived presets are excluded from generated Discovery plans.

## Backup
User/imported preset state belongs to the canonical `EQUIPMENT_PRESETS` Backup/Restore section. Persistence wiring must use the single Room database.
