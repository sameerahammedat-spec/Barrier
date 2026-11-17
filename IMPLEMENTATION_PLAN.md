# Barrier implementation plan

## Phase 1 — Critical foundation (in progress)

- Implement persistent protection state, honest live capability status, permission-aware service start/stop, and transactional session transitions.
- Keep automatic backup disabled until authenticated, consented backup exists.
- Add converter and session policy tests.
- **Gate:** verify whether a v1 database shipped and obtain its schema before writing a migration; test migration and service recovery on a working device.

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
