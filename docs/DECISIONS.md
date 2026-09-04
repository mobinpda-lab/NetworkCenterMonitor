# NetworkCenterMonitor Decision Memory

## Core Device Model
All equipment types use one shared Device Core.

## Extension Model
Specialized capabilities are implemented through Profiles and Adapters.

## Data Ownership
Automatic, manual and imported values must keep source information.
Manual values must not be overwritten without approval.

## Architecture Rule
Do not create parallel databases, scanners, monitoring engines or incident engines.

## Development Rule
Important changes require implementation, testing and documentation alignment.
