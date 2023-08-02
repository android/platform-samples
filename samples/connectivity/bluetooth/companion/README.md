# Companion Device Manager Sample

This sample showcases the use of the
[Companion Device Manager](https://developer.android.com/reference/android/companion/CompanionDeviceManager#startSystemDataTransfer(int,%20java.util.concurrent.Executor,%20android.os.OutcomeReceiver%3Cjava.lang.Void,android.companion.CompanionException%3E))
(CDM) to find and associate devices. 

## How to use the sample:

1. Use two devices running the sample
2. In one device start the [GATTServerSample](../ble/src/main/java/com/example/platform/connectivity/bluetooth/ble/GATTServerSample.kt)
3. In the other open the [CompanionDeviceManagerSample](/src/main/java/com/example/platform/connectivity/bluetooth/cdm/CompanionDeviceManagerSample.kt))
4. Click start button in the CDM sample

It should directly find the server and a system request will appear. Once accepted both devices will
be associated. You can then connect (see [ConnectGATTSample](../ble/src/main/java/com/example/platform/connectivity/bluetooth/ble/ConnectGATTSample.kt)
and receive (see [CompanionDeviceSampleService](/src/main/java/com/example/platform/connectivity/bluetooth/cdm/CompanionDeviceSampleService.kt))
appear and disappear events (if running A12+).

> Note: You can associate multiple devices. When the server closes and opens again a new mac address
> will be used, thus you need to associate them again.

## License

```
Copyright 2022 The Android Open Source Project
 
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
