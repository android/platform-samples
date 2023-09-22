# PredictiveBackSample samples

Shows different types of predictive back animations, including:

+ Back-to-home
+ Cross-activity
+ Custom cross-activity.

Although animation resources are expected for `overrideActivityTransition`, we strongly recommend to
stop using animation and to instead use animator and androidx transitions for most use cases.

```kotlin
    override fun onCreate(savedInstanceState: Bundle?) {
        "..."

        this.overrideActivityTransition(
            OVERRIDE_TRANSITION_OPEN,
            android.R.anim.fade_in,
            0
        )
    
        this.overrideActivityTransition(
            OVERRIDE_TRANSITION_CLOSE,
            0,
            android.R.anim.fade_out
        )
    }
```

+ Cross-fragment animations with navigation component default animations. Example code:

```xml
<action
    android:id="..."
    app:destination="..."
    app:enterAnim="@animator/nav_default_enter_anim"
    app:exitAnim="@animator/nav_default_exit_anim"
    app:popEnterAnim="@animator/nav_default_pop_enter_anim"
    app:popExitAnim="@animator/nav_default_pop_exit_anim" />
```

+ (coming soon) androidx-transitions