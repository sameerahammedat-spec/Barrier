# Barrier implementation plan

## Phase 1 — Critical foundation (in progress)

- Implement persistent protection state, honest live capability status, permission-aware service start/stop, and transactional session transitions.
- Keep automatic backup disabled until authenticated, consented backup exists.
- Add converter and session policy tests.
- **Database release decision (2026-11-17):** the project owner confirms no earlier Barrier version was distributed. Room schema version 2 is the initial release baseline; no v1 migration is required. Keep schema export enabled and commit the v2 schema. Do not add destructive migration fallback. Every future released schema version must have a forward migration and preservation test.
- **Remaining Phase 1 gate:** implement safe, user-visible handling for corrupt persisted converter data; verify service recovery and permission revocation on a working Android device. Unit/build success alone does not prove Android lifecycle behavior.

## Phase 2 — Core protection

- Centralize app enforcement so Accessibility and UsageStats do not duplicate work.
- Complete and test website rule parsing and subdomain matching; communicate browser coverage limits.
- Verify session scheduling, permission revocation, boot, force-stop, and Android background execution behavior.

## Phase 3 — Accounts and cloud

- Integrate Firebase Authentication and authenticated backup after `google-services.json`, enabled providers, OAuth fingerprints, Firestore schema/rules, and privacy/retention decisions are available.
- Add consent, backup/restore, conflict handling, retry behavior, and Firebase emulator tests.

## Phase 4 — Advanced protection

- Evaluate a licensed, privacy-conscious on-device content classifier; keep heuristic image detection labeled experimental or unavailable.
- Keep VPN filtering disabled unless a complete forwarding/DNS design can be implemented and tested on Wi-Fi, mobile data, IPv4/IPv6, and network transitions.

## Phase 5 — Security and release

- Complete manifest, data, Accessibility disclosure, permissions, dependency, and Play policy review.
- Run physical-device tests, configure signing through local/CI secrets, build and verify a release AAB, and prepare privacy/data-safety/store materials.
