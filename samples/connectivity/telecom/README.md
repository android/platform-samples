# Telecom Sample

This module contains the sample for integrating Jetpack Telecom SDK to do audio and/or video calls
using the Android Telecom stack.

The sample simulates a caller app that can make ongoing calls and receive incoming calls. There is
no actual call being made, the sample uses [AudioLoopSource](https://github.com/android/platform-samples/blob/14d464a7c2613e808024f12b2d1c23be15368f4e/samples/connectivity/audio/src/main/java/com/example/platform/connectivity/audio/datasource/AudioLoopSource.kt)
to capture the audio in the device and loop it back to the active endpoint (e.g speaker)

The structure of the sample is the following:

- [TelecomCallSample](src/main/java/com/example/platform/connectivity/telecom/TelecomCallSample.kt):
The entry point of the sample with the options to perform a call or to fake an incoming call
- [TelecomCallActivity](src/main/java/com/example/platform/connectivity/telecom/call/TelecomCallActivity.kt):
The activity to be launch when there is an active call. It handles the UI logic based on the current call.
- [TelecomCallService](src/main/java/com/example/platform/connectivity/telecom/call/TelecomCallService.kt):
A service that manage the logic of the call and communicates with the Telecom SDK
- [TelecomCallRepository](src/main/java/com/example/platform/connectivity/telecom/model/TelecomCallRepository.kt):
The actual logic to communicate with the Telecom SDK
