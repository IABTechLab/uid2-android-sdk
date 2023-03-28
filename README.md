# UID2 Android SDK

A framework for integrating [UID2](https://github.com/IABTechLab/uid2docs) into Android applications.


[![License: Apache](https://img.shields.io/badge/License-Apache-green.svg)](https://www.apache.org/licenses/)

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

## License

UID2 is released under the Apache license. [See LICENSE](https://github.com/IABTechLab/uid2-android-sdk/blob/main/LICENSE.md) for details.
