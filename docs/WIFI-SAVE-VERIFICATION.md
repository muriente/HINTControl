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

## macOS input and access checks

Use JetBrains Runtime 21 for the desktop application's runtime. In the local repair acceptance run, Microsoft OpenJDK 21.0.12.1 rendered the login screen and responded to accessible switch actions, but automated text entry did not reach the password field. Replacing only the bundled runtime with JBR 21.0.11 restored the editable accessibility field and successful gateway login. This establishes a compatibility difference for the tested build and automation path; it does not establish a general Microsoft JDK defect.

When packaging with a different JDK, `jpackage --runtime-image` supports supplying a prepared runtime image. Keep the application launcher, bundle identifier, and signing identity stable during a runtime comparison, then verify the signature and actual app login. Distinct QA applications must not share a main executable UUID with the installed app; Apple documents that this can confuse local-network identity tracking.

Check text entry and actual request initiation before diagnosing local-network permission. A prior permission screenshot does not establish a currently pending prompt. Do not repeatedly ask for a dialog that the user cannot see, reset privacy state, or create another app identity to resolve an unverified permission hypothesis.

References: [JetBrains Runtime](https://github.com/JetBrains/JetBrainsRuntime), [jpackage runtime images](https://docs.oracle.com/en/java/javase/21/docs/specs/man/jpackage.html), [Apple app executable UUID guidance](https://developer.apple.com/documentation/technotes/tn3178-checking-for-and-resolving-build-uuid-problems).
