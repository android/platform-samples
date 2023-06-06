# WindowInsets samples

## Immersive mode

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

## WindowInsetsAnimation

This sample shows how to react to the on-screen keyboard (IME) changing visibility, and also
controlling the IME's visibility. To do this, the sample uses the
[WindowInsetsAnimationCompat](https://developer.android.com/reference/androidx/core/view/WindowInsetsAnimationCompat)
and
[WindowInsetsAnimationControllerCompat][wiac] APIs available in [AndroidX Core][androidxcore] and
[Android 11](https://developer.android.com/11).

The app displays a mock instant-message style conversation, and has two key pieces of functionality:

### #1: Reacting to the IME coming on/off screen (API 21+)

When the IME is displayed due to a implicit event (such as the `EditText` being focused), the UI
will react as the IME animates in by moving any relevant views in unison. This creates the effect of
the IME pushing the app's UI up. You can see this in the demo above on the right. 

In terms of implementation, this is done using a
[`WindowInsetsAnimationCompat.Callback`](https://developer.android.com/reference/androidx/core/view/WindowInsetsAnimationCompat.Callback),
which allows views to be notified when an insets animation is taking place. In this sample, we have
provided an implementation called
[`TranslateDeferringInsetsAnimationCallback`](./app/src/main/java/com/google/android/samples/insetsanimation/TranslateDeferringInsetsAnimationCallback.kt)
which automatically moves the host view between it's position before and after the IME visibility
change. This is used on both the text field and scrolling views, allowing them both to move in
unison with the IME.

#### Graceful degradation

As this feature relies on new APIs, we gracefully degrade the experience as so:

 - When running on devices with API level 30+, this feature perfectly tracks the IME as it
   enter/exits the screens. 
 - When running on devices with API level 21-29,
   [`WindowInsetsAnimationCompat`](https://developer.android.com/reference/androidx/core/view/WindowInsetsAnimationCompat)
   will run an animation which attempts to mimic the system IME animation. This will never be able
   to perfectly track the IME, but should provide a pleasant experience for users.
 - When running devices with API < 21, the animation won't run at all and will revert back to an
   instant 'snap'.

### #2: Controlling the IME (API 30+)

When the user scrolls up on the conversation list, to the end of the list's content, and keeps
scrolling (aka over-scrolling) the sample takes control of the IME and animates it on/off screen as
part of the scroll gesture. You can see this in the demo above on the left, as the IME scrolls on
and off screen with the conversation.

In terms of implementation, this is done using the [`WindowInsetsAnimationControllerCompat`][wiac]
API available in [AndroidX Core][androidxcore] 1.5.0. A nested scrolling `LinearLayout` which
automatically reacts to scrolls by insetting the IME on/off screen is provided in
[`InsetsAnimationLinearLayout`](./app/src/main/java/com/google/android/samples/insetsanimation/InsetsAnimationLinearLayout.kt).

A [`View.OnTouchListener`](https://developer.android.com/reference/kotlin/android/view/View.OnTouchListener)
implementation, for use with non-scrolling views is also provided as
[`InsetsAnimationTouchListener`](./app/src/main/java/com/google/android/samples/insetsanimation/InsetsAnimationTouchListener.kt).

As these APIs are in AndroidX Core, they are available to use on devices back to API < 21, _but_
they only provide functionality on devices with Android 11 or newer.

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
