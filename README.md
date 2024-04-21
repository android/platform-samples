![Build](https://github.com/android/platform-samples/actions/workflows/build.yml/badge.svg)

# Android Platform Samples

This repository contains a collection of samples that demonstrate the use of different Android OS platform APIs. The samples are organized into folders by topic, and each folder contains a README file that provides more information about the samples in that folder.

> **Note:** These samples are intended to showcase specific functionality in isolation, and they may use
> simplified code. They are not intended to be used as production-ready code. The project uses the
> [casa-android](https://github.com/google/casa-android) (intended only for demo projects).
> For best practices follow our documentation and check
> [Now In Android](https://github.com/android/nowinandroid)

Browse the samples inside each topic samples folder:

- [Accessibility](https://github.com/android/platform-samples/tree/main/samples/accessibility)
- [Camera](https://github.com/android/platform-samples/tree/main/samples/camera)
- [Connectivity](https://github.com/android/platform-samples/tree/main/samples/connectivity)
- [Graphics](https://github.com/android/platform-samples/tree/main/samples/graphics)
- [Location](https://github.com/android/platform-samples/tree/main/samples/location)
- [Privacy](https://github.com/android/platform-samples/tree/main/samples/privacy)
- [Storage](https://github.com/android/platform-samples/tree/main/samples/storage)
- [User-interface](https://github.com/android/platform-samples/tree/main/samples/user-interface)
- More to come...

We are constantly adding new samples to this repository. You can find a list of all the available samples [here](https://github.com/android/platform-samples/tree/main/samples/README.md).

> 🚧 **Work-in-Progress:** we are working on bringing more existing and new samples into this format.

## How to run

1. Clone the repository
2. Open the whole project in Android Studio.
3. Sync & Run `app` configuration

The app will open with the samples list screen that allows you to navigate throughout the different
categories and available samples.

> **Note:** the `app` module is required to bring together all the samples but it's not relevant
> for their functionality, you can simply ignore it. The wiring is done under the hood and an
> implementation detail not needed to understand any sample functionality.

### Deeplink to sample

To open a specific sample directly you can use one of the auto-generated configurations.

1. Build the project at least once
2. Open `Run Configuration` dropdown
3. Select sample name
4. Run

> **Tip:** use `⌃⌥R` or `Alt+Shift+F10` shortcut to open the full list and launch the selected one. 

## Reporting Issues

You can report an [issue with a sample](https://github.com/android/platform-samples/issues) using
this repository. When doing so, make sure to specify which sample you are referring to.

## Contributions

Please contribute! We will gladly review any pull requests.
Make sure to read the [Contributing](CONTRIBUTING.md) page first though.

> Note: make sure to run `./gradlew --init-script gradle/spotless-init.gradle.kts spotlessApply` before 
> submitting PRs.

## License

```
Copyright 2023 The Android Open Source Project
 
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
