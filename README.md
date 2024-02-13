# UID2 Android SDK

A framework for integrating [UID2](https://github.com/IABTechLab/uid2docs) into Android applications.


[![License: Apache](https://img.shields.io/badge/License-Apache-green.svg)](https://www.apache.org/licenses/)

## Integration Guides

For the latest instructions on how to use the SDK and plugins in this repository, refer to the following:

| Component        | Guide                                                                                                        |
|------------------|--------------------------------------------------------------------------------------------------------------|
| UID2 Android SDK | [UID2 SDK for Android Reference Guide](https://unifiedid.com/docs/sdks/uid2-sdk-ref-android)                 |
| UID2 Google Mobile Ads (GMA) Plugin | [UID2 GMA Plugin for Android Integration Guide](https://unifiedid.com/docs/guides/mobile-plugin-gma-android) |
| UID2 Interactive Media Ads (IMA) Plugin | [UID2 IMA Plugin for Android Integration Guide](https://unifiedid.com/docs/guides/mobile-plugin-ima-android) |

## Repository Structure

```
.
├── dev-app
│   └── Development Application
├── sdk
│   ├── Sources
│   └── Unit Tests
├── securesignals-gma
│   └── UID2 / GMA Plugin
├── securesignals-ima
│   └── UID2 / IMA Plugin
├── build.gradle
├── common.gradle
├── LICENSE.md
├── README.md
```

## Requirements

* Android Studio (2022.1.1 Patch 2+)

| Platform | Minimum target                                |
|----------|-----------------------------------------------|
| Android  | 4.4+ / API 19+ (SDK) 5.0+ / API 21+ (Dev-App) |

## Development

The UID2 SDK is a standalone headless library defined and published via Maven Central.  As such the `dev-app` is the primary way for developing the SDK.  Use Android Studio to open the root folder to begin development.

ode style is enforced via the [Spotless Gradle plugin](https://github.com/diffplug/spotless) (using [ktlint](https://pinterest.github.io/ktlint/)). You can fix any formatting issues by running `./gradlew spotlessApply`.

R8 / ProGuard
-------------
If you are using R8 the shrinking and obfuscation rules are included automatically.

ProGuard users must manually add the options from
[uid2-gma.pro](https://github.com/IABTechLab/uid2-android-sdk/blob/main/securesignals-gma/uid2-gma.pro) and [uid2-ima.pro](https://github.com/IABTechLab/uid2-android-sdk/blob/main/securesignals-ima/uid2-ima.pro).

## License

UID2 is released under the Apache license. [See LICENSE](https://github.com/IABTechLab/uid2-android-sdk/blob/main/LICENSE.md) for details.
