# Engineering changelog

## 2026-11-17 — Phase 1 foundation update

- Replaced fake sign-in success with explicit unconfigured-provider errors; local guest mode remains.
- Made Protection Center status derive from persistent preferences, active session rules, and live Accessibility service state.
- Hardened session activation/cancellation with Room transactions; improved worker expiry, conflict deferral, boot restoration, Usage Access gating, and visible startup errors.
- Added version-prefixed JSON list conversion with legacy delimiter reads and tests; malformed break-settings data now fails explicitly rather than silently resetting.
- Disabled Android automatic backup and excluded application data pending user-consented authenticated backup.
- Corrected Device Admin messaging and documented unsupported protection capabilities.
- Verification: `compileDebugKotlin` passed; `clean testDebugUnitTest assembleDebug` completed successfully with 31 unit tests and a debug APK. Instrumented device behavior, release bundle, and schema migration remain unverified.


## 2026-11-20 — Phase 1 recovery and permission hardening (branch patch)

- Aligned Kotlin/KSP/Serialization and all Room artifacts to Kotlin 2.1.21, KSP 2.1.21-2.0.2, and Room 2.7.2 to match the locally verified dependency combination.
- Made malformed Room list and break-settings values recover to safe read defaults without rewriting their original stored text.
- Added a recovery signal persisted in DataStore and surfaced in Settings and Protection Center. Session-specific enforcement is paused when an active session contains malformed converted data; global adult URL checks remain separate.
- Rechecked Usage Access while BlockingService runs and stop the monitor after revocation; Accessibility permission status now uses Android's enabled-service registry.
- Added unit coverage for converter recovery, recovery signal lifecycle, and honest Protection Center status.
- **Verification pending:** this patch has not yet been compiled locally. Physical-device tests for permission revocation, service recovery, process death and reboot remain mandatory before Phase 1 can be declared complete.
