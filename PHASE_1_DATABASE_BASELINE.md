# Phase 1 database release-baseline decision

**Decision date:** 2026-11-17  
**Application:** Barrier (com.rushgames.barrier)  
**Room schema currently configured:** version 2

## Owner confirmation

The project owner confirms that no earlier version of Barrier was distributed or installed by external users. Barrier is still in development.

## Decision

- Treat Room schema version 2 as the initial production-release database baseline.
- No migration from a released v1 database is required because no v1 release exists.
- Do not fabricate a v1 schema or add a speculative migration.
- Do not use destructive migration fallback in production.
- Keep Room schema export enabled and include the generated version-2 schema in source control before the first release.
- Once a schema version is released to users, every later schema change must include a forward migration and a migration test that verifies existing user data is preserved.

## Development data

Existing local development databases are not evidence of a released schema. Developers may clear app data or reinstall during development only when that local data is disposable. Back up any development data that must be retained before doing so.

## Remaining Phase 1 work

This decision closes only the historical v1 migration question. It does not complete:
- Corrupt persisted-value recovery and a clear user-facing repair path.
- Service recovery and permission-revocation tests on a working Android device.
- Process-death, reboot, and foreground-service behavior verification.

## Release gate

Before the first production release, verify that the generated Room version-2 schema is committed. For every subsequent release that changes the schema, add and run migration tests against the immediately previous released schema.
