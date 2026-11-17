# Engineering changelog

## 2026-11-17 — Phase 1 foundation update

- Replaced fake sign-in success with explicit unconfigured-provider errors; local guest mode remains.
- Made Protection Center status derive from persistent preferences, active session rules, and live Accessibility service state.
- Hardened session activation/cancellation with Room transactions; improved worker expiry, conflict deferral, boot restoration, Usage Access gating, and visible startup errors.
- Added version-prefixed JSON list conversion with legacy delimiter reads and tests; malformed break-settings data now fails explicitly rather than silently resetting.
- Disabled Android automatic backup and excluded application data pending user-consented authenticated backup.
- Corrected Device Admin messaging and documented unsupported protection capabilities.
- Verification: `compileDebugKotlin` passed; `clean testDebugUnitTest assembleDebug` completed successfully with 31 unit tests and a debug APK. Instrumented device behavior, release bundle, and schema migration remain unverified.
