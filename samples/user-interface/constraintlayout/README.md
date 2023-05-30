# ConstraintLayout

This module contains samples for `ConstraintLayout`and `MotionLayout`.

## ConstraintLayout

[`ConstraintLayout`][1] lets you create large, complex layouts with a flat view hierarchy—no nested
view groups. All views are laid out according to relationships between sibling views and the parent
layout. It is easy to use with Android Studio's Layout Editor. For detail, read
[Build a responsive UI with ConstraintLayout][2].

[1]: https://developer.android.com/reference/androidx/constraintlayout/widget/ConstraintLayout
[2]: https://developer.android.com/develop/ui/views/layout/constraint-layout

## MotionLayout

[`MotionLayout`][3] is a layout type that helps you manage motion and widget animation in your app.
`MotionLayout` is a subclass of `ConstraintLayout` and builds upon its rich layout capabilities.
For the detail, read [Manage motion and widget animation with MotionLayout][4].

[3]: https://developer.android.com/reference/androidx/constraintlayout/motion/widget/MotionLayout
[4]: https://developer.android.com/develop/ui/views/animations/motionlayout

| Title                       |                                                                   GIF                                                                    | Layout                                                               | MotionScene                                                                                      | 
|:----------------------------|:----------------------------------------------------------------------------------------------------------------------------------------:|:---------------------------------------------------------------------|:-------------------------------------------------------------------------------------------------|
| 01. Basic                   | <img src="https://user-images.githubusercontent.com/796361/53616329-6ce64e80-3c25-11e9-8fe1-a4d60bf25584.gif" height="360" width="180" > | [Layout](src/main/res/layout/motion_01_basic.xml)                    | [MotionScene](src/main/res/xml/scene_01.xml)                                                     | 
| 02. Basic                   | <img src="https://user-images.githubusercontent.com/796361/53616351-84bdd280-3c25-11e9-83ce-b933e276d08e.gif" height="360" width="180" > | [Layout](src/main/res/layout/motion_02_basic.xml)                    | [MotionScene](src/main/res/xml/scene_02.xml)                                                     | 
| 02. Basic, no auto complete | <img src="https://user-images.githubusercontent.com/796361/57440055-4296ba00-7282-11e9-8209-981ba020cda1.gif" height="360" width="180" > | [Layout](src/main/res/layout/motion_02_basic_autocomplete_false.xml) | [MotionScene](src/main/res/xml/scene_02_autocomplete_false.xml)                                  | 
| 03. Custom attribute        | <img src="https://user-images.githubusercontent.com/796361/53616369-91dac180-3c25-11e9-9245-7ab48fc94334.gif" height="360" width="180" > | [Layout](src/main/res/layout/motion_03_custom_attribute.xml)         | [MotionScene](src/main/res/xml/scene_03.xml)                                                     | 
| 04. ImageFilterView 1       | <img src="https://user-images.githubusercontent.com/796361/53616380-9c955680-3c25-11e9-801e-d6d2bbf140a3.gif" height="360" width="180" > | [Layout](src/main/res/layout/motion_04_imagefilter.xml)              | [MotionScene](src/main/res/xml/scene_04.xml)                                                     | 
| 04. ImageFilterView 2       | <img src="https://user-images.githubusercontent.com/796361/53616396-a6b75500-3c25-11e9-985f-8c800b1bb174.gif" height="360" width="180" > | [Layout](src/main/res/layout/motion_05_imagefilter.xml)              | [MotionScene](src/main/res/xml/scene_05.xml)                                                     | 
| 06. Keyframe position       | <img src="https://user-images.githubusercontent.com/796361/53616407-b171ea00-3c25-11e9-8cd7-03c1631b4fa1.gif" height="360" width="180" > | [Layout](src/main/res/layout/motion_06_keyframe.xml)                 | [MotionScene](src/main/res/xml/scene_06.xml)                                                     | 
| 07. Keyframe interpolation  | <img src="https://user-images.githubusercontent.com/796361/53616488-26452400-3c26-11e9-9e6a-eb216d0d0379.gif" height="360" width="180" > | [Layout](src/main/res/layout/motion_07_keyframe.xml)                 | [MotionScene](src/main/res/xml/scene_07.xml)                                                     | 
| 08. Keyframe cycle          | <img src="https://user-images.githubusercontent.com/796361/53616423-c5b5e700-3c25-11e9-98a4-d98351664844.gif" height="360" width="180" > | [Layout](src/main/res/layout/motion_08_cycle.xml)                    | [MotionScene](src/main/res/xml/scene_08.xml)                                                     | 
| 09. CoordinatorLayout 1     | <img src="https://user-images.githubusercontent.com/796361/53616433-cea6b880-3c25-11e9-9a56-1512385772e5.gif" height="360" width="180" > | [Layout](src/main/res/layout/motion_09_coordinatorlayout.xml)        | [MotionScene](src/main/res/xml/scene_09.xml)                                                     | 
| 10. CoordinatorLayout 2     | <img src="https://user-images.githubusercontent.com/796361/53616441-d9f9e400-3c25-11e9-8b5b-e49cbb255850.gif" height="360" width="180" > | [Layout](src/main/res/layout/motion_10_coordinatorlayout.xml)        | [MotionScene](src/main/res/xml/scene_10_header.xml)                                              | 
| 11. CoordinatorLayout 3     | <img src="https://user-images.githubusercontent.com/796361/53616794-a029dd00-3c27-11e9-9fa9-848c1cde736b.gif" height="360" width="180" > | [Layout](src/main/res/layout/motion_11_coordinatorlayout.xml)        | [MotionScene](src/main/res/xml/scene_11_header.xml)                                              | 
| 12. DrawerLayout 1          | <img src="https://user-images.githubusercontent.com/796361/53616524-4d9bf100-3c26-11e9-85db-88b1450be0a3.gif" height="360" width="180" > | [Layout](src/main/res/layout/motion_12_drawerlayout.xml)             | [MotionScene](src/main/res/xml/scene_12_content.xml)                                             | 
| 13. DrawerLayout 2          | <img src="https://user-images.githubusercontent.com/796361/53616767-8092b480-3c27-11e9-8a25-cfba87a5dedf.gif" height="360" width="180" > | [Layout](src/main/res/layout/motion_13_drawerlayout.xml)             | [Content](src/main/res/xml/scene_12_content.xml) <br> [Menu](src/main/res/xml/scene_13_menu.xml) | 
| 14. SidePanel               | <img src="https://user-images.githubusercontent.com/796361/53616774-8b4d4980-3c27-11e9-9f98-00b42a6f862d.gif" height="360" width="180" > | [Layout](src/main/res/layout/motion_14_side_panel.xml)               | [MotionScene](src/main/res/xml/scene_14.xml)                                                     | 
| 15. Parallax                | <img src="https://user-images.githubusercontent.com/796361/53616582-a4092f80-3c26-11e9-8f80-05d91fe42f8f.gif" height="360" width="180" > | [Layout](src/main/res/layout/motion_15_parallax.xml)                 | [MotionScene](src/main/res/xml/scene_15.xml)                                                     | 
| 16. ViewPager               | <img src="https://user-images.githubusercontent.com/796361/53616757-74a6f280-3c27-11e9-8b20-0b166a00928d.gif" height="360" width="180" > | [Layout](src/main/res/layout/motion_16_viewpager.xml)                | [MotionScene](src/main/res/xml/scene_15.xml)                                                     | 
| 17. Complex Motion 1        | <img src="https://user-images.githubusercontent.com/796361/53616661-06623000-3c27-11e9-8616-901a22d2cf38.gif" height="360" width="180" > | [Layout](src/main/res/layout/motion_17_coordination.xml)             | [MotionScene](src/main/res/xml/scene_17.xml)                                                     | 
| 18. Complex Motion 2        | <img src="https://user-images.githubusercontent.com/796361/53616679-1ed24a80-3c27-11e9-9435-badc2ad2da97.gif" height="360" width="180" > | [Layout](src/main/res/layout/motion_18_coordination.xml)             | [MotionScene](src/main/res/xml/scene_18.xml)                                                     | 
| 19. Complex Motion 3        | <img src="https://user-images.githubusercontent.com/796361/53616684-2691ef00-3c27-11e9-9009-4e54debb1636.gif" height="360" width="180" > | [Layout](src/main/res/layout/motion_19_coordination.xml)             | [MotionScene](src/main/res/xml/scene_19.xml)                                                     | 
| 20. Complex Motion 4        |                                                                   N/A                                                                    | [Layout](src/main/res/layout/motion_20_reveal.xml)                   | [MotionScene](src/main/res/xml/scene_20.xml)                                                     | 
| 21. Fragment transition 1   | <img src="https://user-images.githubusercontent.com/796361/53616688-301b5700-3c27-11e9-9868-f5cd6a788b78.gif" height="360" width="180" > | [Layout](src/main/res/layout/motion_21_container.xml)                | [MotionScene](src/main/res/xml/main_scene.xml)                                                   |
| 22. Fragment transition 2   | <img src="https://user-images.githubusercontent.com/796361/53616701-3c9faf80-3c27-11e9-85f7-487668e44f79.gif" height="360" width="180" > | [Layout](src/main/res/layout/motion_21_container.xml)                | [MotionScene](src/main/res/xml/main_scene.xml)                                                   |  
| 23. Lottie                  | <img src="https://user-images.githubusercontent.com/796361/53616613-cb5ffc80-3c26-11e9-8b6b-2b2b9f4ef883.gif" height="360" width="180" > | [Layout](src/main/res/layout/motion_23_viewpager.xml)                | [MotionScene](src/main/res/xml/scene_23.xml)                                                     | 
| 24. YouTube-like motion     | <img src="https://user-images.githubusercontent.com/796361/53616722-4f19e900-3c27-11e9-86b4-7ecaaeb57d9a.gif" height="360" width="180" > | [Layout](src/main/res/layout/motion_24_youtube.xml)                  | [MotionScene](src/main/res/xml/scene_24.xml)                                                     |  
| 25. KeyTrigger              | <img src="https://user-images.githubusercontent.com/796361/53616732-59d47e00-3c27-11e9-89b6-b6174c4bddfe.gif" height="360" width="180" > | [Layout](src/main/res/layout/motion_25_keytrigger.xml)               | [MotionScene](src/main/res/xml/scene_25.xml)                                                     |  
| 26. Multi-state             | <img src="https://user-images.githubusercontent.com/796361/53616746-635de600-3c27-11e9-9ad8-451cc175de3d.gif" height="360" width="180" > | [Layout](src/main/res/layout/motion_26_multistate.xml)               | [MotionScene](src/main/res/xml/scene_26.xml)                                                     |  

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
