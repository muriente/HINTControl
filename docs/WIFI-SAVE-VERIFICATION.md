# Wi-Fi save verification

HINT now distinguishes a completed write from an unconfirmed radio setting. After a successful write it reads the gateway configuration and compares every submitted, non-null radio state for 2.4, 5, and 6 GHz. A missing or different reported state opens a warning identifying the affected bands. Failed writes and unavailable readback retain pending settings. Available readback replaces the submitted draft with gateway-reported settings; edits made after submission remain pending. Save is disabled while a save is in progress.

This is configuration verification, not a measurement of radio transmissions. Hidden SSIDs can still broadcast. Gateway firmware may ignore a setting or report a state that disagrees with actual RF behavior. HINT does not claim radio shutdown from an HTTP success response.

The existing configuration/v2 endpoint and serialized radio field names are unchanged. This patch does not claim a firmware workaround, automatically reboot a gateway, or hide networks as a substitute for disabling radios. Other Wi-Fi settings are reloaded but are not independently compared by this radio verification check.

## Validation

Run `./gradlew :common:desktopTest`. Tests cover explicit false serialization, accepted writes with ignored or missing radio settings, matching readback, failed writes, absent readback, optional 6 GHz, successful HTTP retries, and cancellation cleanup.

On macOS, use an installed non-Homebrew JDK 21 for `./gradlew :desktop:createDistributable`; Compose's packaging task rejects Homebrew JDKs. The resulting app is under `desktop/build/compose/binaries/main/app`.

For live acceptance, use a wired management connection and retain a known recovery route. Record pre-save configuration and broadcasts, save a requested radio change, inspect the fresh readback and any warning, refresh again, and independently observe broadcasts over time. A single scan or a matching switch does not establish durable shutdown. Restore the agreed original settings after a test unless the owner explicitly wants the changed state retained.

## Upstream evidence

- [Issue 94](https://github.com/zacharee/HINTControl/issues/94) describes historical radio readback and broadcast disagreements. Its older firmware reports do not establish the cause on a different firmware version.
- [Issue 132](https://github.com/zacharee/HINTControl/issues/132) concerns retained channel bandwidth settings.
- [Issue 133](https://github.com/zacharee/HINTControl/issues/133) concerns band steering retention.

No upstream issue or pull request was submitted as part of this fork repair.
