# Drag and Drop Samples

The samples in this directory address various various ways of implementing Drag and Drop, along with
implementation of RichContentReceiver

- [Drag and Drop using Views](src/main/java/com/example/platform/ui/draganddrop/DragAndDropWithViews.kt)
  This sample demonstrate basic implementation with onLongClickListener and implementing
  DragListener callback
- [Drag and Drop in Multi-Window mode](src/main/java/com/example/platform/ui/draganddrop/DragAndDropMultiWindow.kt)
  This sample demonstrate the drag-and-drop across apps.
- [Drag and Drop using DragAndDropHelper Library](src/main/java/com/example/platform/ui/draganddrop/DragAndDropWithHelper.kt)
  This sample uses
  the [`Jetpack Drag and Drop Library`](https://developer.android.com/jetpack/androidx/releases/draganddrop)
  to demonstrate drag-and-drop.
- [Drag and Drop in compose](src/main/java/com/example/platform/ui/draganddrop/DragAndDropUsingCompose.kt)
  This sample showcase drag-and-drop implementation in compose.
- [Accepting rich media](src/main/java/com/example/platform/ui/draganddrop/DragAndDropRichContentReceiverFragment.kt)
  This sample showcase how rich media can be handled using RichContentReceiver unified Api which
  works for drag-and-drop, keyboard and clipboard

> **Important:** All samples run independent of each other.

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
