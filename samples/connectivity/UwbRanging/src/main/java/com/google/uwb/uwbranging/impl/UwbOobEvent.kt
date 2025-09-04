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

package com.google.uwb.uwbranging.impl

import androidx.core.uwb.UwbAddress
import androidx.core.uwb.UwbClientSessionScope
import androidx.core.uwb.UwbComplexChannel
import com.google.uwb.uwbranging.UwbEndpoint

/** Events that happen during UWB OOB Connections. */
internal abstract class UwbOobEvent private constructor() {

  abstract val endpoint: UwbEndpoint

  /** An event that notifies an endpoint is found through OOB. */
  data class UwbEndpointFound(
      override val endpoint: UwbEndpoint,
      val configId: Int,
      val endpointAddress: UwbAddress,
      val complexChannel: UwbComplexChannel,
      val sessionId: Int,
      val sessionKeyInfo: ByteArray,
      val sessionScope: UwbClientSessionScope,
      val subSessionid: Int,
      val subSessionKeyInfo: ByteArray?,
  ) : UwbOobEvent() {
      override fun equals(other: Any?): Boolean {
          if (this === other) return true
          if (other !is UwbEndpointFound) return false

          if (configId != other.configId) return false
          if (sessionId != other.sessionId) return false
          if (subSessionid != other.subSessionid) return false
          if (endpoint != other.endpoint) return false
          if (endpointAddress != other.endpointAddress) return false
          if (complexChannel != other.complexChannel) return false
          if (!sessionKeyInfo.contentEquals(other.sessionKeyInfo)) return false
          if (sessionScope != other.sessionScope) return false
          if (!subSessionKeyInfo.contentEquals(other.subSessionKeyInfo)) return false

          return true
      }

      override fun hashCode(): Int {
          var result = configId
          result = 31 * result + sessionId
          result = 31 * result + subSessionid
          result = 31 * result + endpoint.hashCode()
          result = 31 * result + endpointAddress.hashCode()
          result = 31 * result + complexChannel.hashCode()
          result = 31 * result + sessionKeyInfo.contentHashCode()
          result = 31 * result + sessionScope.hashCode()
          result = 31 * result + (subSessionKeyInfo?.contentHashCode() ?: 0)
          return result
      }
  }

    /** An event that notifies a UWB endpoint is lost. */
  data class UwbEndpointLost(override val endpoint: UwbEndpoint) : UwbOobEvent()

  /** Notifies that a message is received. */
  data class MessageReceived(override val endpoint: UwbEndpoint, val message: ByteArray) :
    UwbOobEvent() {
      override fun equals(other: Any?): Boolean {
          if (this === other) return true
          if (other !is MessageReceived) return false

          if (endpoint != other.endpoint) return false
          if (!message.contentEquals(other.message)) return false

          return true
      }

      override fun hashCode(): Int {
          var result = endpoint.hashCode()
          result = 31 * result + message.contentHashCode()
          return result
      }
  }
}
