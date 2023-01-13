package com.example.platform.connectivity.audio

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.platform.connectivity.audio.datasource.PlatformAudioSource
import com.example.platform.connectivity.audio.viewmodel.AudioDeviceUI
import com.example.platform.connectivity.audio.viewmodel.AudioDeviceViewModel
import com.example.platform.connectivity.audio.viewmodel.getDeviceName
import com.example.platform.connectivity.audio.viewmodel.getStatusColor
import com.google.android.catalog.framework.annotations.Sample

@Sample(
    name = "Audio Manager",
    description = "This sample will show you how get all audio sources and set an audio device. Covers Bluetooth, LEA, Wired and internal speakers"
)
@Composable
fun AudioSample() {
  val context = LocalContext.current

  val audioManager: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
  val viewModel = AudioDeviceViewModel(PlatformAudioSource(audioManager))
  AudioSampleScreen(viewModel)
}

@Composable
fun AudioSampleScreen(viewModel: AudioDeviceViewModel) {
  val uiStateAvailableDevices by viewModel.availableDeviceUiState.collectAsState()
  val uiStateActiveDevice by viewModel.activeDeviceUiState.collectAsState()
  val uiStateErrorMessage by viewModel.errorUiState.collectAsState()

  uiStateErrorMessage?.let {
      Toast.makeText(LocalContext.current, uiStateErrorMessage, Toast.LENGTH_LONG).show()
      viewModel.onErrorMessageShown()
  }

  Column {
    ActiveAudioSource(uiStateActiveDevice)
    Text(stringResource(id = R.string.selectdevice),
         modifier = Modifier
           .padding(8.dp, 12.dp),
         style = MaterialTheme.typography.displayMedium)
    AvailableDevicesList(uiStateAvailableDevices, viewModel::setAudioDevice)
  }
}

@Composable
fun AvailableDevicesList(audioDeviceWidgetUiState: AudioDeviceViewModel.AudioDeviceListUiState, onDeviceSelected: (AudioDeviceInfo) -> Unit){
    when(audioDeviceWidgetUiState){
        AudioDeviceViewModel.AudioDeviceListUiState.Loading -> {}
        is AudioDeviceViewModel.AudioDeviceListUiState.Success -> {
            ListOfAudioDevices(audioDeviceWidgetUiState.audioDevices, onDeviceSelected)
        }
    }
}

@Composable
fun ActiveAudioSource(activeAudioDeviceUiState: AudioDeviceViewModel.ActiveAudioDeviceUiState) =
    when(activeAudioDeviceUiState){
        AudioDeviceViewModel.ActiveAudioDeviceUiState.NotActive -> {
            ActiveAudioSource(stringResource(id = R.string.nodevice), "", R.drawable.phone_icon)
        }
        is AudioDeviceViewModel.ActiveAudioDeviceUiState.OnActiveDevice -> {
            ActiveAudioSource(stringResource(id = R.string.connected),
                              activeAudioDeviceUiState.audioDevice.getDeviceName(),
                              activeAudioDeviceUiState.audioDevice.resIconId)
        }
    }

/**
 * Shows user the active audio source
 */
@Composable
fun ActiveAudioSource(title: String, subTitle: String, resId: Int){
    Row(verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(12.dp, 24.dp)){
        Icon(painterResource(resId),
             contentDescription = null,
             tint = MaterialTheme.colorScheme.primary)
        Column {
            Text(title, modifier = Modifier.padding(8.dp, 0.dp),
                 color = MaterialTheme.colorScheme.primary,
                 style = MaterialTheme.typography.headlineMedium)
            Text(subTitle, modifier = Modifier.padding(8.dp, 0.dp),
                 color = MaterialTheme.colorScheme.primary,
                 style = MaterialTheme.typography.headlineSmall)
        }
    }
}

/**
 * Build an list of Audio Devices we can connect to
 */
@Composable
fun ListOfAudioDevices(devices: List<AudioDeviceUI>, onDeviceSelected: (AudioDeviceInfo) -> Unit) {
    LazyColumn() {
        items(devices) { item ->
            AudioItem(audioDevice = item, onDeviceSelected = onDeviceSelected)
        }
    }
}

/**
 * Displays the audio device with Icon and Text
 */
@Composable
fun AudioItem(
    audioDevice : AudioDeviceUI,
    onDeviceSelected: (AudioDeviceInfo) -> Unit
){
    Box(modifier = Modifier
      .fillMaxWidth()
      .clickable { onDeviceSelected(audioDevice.audioDeviceInfo) }){
        Row(modifier = Modifier.padding(12.dp, 8.dp),
            verticalAlignment = Alignment.CenterVertically,) {
            Icon(painterResource(audioDevice.resIconId),
                 contentDescription = null,
                 tint = Color.White)
            Column {
                Text(audioDevice.getDeviceName(),
                     modifier = Modifier
                    .padding(8.dp, 0.dp),
                     audioDevice.getStatusColor())
                Text("",
                     modifier = Modifier.
                     padding(8.dp, 0.dp),
                     audioDevice.getStatusColor())
            }
        }
    }
}
