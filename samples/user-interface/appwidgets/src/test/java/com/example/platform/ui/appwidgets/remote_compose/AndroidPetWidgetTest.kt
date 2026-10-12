/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.platform.ui.appwidgets.remote_compose

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.PaintContext
import androidx.compose.remote.core.RcPlatformServices
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.VariableSupport
import androidx.compose.remote.core.operations.Theme
import androidx.compose.remote.core.operations.layout.Component
import androidx.compose.remote.core.operations.layout.LayoutComponent
import androidx.compose.remote.core.operations.paint.PaintBundle
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import java.io.ByteArrayInputStream
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.ConscryptMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@ConscryptMode(ConscryptMode.Mode.OFF)
class AndroidPetWidgetTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        BuddyPreferences.prefs(context).edit().clear().commit()
        WidgetAssetProvider.clearCache()
        RemoteWidgetActionBridge.clearLambdaActions()
    }

    @After
    fun tearDown() {
        BuddyPreferences.prefs(context).edit().clear().commit()
        WidgetAssetProvider.clearCache()
    }

    @Test
    fun petTaps_coinButtonHiddenUntilThreeTaps_andResetsWhenClaimed() = runBlocking {
        val widgetId = 42
        val doc = captureWidgetDocument(widgetId)
        val remoteContext = TestRemoteContext(widgetId)

        doc.initializeContext(remoteContext)
        doc.paint(remoteContext, Theme.UNSPECIFIED)

        val root = doc.rootLayoutComponent
        assertNotNull("Expected RootLayoutComponent in CoreDocument", root)

        // StoreButton is 48x48 with 12dp padding at BottomEnd (outer box: x=228, y=228, w=72, h=72)
        val storeBox = findComponentByBounds(root!!, x = 228f, y = 228f, w = 72f, h = 72f)
        assertNotNull("Expected StoreButton at BottomEnd (228, 228, 72x72) in:\n${describeTree(root)}", storeBox)
        assertTrue("StoreButton should always be visible", storeBox!!.isVisible)

        // ClaimCreditButton is 48x48 with end=68dp, bottom=12dp at BottomEnd (outer box: x=184, y=240, w=116, h=60)
        val claimBox = findComponentByBounds(root, x = 184f, y = 240f, w = 116f, h = 60f)
        assertNotNull("Expected ClaimCreditButton at BottomEnd (184, 240, 116x60) in:\n${describeTree(root)}", claimBox)

        // 0 taps: coin button is hidden
        assertFalse("Coin button must be invisible at 0 taps", claimBox!!.isVisible)

        // Clicking coin button while invisible does nothing
        doc.onClick(remoteContext, 208f, 264f)
        assertEquals(0, BuddyPreferences.getCoinsForWidget(context, widgetId))

        // Tap 1 on pet: coin button still invisible
        doc.touchDown(remoteContext, 150f, 150f)
        doc.paint(remoteContext, Theme.UNSPECIFIED)
        doc.paint(remoteContext, Theme.UNSPECIFIED)
        assertFalse("Coin button must remain invisible after 1 tap", claimBox.isVisible)

        // Tap 2 on pet: coin button still invisible
        doc.touchDown(remoteContext, 150f, 150f)
        doc.paint(remoteContext, Theme.UNSPECIFIED)
        doc.paint(remoteContext, Theme.UNSPECIFIED)
        assertFalse("Coin button must remain invisible after 2 taps", claimBox.isVisible)

        // Tap 3 on pet: coin button becomes visible locally without waking app process
        doc.touchDown(remoteContext, 150f, 150f)
        doc.paint(remoteContext, Theme.UNSPECIFIED)
        doc.paint(remoteContext, Theme.UNSPECIFIED)
        assertTrue(
            "Coin button must become visible on the 3rd tap",
            claimBox.isVisible,
        )
        assertEquals(
            "No host actions should fire during local pet taps",
            0,
            remoteContext.triggeredActionIds.size,
        )

        // Tap the coin button to claim credit:
        // 1. Persists +1 coin in BuddyPreferences
        // 2. Resets local tap state so coin disappears again until another 3 taps
        doc.onClick(remoteContext, 208f, 264f)
        shadowOf(Looper.getMainLooper()).idle()
        doc.paint(remoteContext, Theme.UNSPECIFIED)
        doc.paint(remoteContext, Theme.UNSPECIFIED)

        assertEquals(1, BuddyPreferences.getCoinsForWidget(context, widgetId))
        assertFalse("Coin button must hide again after claiming", claimBox.isVisible)

        // Another 3 taps makes the coin button reappear and allows claiming a 2nd coin
        repeat(2) {
            doc.touchDown(remoteContext, 150f, 150f)
            doc.paint(remoteContext, Theme.UNSPECIFIED)
            doc.paint(remoteContext, Theme.UNSPECIFIED)
            assertFalse(claimBox.isVisible)
        }
        doc.touchDown(remoteContext, 150f, 150f)
        doc.paint(remoteContext, Theme.UNSPECIFIED)
        doc.paint(remoteContext, Theme.UNSPECIFIED)
        assertTrue("Coin button must reappear after another 3 taps", claimBox.isVisible)

        doc.onClick(remoteContext, 208f, 264f)
        shadowOf(Looper.getMainLooper()).idle()
        doc.paint(remoteContext, Theme.UNSPECIFIED)
        doc.paint(remoteContext, Theme.UNSPECIFIED)
        assertEquals(2, BuddyPreferences.getCoinsForWidget(context, widgetId))
        assertFalse(claimBox.isVisible)
    }

    @Test
    fun storeButton_clickLaunchesStoreActivityWithWidgetId() = runBlocking {
        val widgetId = 42
        val doc = captureWidgetDocument(widgetId)
        val remoteContext = TestRemoteContext(widgetId)

        doc.initializeContext(remoteContext)
        doc.paint(remoteContext, Theme.UNSPECIFIED)

        // Click StoreButton center (x = 240 + 24 = 264, y = 240 + 24 = 264)
        doc.onClick(remoteContext, 264f, 264f)

        val startedIntent = shadowOf(context as android.app.Application).nextStartedActivity
        assertNotNull("Expected StoreButton click to start StoreActivity", startedIntent)
        assertEquals(StoreActivity::class.java.name, startedIntent.component?.className)
        assertEquals(
            widgetId,
            startedIntent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID,
            ),
        )
        assertEquals(
            widgetId,
            startedIntent.getIntExtra(
                StoreActivity.EXTRA_WIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID,
            ),
        )
    }

    @Test
    fun storeActivity_singleWidget_displaysWidgetIdAndCoins() {
        BuddyPreferences.saveCoinsForWidget(context, 42, 3)

        val intent = Intent(context, StoreActivity::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 42)
        }
        ActivityScenario.launch<StoreActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val texts = collectSemanticsTexts(activity)
                assertTrue("Expected 'Widget ID: 42' in $texts", texts.contains("Widget ID: 42"))
                assertTrue("Expected 'Coins: 3' in $texts", texts.contains("Coins: 3"))
                assertTrue("Expected 'Active Widget' in $texts", texts.contains("Active Widget"))
            }
        }
    }

    @Test
    fun storeActivity_multipleWidgets_displaysAllActiveWidgetsAndBalances() {
        BuddyPreferences.saveCoinsForWidget(context, 101, 2)
        BuddyPreferences.saveCoinsForWidget(context, 102, 5)

        val intent = Intent(context, StoreActivity::class.java)
        ActivityScenario.launch<StoreActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val texts = collectSemanticsTexts(activity)
                assertTrue("Expected 'Widget ID: 101' in $texts", texts.contains("Widget ID: 101"))
                assertTrue("Expected 'Coins: 2' in $texts", texts.contains("Coins: 2"))
                assertTrue("Expected 'Widget ID: 102' in $texts", texts.contains("Widget ID: 102"))
                assertTrue("Expected 'Coins: 5' in $texts", texts.contains("Coins: 5"))
            }
        }
    }

    @Test
    fun storeActivity_noActiveWidgets_displaysEmptyStateMessage() {
        val intent = Intent(context, StoreActivity::class.java)
        ActivityScenario.launch<StoreActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val texts = collectSemanticsTexts(activity)
                assertTrue(
                    "Expected empty state message in $texts",
                    texts.contains("No active Android Pet widgets"),
                )
            }
        }
    }

    @Test
    fun onDeleted_removesDeletedWidgetStateOnly() {
        BuddyPreferences.saveCoinsForWidget(context, 101, 4)
        BuddyPreferences.saveTapsCount(context, 101, 2)
        BuddyPreferences.saveCoinsForWidget(context, 102, 7)

        AndroidPetWidget().onDeleted(context, intArrayOf(101))

        assertEquals(0, BuddyPreferences.getCoinsForWidget(context, 101))
        assertEquals(0, BuddyPreferences.getTapsCount(context, 101))
        assertEquals(7, BuddyPreferences.getCoinsForWidget(context, 102))
        assertEquals(listOf(102), BuddyPreferences.getActiveWidgetIds(context))
    }

    @Test
    fun multipleWidgets_actionClickedBroadcasts_operateIndependentlyWithoutInterference() = runBlocking {
        val widgetA = 10
        val widgetB = 20

        // Capture both widgets (simulating widget A and widget B both pinned)
        captureWidgetDocument(widgetA)
        captureWidgetDocument(widgetB)

        // Claim action for widget A (slot 0 -> id = 10 * 1000 + 0 = 10000)
        // Store action for widget B (slot 1 -> id = 20 * 1000 + 1 = 20001)
        val receiver = AndroidPetWidget()
        val claimIntentA = Intent(context, AndroidPetWidget::class.java).apply {
            action = "androidx.compose.remote.ACTION_CLICKED"
            putExtra("widgetId", widgetA)
            putExtra("id", widgetA * 1000 + 0)
        }
        receiver.onReceive(context, claimIntentA)

        assertEquals(1, BuddyPreferences.getCoinsForWidget(context, widgetA))
        assertEquals(0, BuddyPreferences.getCoinsForWidget(context, widgetB))

        // Now click StoreButton on widget B via ACTION_CLICKED without manually re-updating widget B first
        val storeIntentB = Intent(context, AndroidPetWidget::class.java).apply {
            action = "androidx.compose.remote.ACTION_CLICKED"
            putExtra("widgetId", widgetB)
            putExtra("id", widgetB * 1000 + 1)
        }
        receiver.onReceive(context, storeIntentB)

        val startedIntent = shadowOf(context as android.app.Application).nextStartedActivity
        assertNotNull("Expected StoreButton click on widget B to start StoreActivity", startedIntent)
        assertEquals(
            widgetB,
            startedIntent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID,
            ),
        )
    }

    @Test
    fun assetsAndPreferences_edgeCases_decodeBitmapsAndClampNegativeValues() {
        val claimBitmap = WidgetAssetProvider.claimCreditBitmap(context)
        val storeBitmap = WidgetAssetProvider.storeBitmap(context)
        assertTrue(claimBitmap.width > 0 && claimBitmap.height > 0)
        assertTrue(storeBitmap.width > 0 && storeBitmap.height > 0)

        // Negative values clamp to 0
        BuddyPreferences.saveCoinsForWidget(context, 55, -10)
        BuddyPreferences.saveTapsCount(context, 55, -5)
        assertEquals(0, BuddyPreferences.getCoinsForWidget(context, 55))
        assertEquals(0, BuddyPreferences.getTapsCount(context, 55))

        // Invalid widget ID (0) is excluded from active widget IDs
        BuddyPreferences.saveCoinsForWidget(context, AppWidgetManager.INVALID_APPWIDGET_ID, 3)
        assertFalse(
            BuddyPreferences.getActiveWidgetIds(context)
                .contains(AppWidgetManager.INVALID_APPWIDGET_ID)
        )
    }

    @Test
    fun onUpdate_andUpdateAppWidget_populateLambdaActionsForAllWidgetsEvenWhenUnbound() {
        val manager = AppWidgetManager.getInstance(context)
        val widget = AndroidPetWidget()

        // Calling onUpdate with multiple unbound widget IDs in Robolectric must still
        // compose Content and preserve registered WidgetLambdaActions for both widgets.
        widget.onUpdate(context, manager, intArrayOf(10, 20))

        RemoteWidgetActionBridge.runLambdaAction(10 * 1000 + 0)
        assertEquals(1, BuddyPreferences.getCoinsForWidget(context, 10))
        assertEquals(0, BuddyPreferences.getCoinsForWidget(context, 20))

        RemoteWidgetActionBridge.runLambdaAction(20 * 1000 + 0)
        assertEquals(1, BuddyPreferences.getCoinsForWidget(context, 10))
        assertEquals(1, BuddyPreferences.getCoinsForWidget(context, 20))

        // updateAppWidget must also compose Content for a new widget ID and send
        // ACTION_APPWIDGET_UPDATE guarded by EXTRA_ALREADY_UPDATED.
        AndroidPetWidget.updateAppWidget(context, 30)
        RemoteWidgetActionBridge.runLambdaAction(30 * 1000 + 0)
        assertEquals(1, BuddyPreferences.getCoinsForWidget(context, 30))
    }

    @Test
    fun directContentCapture_withoutExternalCompositionLocalProvider_registersCorrectWidgetId() = runBlocking {
        val widgetId = 77
        val widget = AndroidPetWidget()
        val captured = captureSingleRemoteDocument(context = context) {
            widget.Content(context, widgetId)
        }
        val buffer = RemoteComposeBuffer.fromInputStream(ByteArrayInputStream(captured.bytes))
        val doc = CoreDocument().apply {
            initFromBuffer(buffer)
        }
        val remoteContext = TestRemoteContext(widgetId)
        doc.initializeContext(remoteContext)
        doc.paint(remoteContext, Theme.UNSPECIFIED)

        // Click StoreButton center (264f, 264f) -> must trigger actionId 77001 (not -999)
        doc.onClick(remoteContext, 264f, 264f)
        assertEquals(listOf(widgetId * 1000 + 1), remoteContext.triggeredActionIds)

        val startedIntent = shadowOf(context as android.app.Application).nextStartedActivity
        assertNotNull("Expected StoreButton click to start StoreActivity", startedIntent)
        assertEquals(
            widgetId,
            startedIntent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID,
            ),
        )
    }

    @Test
    fun storeActivity_livePreferenceUpdates_recomposeCoinBalanceImmediately() {
        BuddyPreferences.saveCoinsForWidget(context, 42, 1)

        val intent = Intent(context, StoreActivity::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 42)
        }
        ActivityScenario.launch<StoreActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val initialTexts = collectSemanticsTexts(activity)
                assertTrue("Expected 'Coins: 1' in $initialTexts", initialTexts.contains("Coins: 1"))

                // Mutate preferences while StoreActivity is already resumed
                BuddyPreferences.addCoinsForWidget(context, 42, 2)
                val updatedTexts = collectSemanticsTexts(activity)
                assertTrue("Expected 'Coins: 3' after live update in $updatedTexts", updatedTexts.contains("Coins: 3"))
            }
        }
    }

    @Test
    fun onUpdate_registersWidgetsWithZeroCoins_andStoreActivityDisplaysThem() {
        val manager = AppWidgetManager.getInstance(context)
        AndroidPetWidget().onUpdate(context, manager, intArrayOf(501, 502))

        assertEquals(listOf(501, 502), BuddyPreferences.getActiveWidgetIds(context))
        assertEquals(0, BuddyPreferences.getCoinsForWidget(context, 501))
        assertEquals(0, BuddyPreferences.getCoinsForWidget(context, 502))

        // Launching StoreActivity without EXTRA_APPWIDGET_ID (as from the samples list)
        // must still display both active widgets with 0 coins.
        ActivityScenario.launch(StoreActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val texts = collectSemanticsTexts(activity)
                assertTrue("Expected 'Widget ID: 501' in $texts", texts.contains("Widget ID: 501"))
                assertTrue("Expected 'Widget ID: 502' in $texts", texts.contains("Widget ID: 502"))
                assertTrue("Expected 'Coins: 0' in $texts", texts.contains("Coins: 0"))
            }
        }
    }

    @Test
    fun buddyRepository_andTapsCountInteroperability_syncCoinsAndTapsCount() {
        val repo = BuddyRepository(context)
        val widgetId = 88

        assertEquals(0, repo.getCoinsForWidget(widgetId))
        assertEquals(0, repo.getTapsCount(widgetId))

        // playWithBuddy increments both coins and tapsCount (GamingWidget compatibility)
        val updated = repo.playWithBuddy(widgetId)
        assertEquals(1, updated)
        assertEquals(1, repo.getCoinsForWidget(widgetId))
        assertEquals(1, repo.getTapsCount(widgetId))
        assertEquals(1, repo.getWidgetBuddyState(widgetId).coins)
        assertEquals(1, repo.getWidgetBuddyState(widgetId).tapsCount)

        // Saving via tapsCount alone (GamingWidget convention) is reflected by getCoinsForWidget
        val otherWidgetId = 89
        BuddyPreferences.addTapsCount(context, otherWidgetId, 4)
        assertEquals(4, BuddyPreferences.getCoinsForWidget(context, otherWidgetId))
        assertEquals(4, BuddyPreferences.getWidgetBuddyState(context, otherWidgetId).coins)
    }

    private fun collectSemanticsTexts(activity: Activity): List<String> {
        shadowOf(Looper.getMainLooper()).idle()
        val rootView = activity.window.decorView
        val rootForTest = findRootForTest(rootView)
        assertNotNull("Expected AndroidComposeView (RootForTest) in activity view hierarchy", rootForTest)
        val texts = mutableListOf<String>()
        collectNodeTexts(rootForTest!!.semanticsOwner.unmergedRootSemanticsNode, texts)
        return texts
    }

    private fun findRootForTest(view: View): RootForTest? {
        if (view is RootForTest) return view
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val found = findRootForTest(view.getChildAt(i))
                if (found != null) return found
            }
        }
        return null
    }

    private fun collectNodeTexts(node: SemanticsNode, out: MutableList<String>) {
        node.config.getOrNull(SemanticsProperties.Text)?.forEach { annotatedString ->
            out.add(annotatedString.text)
        }
        node.children.forEach { child ->
            collectNodeTexts(child, out)
        }
    }

    private suspend fun captureWidgetDocument(widgetId: Int): CoreDocument {
        RemoteWidgetActionBridge.clearLambdaActions()
        val widget = AndroidPetWidget()
        val captured = captureSingleRemoteDocument(context = context) {
            CompositionLocalProvider(
                RemoteWidgetActionBridge.getLocalWidget() provides
                    RemoteWidgetActionBridge.createWidgetInformation(widgetId)
            ) {
                widget.Content(context, widgetId)
            }
        }
        val buffer = RemoteComposeBuffer.fromInputStream(ByteArrayInputStream(captured.bytes))
        return CoreDocument().apply {
            initFromBuffer(buffer)
        }
    }

    private fun findComponentByBounds(
        component: Component,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
    ): Component? {
        if (
            component is LayoutComponent &&
            component.x == x &&
            component.y == y &&
            component.width == w &&
            component.height == h
        ) {
            return component
        }
        for (op in component.list) {
            if (op is Component) {
                val found = findComponentByBounds(op, x, y, w, h)
                if (found != null) return found
            }
        }
        return null
    }

    private fun describeTree(component: Component, indent: String = ""): String {
        val sb = StringBuilder()
        if (component is LayoutComponent) {
            sb.appendLine("$indent${component.javaClass.simpleName}(id=${component.componentId}, x=${component.x}, y=${component.y}, w=${component.width}, h=${component.height}, vis=${component.isVisible})")
        } else {
            sb.appendLine("$indent${component.javaClass.simpleName}(id=${component.componentId}, vis=${component.isVisible})")
        }
        for (op in component.list) {
            if (op is Component) {
                sb.append(describeTree(op, "$indent  "))
            } else {
                sb.appendLine("$indent  op: ${op.javaClass.simpleName} ($op)")
            }
        }
        return sb.toString()
    }

    private inner class TestRemoteContext(private val widgetId: Int) : RemoteContext() {
        val triggeredActionIds = mutableListOf<Int>()

        init {
            mWidth = 300f
            mHeight = 300f
            density = 1f
            setAnimationEnabled(false)
            mPaintContext = NoOpPaintContext(this)
        }

        override fun loadPathData(instanceId: Int, winding: Int, floatPath: FloatArray) {
            mRemoteComposeState.putPathData(instanceId, floatPath)
            mRemoteComposeState.putPathWinding(instanceId, winding)
        }
        override fun getPathData(instanceId: Int): FloatArray? =
            mRemoteComposeState.getPathData(instanceId)
        override fun loadVariableName(varName: String, varId: Int, varType: Int) {}
        override fun loadColor(id: Int, color: Int) {
            mRemoteComposeState.updateColor(id, color)
        }
        override fun loadBitmap(
            imageId: Int,
            encoding: Short,
            type: Short,
            width: Int,
            height: Int,
            bitmap: ByteArray,
        ) {}
        override fun loadText(id: Int, text: String) {
            if (!mRemoteComposeState.containsId(id)) {
                mRemoteComposeState.cacheData(id, text)
            } else {
                mRemoteComposeState.updateData(id, text)
            }
        }
        override fun getText(id: Int): String? = mRemoteComposeState.getFromId(id) as? String
        override fun loadFloat(id: Int, value: Float) {
            mRemoteComposeState.updateFloat(id, value)
        }
        override fun overrideFloat(id: Int, value: Float) {
            mRemoteComposeState.overrideFloat(id, value)
        }
        override fun loadInteger(id: Int, value: Int) {
            mRemoteComposeState.updateInteger(id, value)
        }
        override fun overrideInteger(id: Int, value: Int) {
            mRemoteComposeState.overrideInteger(id, value)
        }
        override fun overrideText(id: Int, valueId: Int) {
            val text = getText(valueId) ?: ""
            mRemoteComposeState.overrideData(id, text)
        }
        override fun loadAnimatedFloat(
            id: Int,
            animatedFloat: androidx.compose.remote.core.operations.FloatExpression,
        ) {}
        override fun loadShader(id: Int, value: androidx.compose.remote.core.operations.ShaderData) {}
        override fun getFloat(id: Int): Float = mRemoteComposeState.getFloat(id)
        override fun putObject(id: Int, value: Any) {
            mRemoteComposeState.updateObject(id, value)
        }
        override fun getObject(id: Int): Any? = mRemoteComposeState.getObject(id)
        override fun getInteger(id: Int): Int = mRemoteComposeState.getInteger(id)
        override fun getLong(id: Int): Long = 0L
        override fun getColor(id: Int): Int = mRemoteComposeState.getColor(id)
        override fun listensTo(id: Int, variableSupport: VariableSupport) {
            mRemoteComposeState.listenToVar(id, variableSupport)
        }
        override fun updateOps(): Int =
            mRemoteComposeState.getOpsToUpdate(this, System.currentTimeMillis())
        override fun getShader(id: Int): androidx.compose.remote.core.operations.ShaderData? = null
        override fun addClickArea(
            id: Int,
            contentDescription: Int,
            left: Float,
            top: Float,
            right: Float,
            bottom: Float,
            metadataId: Int,
        ) {}
        override fun runAction(id: Int, metadata: String) {
            triggeredActionIds.add(id)
            RemoteWidgetActionBridge.runLambdaAction(id)
        }
        override fun runNamedAction(id: Int, value: Any?) {
            triggeredActionIds.add(id)
            RemoteWidgetActionBridge.runLambdaAction(id)
        }
        override fun hapticEffect(type: Int) {}
        override fun addCollection(
            id: Int,
            collection: androidx.compose.remote.core.operations.utilities.ArrayAccess,
        ) {}
        override fun putDataMap(
            id: Int,
            map: androidx.compose.remote.core.operations.utilities.DataMap,
        ) {}
        override fun getDataMap(id: Int): androidx.compose.remote.core.operations.utilities.DataMap? = null
        override fun setNamedColorOverride(name: String, value: Int) {}
        override fun setNamedStringOverride(name: String, value: String) {}
        override fun clearNamedStringOverride(name: String) {}
        override fun setNamedBooleanOverride(name: String, value: Boolean) {}
        override fun clearNamedBooleanOverride(name: String) {}
        override fun setNamedIntegerOverride(name: String, value: Int) {}
        override fun clearNamedIntegerOverride(name: String) {}
        override fun setNamedFloatOverride(name: String, value: Float) {}
        override fun clearNamedFloatOverride(name: String) {}
        override fun setNamedLong(name: String, value: Long) {}
        override fun setNamedDataOverride(name: String, value: Any) {}
        override fun clearNamedDataOverride(name: String) {}
    }

    private class NoOpPaintContext(context: RemoteContext) : PaintContext(context) {
        override fun roundedClipRect(
            width: Float,
            height: Float,
            topStart: Float,
            topEnd: Float,
            bottomStart: Float,
            bottomEnd: Float,
        ) {}
        override fun matrixFromPath(pathId: Int, fraction: Float, vOffset: Float, flags: Int) {}
        override fun drawToBitmap(bitmapId: Int, mode: Int, color: Int) {}
        override fun drawBitmap(
            imageId: Int,
            srcLeft: Int,
            srcTop: Int,
            srcRight: Int,
            srcBottom: Int,
            dstLeft: Int,
            dstTop: Int,
            dstRight: Int,
            dstBottom: Int,
            cdId: Int,
        ) {}
        override fun scale(scaleX: Float, scaleY: Float) {}
        override fun translate(translateX: Float, translateY: Float) {}
        override fun drawArc(
            left: Float,
            top: Float,
            right: Float,
            bottom: Float,
            startAngle: Float,
            sweepAngle: Float,
        ) {}
        override fun drawSector(
            left: Float,
            top: Float,
            right: Float,
            bottom: Float,
            startAngle: Float,
            sweepAngle: Float,
        ) {}
        override fun drawBitmap(id: Int, left: Float, top: Float, right: Float, bottom: Float) {}
        override fun drawCircle(centerX: Float, centerY: Float, radius: Float) {}
        override fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float) {}
        override fun drawOval(left: Float, top: Float, right: Float, bottom: Float) {}
        override fun drawPath(id: Int, start: Float, end: Float) {}
        override fun drawRect(left: Float, top: Float, right: Float, bottom: Float) {}
        override fun savePaint() {}
        override fun restorePaint() {}
        override fun replacePaint(paint: PaintBundle) {}
        override fun drawRoundRect(
            left: Float,
            top: Float,
            right: Float,
            bottom: Float,
            radiusX: Float,
            radiusY: Float,
        ) {}
        override fun drawTextOnPath(textId: Int, pathId: Int, hOffset: Float, vOffset: Float) {}
        override fun getTextBounds(
            textId: Int,
            start: Int,
            end: Int,
            flags: Int,
            bounds: FloatArray,
        ) {
            bounds[0] = 0f
            bounds[1] = 0f
            bounds[2] = 50f
            bounds[3] = 20f
        }
        override fun layoutComplexText(
            textId: Int,
            start: Int,
            end: Int,
            alignment: Int,
            overflow: Int,
            maxLines: Int,
            maxWidth: Float,
            maxHeight: Float,
            letterSpacing: Float,
            lineHeightAdd: Float,
            lineHeightMultiplier: Float,
            lineBreakStrategy: Int,
            hyphenationFrequency: Int,
            justificationMode: Int,
            underline: Boolean,
            strikethrough: Boolean,
            flags: Int,
        ): RcPlatformServices.ComputedTextLayout? = null
        override fun drawComplexText(complexTextLayout: RcPlatformServices.ComputedTextLayout?) {}
        override fun drawTextRun(
            textId: Int,
            start: Int,
            end: Int,
            contextStart: Int,
            contextEnd: Int,
            x: Float,
            y: Float,
            rtl: Boolean,
        ) {}
        override fun drawTweenPath(
            path1Id: Int,
            path2Id: Int,
            tween: Float,
            start: Float,
            stop: Float,
        ) {}
        override fun tweenPath(out: Int, path1: Int, path2: Int, tween: Float) {}
        override fun combinePath(out: Int, path1: Int, path2: Int, operation: Byte) {}
        override fun applyPaint(paintData: PaintBundle) {}
        override fun matrixScale(scaleX: Float, scaleY: Float, centerX: Float, centerY: Float) {}
        override fun matrixTranslate(translateX: Float, translateY: Float) {}
        override fun matrixSkew(skewX: Float, skewY: Float) {}
        override fun matrixRotate(rotate: Float, pivotX: Float, pivotY: Float) {}
        override fun matrixSave() {}
        override fun matrixRestore() {}
        override fun clipRect(left: Float, top: Float, right: Float, bottom: Float) {}
        override fun clipPath(pathId: Int, regionOp: Int) {}
        override fun reset() {}
        override fun startGraphicsLayer(w: Int, h: Int) {}
        override fun setGraphicsLayer(attributes: HashMap<Int, Any>) {}
        override fun endGraphicsLayer() {}
        override fun getText(textId: Int): String? = mContext.getText(textId)
        override fun log(content: String) {}
    }
}
