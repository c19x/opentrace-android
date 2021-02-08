# OpenTrace Android App powered by Herald

![alt text](./OpenTrace.png "OpenTrace Logo")
![herald](./Herald.png "Herald Logo")

This is a fork of the OpenTrace Android App integrated with Herald to provide reliable Bluetooth communication and range finding across a wide range of mobile devices.

Changes to the OpenTrace app are:

- Herald for data transport
  - Compatible with existing OpenTrace devices (detect, read, and write payload).
  - App is fully operational in background mode on locked iPhones.
  - Use new over-the-air binary encoding for OpenTrace payloads.
  - Concurrent running of Herald and OpenTrace protocols for seamless transition.
  - No change to encounter reporting, but faster and more regular capture of RSSI and payloads.
- New test mode for formal evaluation using the [Fair Efficacy Formula](https://vmware.github.io/herald/efficacy/).
  - Full offline operation without any dependency on Firebase.
  - Bypass phone registration and one-time-passcode (OTP) checks.
  - Herald instrumentation to log phone discovery, payload read, and RSSI measurements.
  - Log files are compatible with Herald analysis scripts for evaluation.
  - Fixed payload, rather than rotating payload, to enable automated analysis.

## Building the code

  1. Install the latest [Android Studio](https://developer.android.com/studio) from Google
  2. Clone the repository
  3. Open project folder `opentrace-android`
  7. Build and deploy `app` to test device

## Deployment configuration

1. Enable test mode by setting `HeraldIntegration.testMode = true` (disabled by default).
2. Disable interoperability with existing OpenTrace devices by setting. `BLESensorConfiguration.interopOpenTraceEnabled = false` (enabled by default).
3. Remove "power saver mode" from app as Herald is fully operational in background mode on locked iPhones.

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

## Review changes to OpenTrace for Herald integration

- New `Herald` group contains all the code for Herald integration.
- Search for `HeraldIntegration` in existing OpenTrace code to review changes.
- Most significant changes are found in `BluetoothMonitoringService` for switching data transport from OpenTrace to Herald.
