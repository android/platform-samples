# Permission samples

The samples in this directory showcase how to request
[single](src/main/java/com/example/platform/privacy/permissions/SinglePermission.kt)
and [multiple](src/main/java/com/example/platform/privacy/permissions/MultiplePermissions.kt)
permissions using the [ActivityResultContracts](https://developer.android.com/reference/androidx/activity/result/contract/ActivityResultContracts) API part of:

- [androidx.activity](https://developer.android.com/jetpack/androidx/releases/activity), 
version 1.2.0 or later
- or [androidx.fragment](https://developer.android.com/jetpack/androidx/releases/fragment),
version 1.3.0 or later

In addition, it showcases how to [request permissions in Jetpack Compose](src/main/java/com/example/platform/privacy/permissions/ComposePermissions.kt)
using [accompanist-permissions](https://github.com/google/accompanist/tree/main/permissions) library. 

> **Important:** All samples run independent of each other but the permission status are shared since
they are part of the same app. Thus accepting or revoking a permission in one sample might affect
the other.

While these samples follow the recommended flow when requesting permissions, code and UX is simplified
for brevity. For more check out the documentation at
https://developer.android.com/guide/topics/permissions/overview

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

