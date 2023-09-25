# PredictiveBackSample samples

Shows different types of predictive back animations, including:

+ Back-to-home
+ Cross-activity
+ Custom cross-activity
+ Cross-fragment animation
+ Custom Progress API animation

## Custom cross-activity

In general, rely on the default cross-activity animation; however, if required use
overrideActivityTransition in place of overridePendingTransition. Although animation resources are
expected for `overrideActivityTransition`, we strongly recommend to stop using animation and to
instead use animator and androidx transitions for most use cases.

```kotlin
    override fun onCreate(savedInstanceState: Bundle?) {
        "..."

        overrideActivityTransition(
            OVERRIDE_TRANSITION_OPEN,
            android.R.anim.fade_in,
            0
        )
    
        overrideActivityTransition(
            OVERRIDE_TRANSITION_CLOSE,
            0,
            android.R.anim.fade_out
        )
    }
```

## Cross-fragment animation

Example code uses navigation component default animations.

```xml
<action
    android:id="..."
    app:destination="..."
    app:enterAnim="@animator/nav_default_enter_anim"
    app:exitAnim="@animator/nav_default_exit_anim"
    app:popEnterAnim="@animator/nav_default_pop_enter_anim"
    app:popExitAnim="@animator/nav_default_pop_exit_anim" />
```

## Custom Progress API animation

The following example using the Progress API follows the
[Predictive Back Design Guidance](https://developer.android.com/design/ui/mobile/guides/patterns/predictive-back).

```kotlin
override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    "..."

    val windowWidth = requireActivity().windowManager.currentWindowMetrics.bounds.width()
    val maxXShift = windowWidth / 20

    val predictiveBackCallback = object: OnBackPressedCallback(enabled=false) {

        override fun handleOnBackProgressed(backEvent: BackEventCompat) {
            when (backEvent.swipeEdge) {
                BackEventCompat.EDGE_LEFT ->
                    binding.box.translationX = backEvent.progress * maxXShift
                BackEventCompat.EDGE_RIGHT ->
                    binding.box.translationX = -(backEvent.progress * maxXShift)
            }
            binding.box.scaleX = 1F - (0.1F * backEvent.progress)
            binding.box.scaleY = 1F - (0.1F * backEvent.progress)
        }

        override fun handleOnBackPressed() {
            // your back handling logic
        }

        override fun handleOnBackCancelled() {
            binding.box.scaleX = 1F
            binding.box.scaleY = 1F
            binding.box.translationX = 0F
        }
    }
    
    requireActivity().onBackPressedDispatcher.addCallback(
        this.viewLifecycleOwner,
        predictiveBackCallback
    )
}
```