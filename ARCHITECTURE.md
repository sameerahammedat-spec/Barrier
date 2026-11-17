# Barrier architecture

## Application structure

- **UI:** Jetpack Compose screens collect lifecycle-aware state from screen-scoped ViewModels.
- **Preferences:** `BarrierPreferences` stores user and protection preferences in Preferences DataStore.
- **Domain persistence:** `BarrierDatabase` (Room v2) exposes DAOs for sessions, blocklists, statistics, and usage limits. Hilt provides repositories and workers.
- **Session lifecycle:** `SessionRepository` uses Room transactions to activate at most one eligible session. WorkManager start/expiry workers and `BootReceiver` restore scheduled expiry and attempt enforcement recovery.
- **Enforcement:** `BarrierAccessibilityService` inspects supported accessible UI; `BlockingService` separately polls UsageStats while a session is active. These paths still overlap and have not been device-validated.
- **Accounts and backup:** email/Google authentication and cloud sync explicitly report unavailable until Firebase is configured and implemented. Android automatic backup is disabled pending explicit cloud-backup consent and rules.

## Known boundaries

- Website checks depend on supported browsers exposing content to Accessibility and do not provide device-wide filtering.
- The VPN tunnel remains disabled. The image classifier is a heuristic and is not reliable protection.
- Room schema 2 is committed; no verified schema 1 or migration history is available. Do not invent a migration.
- Legacy user/token preferences may still exist locally; current auth code does not expose them as usable credentials.
