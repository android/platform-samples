# PredictiveBackSample samples

Shows different types of predictive back animations, including:

+ Back-to-home
+ Cross-activity
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
+ (coming soon) custom cross-activity