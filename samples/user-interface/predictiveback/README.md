# Predictive Back Samples

Shows different types of predictive back animations, including:

+ Back-to-home
+ Cross-activity
+ Custom cross-activity
+ Cross-fragment animation
+ Shared element cross-fragment animation
+ Custom Progress API animation
+ Custom AndroidX Transition
+ Cross-fragment animation with MaterialSharedAxis
+ setCustomAnimations

## Custom cross-activity

In general, rely on the default cross-activity animation; however, if required use
`overrideActivityTransition` instead of `overridePendingTransition`. Although animation resources are
expected for `overrideActivityTransition`, we strongly recommend to stop using animation and to
instead use animator and androidx transitions for most use cases. For more details see the
[developer documentation](https://developer.android.com/guide/navigation/custom-back/predictive-back-gesture).

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

## Shared element cross-fragment animation

Example code shows a simple cross-fragment animation between two [shared elements](https://developer.android.com/guide/fragments/animate#shared)
that automatically works with predictive back gesture navigation provided fragment and androidx
transitions dependencies are upgraded to
[1.7.0](https://developer.android.com/jetpack/androidx/releases/fragment#1.7.0-alpha08) and
[1.5.0](https://developer.android.com/jetpack/androidx/releases/transition#1.5.0-alpha06) respectively.

On the first fragment, where "second_card" is the `android:transitionName` of a view in the second
fragment:
```kotlin
binding.sharedElementCrossFragment.setOnClickListener {
   findNavController().navigate(
       R.id.show_PBSharedElementTransitionFragment,
       null,
       null,
       FragmentNavigatorExtras(it to "second_card")
   )
}
```

On the second fragment:
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enterTransition = AutoTransition()
    sharedElementEnterTransition = AutoTransition()
}
```

Note that `enterTransition` must be specified in addition to `sharedElementEnterTransition`, which
will mark the shared element as seekable and therefore enable the predictive back gesture to play.

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

## Custom AndroidX Transition
For more details see the
[developer documentation](https://developer.android.com/about/versions/14/features/predictive-back#androidx-transitions).

```kotlin
class MyFragment : Fragment() {

    val transitionSet = TransitionSet().apply {
        addTransition(Fade(Fade.MODE_OUT))
        addTransition(ChangeBounds())
        addTransition(Fade(Fade.MODE_IN))
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val callback = object : OnBackPressedCallback(enabled = false) {

            var controller: TransitionSeekController? = null

            override fun handleOnBackStarted(backEvent: BackEvent) {
                // Create the transition
                controller = TransitionManager.controlDelayedTransition(
                    // textContainer is a FrameLayout containing the shortText and longText TextViews
                    binding.textContainer,
                    transitionSet
                )
                changeTextVisibility(ShowText.SHORT)
            }

            override fun handleOnBackProgressed(backEvent: BackEvent) {
                // Play the transition as the user swipes back
                if (controller?.isReady == true) {
                    controller?.currentFraction = backEvent.progress
                }
            }

            override fun handleOnBackPressed() {
                // Finish playing the transition when the user commits back
                controller?.animateToEnd()
                this.isEnabled = false
            }

            override fun handleOnBackCancelled() {
                // If the user cancels the back gesture, reset the state
                transition(ShowText.LONG)
            }
        }

        binding.shortText.setOnClickListener {
            transition(ShowText.LONG)
            callback.isEnabled = true
        }

        this.requireActivity().onBackPressedDispatcher.addCallback(callback)
    }

    private fun transition(showText: ShowText) {
        TransitionManager.beginDelayedTransition(
            binding.textContainer,
            transitionSet
        )
        changeTextVisibility(showText)
    }

    enum class ShowText { SHORT, LONG }
    private fun changeTextVisibility(showText: ShowText) {
        when (showText) {
            ShowText.SHORT -> {
                binding.shortText.isVisible = true
                binding.longText.isVisible = false
            }
            ShowText.LONG -> {
                binding.shortText.isVisible = false
                binding.longText.isVisible = true
            }
        }
    }
}
```

## Cross-fragment animation with MaterialSharedAxis

MaterialSharedAxis is a Visibility transition. A Visibility transition is triggered when the target
Fragment's visibility is changed or when the Fragment is added or removed. This means MaterialSharedAxis
requires a View to be changing in visibility or to be added or removed to trigger its animation.

For more details see the
[developer documentation](https://m2.material.io/develop/android/theming/motion#shared-axis).

```kotlin
// FragmentA.kt

override fun onCreateView(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    exitTransition = MaterialSharedAxis(MaterialSharedAxis.Z, /* forward= */ true)
    reenterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, /* forward= */ false)
}

// FragmentB.kt

override fun onCreateView(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, /* forward= */ true)
    returnTransition = MaterialSharedAxis(MaterialSharedAxis.Z, /* forward= */ false)
}
```
## setCustomAnimations

Use `setEnterTransition`, `setExitTransition`, `setReenterTransition`, `setReturnTransition`,
`setSharedElementEnterTransition`, `setSharedElementReturnTransition` instead of `setCustomAnimations`
where possible. 

However, if you are using `setCustomAnimations`, here's a code sample showing
predictive back working with animators and fragment manager.

```kotlin
// PBSetCustomAnimationsActivity.kt

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    "..."
    supportFragmentManager.commit {
        replace(R.id.fragment_container, PBSetCustomAnimationsFirstFragment())
    }

}

// PBSetCustomAnimationsFirstFragment.kt

override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?,
): View {
    _binding = FragmentSetCustomAnimationsBinding
        .inflate(inflater, container, false)

    binding.box.setOnClickListener {
        parentFragmentManager.commit {
            setCustomAnimations(
                android.R.animator.fade_in, // enter
                android.R.animator.fade_out, // exit
                android.R.animator.fade_in, // popEnter
                android.R.animator.fade_out) // popExit
            replace(R.id.fragment_container,PBSetCustomAnimationsSecondFragment())
            setReorderingAllowed(true)
            addToBackStack(null)
        }
    }
    return binding.root
}
```

