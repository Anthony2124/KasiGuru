# Focused lint cleanup — 2026-10-01

Addressed the four warning categories selected after the emulator review.

| Area | Change |
|---|---|
| Tour input | `SpotlightOverlay` consumes complete gestures through `awaitEachGesture`, keeping event handling inside its pointer scope. Background interactions remain blocked while caption controls work. |
| Image metadata | `ImageCompressor` uses AndroidX ExifInterface 1.4.2 instead of the framework implementation. Existing rotation, compression and size limits remain the contract. |
| Backup configuration | The manifest now references legacy `full-backup-content` rules for Android 8–11 and `data-extraction-rules` for Android 12+. The existing file had the legacy root despite being referenced as the newer format. Both formats exclude shared preferences and databases. |
| Number formatting | Literature attachment sizes explicitly use the device's locale. |

The [AndroidX release documentation](https://developer.android.com/jetpack/androidx/releases/exifinterface)
lists 1.4.2 as stable. The backup formats follow
[Android's Auto Backup documentation](https://developer.android.com/identity/data/autobackup).
Espresso was updated from 3.6.1 to 3.7.0 for device tests: the old test-only
dependency called an input API removed on this emulator. The
[Espresso release notes](https://developer.android.com/jetpack/androidx/releases/test#espresso-3.7.0)
document its replacement with a system-service lookup.

## Verification

`testDebugUnitTest assembleDebug lintDebug assembleDebugAndroidTest`, repository
structure, web token and whitespace checks passed. Unit reports contain 312
passing tests across 45 suites. The four targeted warning IDs are absent; lint
dropped from 95 to 86 warnings, with zero errors. Eight warnings were removed by
the functional cleanup and one dependency notice by the Espresso update.
The remaining dependency notices,
artwork/resource warnings and unrelated conventions were outside this change.

Six focused instrumented tests cover JPEG orientation and corner order,
metadata-free PNGs, compression limits, invalid images, both backup XML formats,
and repeated tour taps/drags with working caption controls.

All six focused device tests passed on `kasi_test` (API 37) through the Android
instrumentation runner. The final log ends with `OK (6 tests)` and is saved at
[warning-fixes-device-tests.txt](../../../emulator-review/warning-fixes-device-tests.txt).
Initial runs were stopped by the emulator's low-memory killer. After restarting
with 4 GB of RAM, the first completed run identified a test parser setup issue
and the old Espresso input API incompatibility. Both were corrected before the
successful final run. The earlier Gradle device report contains those failed
attempts; the final ADB log records the passing results for the updated APKs.

Testing used airplane mode with Wi-Fi disabled and a read-only AVD overlay.
The emulator was closed afterward to discard that overlay and preserve the
original AVD's app data. No production Firebase writes or release publishing
were involved.

The backup tests verify packaged rules; they do not perform a real cloud backup
or device transfer. Android 8–11 backup transport behavior and online rankings,
public profiles and cloud sync remain unverified by this cleanup.
