# TextSpan samples

This sample demonstrates several text-related features in Android.

### [Text Span](src/main/java/com/example/platform/ui/text/TextSpan.kt)

[`buildSpannedString`](https://developer.android.com/reference/kotlin/androidx/core/text/package-summary#buildSpannedString(kotlin.Function1))
in the [core-ktx](https://developer.android.com/kotlin/ktx#core) library is useful for quickly
building a rich text.

### [Linkify](src/main/java/com/example/platform/ui/text/Linkify.kt)

[LinkifyCompat](https://developer.android.com/reference/androidx/core/text/util/LinkifyCompat)
is useful for creating links in TextViews. The API supports email addresses, phone numbers, and web
URLs out of the box, and you can also use regular expressions to create a custom link pattern.

### [Hyphenation](src/main/java/com/example/platform/ui/text/Hyphenation.kt)

Automatic hyphenation is available for a number of languages including English.

Android 6.0 Marshmallow (API level 23) introduced the
[android:hyphenationFrequency](https://developer.android.com/reference/android/widget/TextView#attr_android:hyphenationFrequency)
attribute to apply automatic hyphenation to a `TextView`.

Android 13 (API level 33) introduced 2 new options, `fullFast` and `normalFast`. These are the same
as `full` and `normal`, but use faster algorithm for better performance.

### [Line break]src/main/java/com/example/platform/ui/text/LineBreak.kt)

This feature is relevant to _languages written without spaces between words_, such as Japanese and
Chinese. This particular demo works only when the device locale is set to Japanese.

Android 13 (API level 33) introduced the `android:lineBreakWordStyle` attribute to `TextView`. Set
this attribute to `"phrase"`, and
[phrases (bunsetsu)](https://ja.wikipedia.org/wiki/%E6%96%87%E7%AF%80)
in the text will not be separated by line breaks.

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
