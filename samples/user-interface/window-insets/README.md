# Immersive mode

"Immersive mode" is intended for apps in which users will be heavily interacting with the screen.
With this API, apps can hide the status bar, the navigation bar, or both. When users need to bring
back the system bars, they swipe from any edge where a system bar is hidden.

"Immersive mode" was originally introduced in Android 4.4 KitKat (API level 19).
[View.setSystemUiVisibility][1] and other APIs provided ability to toggle visibility of System UI
windows, such as the status bar and the navigation bar. The API was overhauled in Android 11 (API
level 30), and apps can now use [WindowInsetsController][2] and other APIs to control the behavior
and visibility of System UI windows that affect [WindowInsets][3]. Alternatively, apps can use
[WindowInsetsControllerCompat][4] to implement this feature in a backward-compatible manner.

[1]: https://developer.android.com/reference/android/view/View#setSystemUiVisibility(int)
[2]: https://developer.android.com/reference/android/view/WindowInsetsController
[3]: https://developer.android.com/reference/android/view/WindowInsets
[4]: https://developer.android.com/reference/androidx/core/view/WindowInsetsControllerCompat

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
