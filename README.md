# UID2 SDK for Android

The UID 2 Project is subject to the IAB Tech Lab Intellectual Property Rights (IPR) Policy, and is managed by the IAB Tech Lab Addressability Working Group and [Privacy & Rearc Commit Group](https://iabtechlab.com/working-groups/privacy-rearc-commit-group/). Please review the [governance rules](https://github.com/IABTechLab/uid2-core/blob/master/Software%20Development%20and%20Release%20Procedures.md).


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

## Requirements, Installation and Usage

For the latest instructions on how to use the SDK and plugins in this repository, refer to the following:

| Component        | Guide                                                                                                        |
|------------------|--------------------------------------------------------------------------------------------------------------|
| UID2 Android SDK | [UID2 SDK for Android Reference Guide](https://unifiedid.com/docs/sdks/uid2-sdk-ref-android)                 |
| UID2 Google Mobile Ads (GMA) Plugin | [UID2 GMA Plugin for Android Integration Guide](https://unifiedid.com/docs/guides/mobile-plugin-gma-android) |
| UID2 Interactive Media Ads (IMA) Plugin | [UID2 IMA Plugin for Android Integration Guide](https://unifiedid.com/docs/guides/mobile-plugin-ima-android) |

## Development

The UID2 SDK is a standalone headless library defined and published via Maven Central.  As such the `dev-app` is the primary way for developing the SDK.  Use Android Studio to open the root folder to begin development.

Code style is enforced via the [Spotless Gradle plugin](https://github.com/diffplug/spotless) (using [ktlint](https://pinterest.github.io/ktlint/)). You can fix any formatting issues by running `./gradlew spotlessApply`.
