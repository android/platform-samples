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

package com.example.platform.ui.appwidgets.remote_compose;

import android.annotation.SuppressLint;
import androidx.compose.remote.creation.compose.action.Action;
import androidx.compose.remote.creation.compose.widgets.WidgetInformation;
import androidx.compose.remote.creation.compose.widgets.WidgetInformationKt;
import androidx.compose.remote.creation.compose.widgets.WidgetLambdaAction;
import androidx.compose.runtime.ProvidableCompositionLocal;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;

/**
 * Bridge for combining a host {@link WidgetLambdaAction} with local in-document Remote Compose
 * state actions inside a single click modifier.
 */
@SuppressLint({"RestrictedApiAndroidX", "RestrictedApi"})
public final class RemoteWidgetActionBridge {
    private RemoteWidgetActionBridge() {}

    public static Action createLambdaAction(int widgetId, Function0<Unit> action) {
        return new WidgetLambdaAction(widgetId, action);
    }

    public static ProvidableCompositionLocal<WidgetInformation> getLocalWidget() {
        return WidgetInformationKt.getLocalWidget();
    }

    public static WidgetInformation createWidgetInformation(int widgetId) {
        return new WidgetInformation(widgetId);
    }

    public static void clearLambdaActions() {
        WidgetLambdaAction.Companion.clear();
    }

    public static void resetLambdaCounter() {
        WidgetLambdaAction.Companion.setCounter(0);
    }

    public static java.util.Map<Integer, WidgetLambdaAction> snapshotLambdaActions() {
        return new java.util.HashMap<>(WidgetLambdaAction.Companion.getMap());
    }

    public static void restoreLambdaActions(java.util.Map<Integer, WidgetLambdaAction> snapshot) {
        if (snapshot != null) {
            WidgetLambdaAction.Companion.getMap().putAll(snapshot);
        }
    }

    public static void runLambdaAction(int actionId) {
        WidgetLambdaAction.Companion.run(actionId);
    }
}
