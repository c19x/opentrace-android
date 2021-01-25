# OpenTrace Android App with HERALD instrumentation

![alt text](./OpenTrace.png "OpenTrace Logo")

This is a fork of the OpenTrace Android App with HERALD instrumentation to enable evaluation using the [Fair Efficacy Formula](https://vmware.github.io/herald/efficacy/).

Changes to the OpenTrace app are:

- Full offline operation without any dependency on Firebase
- Bypass phone registration and one-time-passcode (OTP) checks
- HERALD instrumentation to log phone discovery, payload read, and RSSI measurements
- Log files are compatible with HERALD analysis scripts for evaluation
- Fixed payload, rather than rotating payload, to enable automated analysis

## Building the code

1. Install the latest [Android Studio](https://developer.android.com/studio) from Google
2. Clone the repository
3. Open project folder `opentrace-android`
7. Build and deploy `app` to test device

## App configuration

1. Start app
2. Select `Allow` for `Location permission` on first use
3. Select `Allow` for `Storage permission` on first use
4. On `Setup is incomplete` screen, press `Complete app setup`
5. On `Set up app permissions` screen, press `Proceed`
6. Select `Allow` for `Background permission`
7. On `App permissions are fully set up` screen, press `Continue`
8. Initial setup is complete, app is ready for test

## Testing

Please refer to [herald-for-android](https://github.com/vmware/herald-for-android) for details on test procedure and log file access.

HERALD instrumentation uses fixed device specific payload by default to enable analysis. This can be disabled by setting `FairEfficacyInstrumentation.testMode = false`.
