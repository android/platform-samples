/*
 *
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.google.uwb.hellouwb.ui.home

import androidx.core.uwb.RangingMeasurement
import androidx.core.uwb.RangingPosition
import androidx.core.uwb.SensorFusionResult
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.uwb.hellouwb.data.UwbRangingControlSource
import com.google.uwb.uwbranging.EndpointEvents
import com.google.uwb.uwbranging.UwbEndpoint
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*

class HomeViewModel(uwbRangingControlSource: UwbRangingControlSource) : ViewModel() {

  private val _uiState: MutableStateFlow<HomeUiState> =
    MutableStateFlow(HomeUiStateImpl(listOf(), listOf(), false, false, null))

  private val endpoints = mutableListOf<UwbEndpoint>()
  private val endpointPositions = mutableMapOf<UwbEndpoint, RangingPosition>()
  private val endpointEstimateStates = mutableMapOf<UwbEndpoint, EstimateState>()
  private var isRanging = false
  private var isSensorFusionFallback = false
  private var fallbackReason: String? = null

  private fun updateUiState(): HomeUiState {
    return HomeUiStateImpl(
      endpoints
        .mapNotNull { endpoint ->
          endpointPositions[endpoint]?.let { position ->
            ConnectedEndpoint(
              endpoint,
              position,
              endpointEstimateStates[endpoint] ?: EstimateState.Precise,
            )
          }
        }
        .toList(),
      endpoints.filter { !endpointPositions.containsKey(it) }.toList(),
      isRanging,
      isSensorFusionFallback,
      fallbackReason,
    )
  }

  val uiState = _uiState.asStateFlow()

  init {
    uwbRangingControlSource
      .observeRangingResults()
      .onEach { result ->
        when (result) {
          is EndpointEvents.EndpointFound -> endpoints.add(result.endpoint)
          is EndpointEvents.UwbDisconnected -> {
            endpointPositions.remove(result.endpoint)
            endpointEstimateStates.remove(result.endpoint)
          }
          is EndpointEvents.PositionUpdated -> {
            endpointPositions[result.endpoint] = result.position
            endpointEstimateStates.remove(result.endpoint)
          }
          is EndpointEvents.SensorFusionEstimateUpdated -> {
            isSensorFusionFallback = false
            fallbackReason = null
            when (val estimate = result.estimate) {
              is SensorFusionResult.PreciseEstimate -> {
                endpointPositions[result.endpoint] = estimateToBoresightPosition(estimate)
                endpointEstimateStates[result.endpoint] = EstimateState.Precise
              }
              is SensorFusionResult.DriftingEstimate -> {
                endpointPositions[result.endpoint] = estimateToBoresightPosition(estimate)
                endpointEstimateStates[result.endpoint] = EstimateState.Drifting
              }
              is SensorFusionResult.ImpreciseEstimate -> {
                endpointPositions[result.endpoint] = estimateToBoresightPosition(estimate)
                endpointEstimateStates[result.endpoint] =
                  EstimateState.Imprecise(estimateFailureReasonToString(estimate.reason))
              }
            }
          }
          is EndpointEvents.SensorFusionFallback -> {
            isSensorFusionFallback = true
            fallbackReason = fallbackReasonToString(result.fallback.reason)
          }
          is EndpointEvents.EndpointLost -> {
            endpoints.remove(result.endpoint)
            endpointPositions.remove(result.endpoint)
            endpointEstimateStates.remove(result.endpoint)
          }
          else -> return@onEach
        }
        _uiState.update { updateUiState() }
      }
      .launchIn(viewModelScope)

    uwbRangingControlSource.isRunning
      .onEach { running ->
        isRanging = running
        if (!running) {
          endpoints.clear()
          endpointPositions.clear()
          endpointEstimateStates.clear()
          isSensorFusionFallback = false
          fallbackReason = null
        }
        _uiState.update { updateUiState() }
      }
      .launchIn(CoroutineScope(Dispatchers.IO))
  }

  private data class HomeUiStateImpl(
    override val connectedEndpoints: List<ConnectedEndpoint>,
    override val disconnectedEndpoints: List<UwbEndpoint>,
    override val isRanging: Boolean,
    override val isSensorFusionFallback: Boolean,
    override val fallbackReason: String?,
  ) : HomeUiState

  companion object {
    internal fun estimateToBoresightPosition(estimate: SensorFusionResult.Estimate): RangingPosition {
      if (estimate is SensorFusionResult.ImpreciseEstimate) {
        return RangingPosition(
          estimate.distance,
          RangingMeasurement(0.0f),
          null,
          estimate.elapsedRealtimeNanos,
        )
      }
      val azimuth = estimate.azimuth
      val elevation = estimate.elevation
      if (azimuth != null && elevation != null) {
        val azRad = Math.toRadians(azimuth.value.toDouble())
        val elRad = Math.toRadians(elevation.value.toDouble())
        val x = cos(elRad) * sin(azRad)
        val y = cos(elRad) * cos(azRad)
        val z = sin(elRad)
        val boresightAzimuthDeg = Math.toDegrees(atan2(x, -z)).toFloat()
        val boresightElevationDeg = Math.toDegrees(asin(y.coerceIn(-1.0, 1.0))).toFloat()
        return RangingPosition(
          estimate.distance,
          RangingMeasurement(boresightAzimuthDeg),
          RangingMeasurement(boresightElevationDeg),
          estimate.elapsedRealtimeNanos,
        )
      }
      return RangingPosition(
        estimate.distance,
        azimuth,
        elevation,
        estimate.elapsedRealtimeNanos,
      )
    }

    fun provideFactory(
      uwbRangingControlSource: UwbRangingControlSource
    ): ViewModelProvider.Factory =
      object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
          return HomeViewModel(uwbRangingControlSource) as T
        }
      }

    internal fun estimateFailureReasonToString(reason: Int): String =
      when (reason) {
        SensorFusionResult.ESTIMATE_FAILURE_REASON_NOT_AVAILABLE ->
          "ESTIMATE_FAILURE_REASON_NOT_AVAILABLE"
        SensorFusionResult.ESTIMATE_FAILURE_REASON_INSUFFICIENT_LIGHT ->
          "ESTIMATE_FAILURE_REASON_INSUFFICIENT_LIGHT"
        SensorFusionResult.ESTIMATE_FAILURE_REASON_EXCESSIVE_MOTION ->
          "ESTIMATE_FAILURE_REASON_EXCESSIVE_MOTION"
        SensorFusionResult.ESTIMATE_FAILURE_REASON_INSUFFICIENT_FEATURES ->
          "ESTIMATE_FAILURE_REASON_INSUFFICIENT_FEATURES"
        SensorFusionResult.ESTIMATE_FAILURE_REASON_CAMERA_UNAVAILABLE ->
          "ESTIMATE_FAILURE_REASON_CAMERA_UNAVAILABLE"
        SensorFusionResult.ESTIMATE_FAILURE_REASON_PEER_MOVING ->
          "ESTIMATE_FAILURE_REASON_PEER_MOVING"
        SensorFusionResult.ESTIMATE_FAILURE_REASON_INCONCLUSIVE_RESULT ->
          "ESTIMATE_FAILURE_REASON_INCONCLUSIVE_RESULT"
        SensorFusionResult.ESTIMATE_FAILURE_REASON_BAD_STATE ->
          "ESTIMATE_FAILURE_REASON_BAD_STATE"
        else -> "UNKNOWN ($reason)"
      }

    internal fun fallbackReasonToString(reason: Int): String =
      when (reason) {
        SensorFusionResult.FALLBACK_REASON_NOT_AVAILABLE ->
          "FALLBACK_REASON_NOT_AVAILABLE"
        SensorFusionResult.FALLBACK_REASON_HARDWARE_AOA_PRECEDENCE ->
          "FALLBACK_REASON_HARDWARE_AOA_PRECEDENCE"
        SensorFusionResult.FALLBACK_REASON_ARCORE_CAMERA_NOT_AVAILABLE ->
          "FALLBACK_REASON_ARCORE_CAMERA_NOT_AVAILABLE"
        SensorFusionResult.FALLBACK_REASON_ARCORE_SDK_TOO_OLD ->
          "FALLBACK_REASON_ARCORE_SDK_TOO_OLD"
        SensorFusionResult.FALLBACK_REASON_ARCORE_DEVICE_NOT_COMPATIBLE ->
          "FALLBACK_REASON_ARCORE_DEVICE_NOT_COMPATIBLE"
        SensorFusionResult.FALLBACK_REASON_ARCORE_MISSING_GL_CONTEXT ->
          "FALLBACK_REASON_ARCORE_MISSING_GL_CONTEXT"
        else -> "UNKNOWN ($reason)"
      }
  }
}

sealed interface EstimateState {
  object Precise : EstimateState

  object Drifting : EstimateState

  data class Imprecise(val reason: String) : EstimateState
}

interface HomeUiState {
  val connectedEndpoints: List<ConnectedEndpoint>
  val disconnectedEndpoints: List<UwbEndpoint>
  val isRanging: Boolean
  val isSensorFusionFallback: Boolean
    get() = false
  val fallbackReason: String?
    get() = null
}

data class ConnectedEndpoint(
  val endpoint: UwbEndpoint,
  val position: RangingPosition,
  val estimateState: EstimateState = EstimateState.Precise,
)
