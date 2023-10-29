package chat.echo.app.views.chat

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Send
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.echo.app.R
import chat.echo.app.SimplexApp.Companion.context
import chat.echo.app.model.ChatModel
import chat.echo.app.model.durationText
import chat.echo.app.ui.theme.AnnouncementText
import chat.echo.app.views.helpers.*
import com.linc.audiowaveform.AudioWaveform
import com.linc.audiowaveform.model.AmplitudeType
import com.linc.audiowaveform.model.WaveformAlignment
import linc.com.amplituda.*
import java.io.File

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun RecordVoiceMessageView(
  chatModel: ChatModel,
  composeState: MutableState<ComposeState>,
  recState: MutableState<RecordingState>,
  sendMessage: () -> Unit,
  stopRecordingAndAddAudio: () -> Unit
) {
  val recordingProgress = remember { mutableStateOf(0) }
  val state = recState.value

  if (state is RecordingState.Started) {
    recordingProgress.value = state.progressMs
    StartRecordingView(progress = state.progressMs, stopRecordingAndAddAudio)
  } else if (state is RecordingState.Finished) {
    PreviewRecordingView(chatModel = chatModel, composeState = composeState, recState = recState, durationMs = state.durationMs, path = state.filePath, sendMessage)
  }
}

@Composable
fun StartRecordingView(
  progress: Int,
  stopRecordingAndAddAudio: () -> Unit
) {
  Column() {
    Row(
      modifier = Modifier
        .padding(top = 5.dp, start = 15.dp, bottom = 5.dp, end = 15.dp)
        .fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text(
        duration(progress),
        Modifier.padding(end = 5.dp),
        fontSize = 18.sp,
        color = AnnouncementText
      )
      Text(
        generalGetString(R.string.recording_voice_message),
        Modifier.padding(start = 5.dp),
        fontSize = 18.sp,
        color = AnnouncementText
      )
      IconButton(stopRecordingAndAddAudio, Modifier.size(36.dp)) {
        Icon(
          Icons.Filled.StopCircle,
          stringResource(R.string.stop_recording_voice_message),
          tint = Color.Red,
          modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .size(60.dp)
            .padding(4.dp)
        )
      }
    }
  }
}

@Composable
fun PreviewRecordingView(
  chatModel: ChatModel,
  composeState: MutableState<ComposeState>,
  recState: MutableState<RecordingState>,
  durationMs: Int,
  path: String,
  sendMessage: () -> Unit
) {
  val progress = rememberSaveable { mutableStateOf(0) }
  val duration = rememberSaveable(durationMs) { mutableStateOf(durationMs) }
  var waveformProgress by remember { mutableStateOf(0.5f) }
  val amplituda = Amplituda(context)
  val amplitudeResult: MutableState<AmplitudaResult<String>?> = remember {
    mutableStateOf(null)
  }
  amplituda.setLogConfig(Log.ERROR, true)
  amplituda.processAudio(
    path,
    Compress.withParams(Compress.AVERAGE, 1),
    Cache.withParams(Cache.REUSE),
    object: AmplitudaProgressListener() {
      override fun onStartProgress() {
        super.onStartProgress()
        println("Start Progress")
      }

      override fun onStopProgress() {
        super.onStopProgress()
        println("Stop Progress")
      }

      override fun onProgress(operation: ProgressOperation, progress: Int) {
        val currentOperation = when (operation) {
          ProgressOperation.PROCESSING -> "Process audio"
          ProgressOperation.DECODING -> "Decode audio"
          ProgressOperation.DOWNLOADING -> "Download audio from url"
        }
        println("$currentOperation: $progress%")
      }
    }
  ).get({ result ->
    amplitudeResult.value = result
    printResult(result)
  }, { exception -> exception.printStackTrace() })
  val audioPlaying = rememberSaveable { mutableStateOf(false) }
  val numberInText = remember(durationMs, progress.value) {
    derivedStateOf {
      when {
        progress.value == 0 && !audioPlaying.value -> duration.value / 1000
        audioPlaying.value -> progress.value / 1000
        else -> durationMs / 1000
      }
    }
  }

  Row(
    modifier = Modifier
      .padding(top = 5.dp, start = 15.dp, bottom = 5.dp, end = 15.dp)
      .fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    IconButton(onClick = {
      AudioPlayer.stop(path)
      cancelVoice(composeState, recState)
    }, Modifier.size(36.dp)) {
      Icon(
        Icons.Outlined.Delete,
        stringResource(R.string.stop_recording_voice_message),
        tint = Color.Black,
        modifier = Modifier
          .size(40.dp)
          .clip(CircleShape)
          .size(60.dp)
          .padding(4.dp)
      )
    }
    IconButton(onClick = {
      if (!audioPlaying.value) {
        AudioPlayer.play(path, audioPlaying, progress, duration, false)
      } else {
        AudioPlayer.pause(audioPlaying, progress)
      }
    }, Modifier.size(36.dp)) {
      Icon(
        if (!audioPlaying.value) Icons.Filled.PlayCircle else Icons.Filled.PauseCircle,
        stringResource(R.string.stop_recording_voice_message),
        tint = Color.Black,
        modifier = Modifier
          .size(40.dp)
          .clip(CircleShape)
          .size(60.dp)
          .padding(4.dp)
      )
    }
    Text(
      durationText(numberInText.value),
      Modifier.padding(end = 5.dp),
      fontSize = 18.sp,
      color = AnnouncementText
    )
    amplitudeResult.value?.let {
      AudioWaveform(
        modifier = Modifier.fillMaxWidth()
          .weight(1f),
        // Spike DrawStyle: Fill or Stroke
        style = Fill,
        waveformAlignment = WaveformAlignment.Center,
        amplitudeType = AmplitudeType.Max,
        // Colors could be updated with Brush API
        progressBrush = SolidColor(Color.Black),
        waveformBrush = SolidColor(Color.LightGray),
        spikeWidth = 4.dp,
        spikePadding = 2.dp,
        spikeRadius = 4.dp,
        progress = progress.value.toFloat() / durationMs.toFloat(),
        amplitudes = it.amplitudesAsList(),
        onProgressChange = {},
        onProgressChangeFinished = {
          //waveformProgress.value = 0f
        }
      )
    }
    IconButton(onClick = {
      recState.value = RecordingState.NotStarted
      onAudioAdded(chatModel, composeState, path, durationMs, true)
      sendMessage()
    }, Modifier.size(36.dp)) {
      Icon(
        Icons.Outlined.Send,
        stringResource(R.string.icon_descr_send_message),
        tint = Color.Black,
        modifier = Modifier
          .size(40.dp)
          .clip(CircleShape)
          .size(60.dp)
          .padding(4.dp)
      )
    }
  }
}

fun cancelVoice(
  composeState: MutableState<ComposeState>,
  recState: MutableState<RecordingState>,
) {
  val filePath = recState.value.filePathNullable
  recState.value = RecordingState.NotStarted
  composeState.value = composeState.value.copy(preview = ComposePreview.NoPreview)
  withBGApi {
    RecorderNative.stopRecording?.invoke()
    AudioPlayer.stop(filePath)
    filePath?.let { File(it).delete() }
  }
}

fun onAudioAdded(chatModel: ChatModel,
  composeState: MutableState<ComposeState>,
  filePath: String,
  durationMs: Int,
  finished: Boolean) {
  val file = File(filePath)
  chatModel.filesToDelete.add(file)
  composeState.value = composeState.value.copy(preview = ComposePreview.VoicePreview(filePath, durationMs, finished))
}

private fun printResult(result: AmplitudaResult<*>) {
  println(
    """
        Audio info:
        millis = ${result.getAudioDuration(AmplitudaResult.DurationUnit.MILLIS)}
        seconds = ${result.getAudioDuration(AmplitudaResult.DurationUnit.SECONDS)}
        
        source = ${if (result.getInputAudioType() == InputAudio.Type.FILE) (result.getAudioSource() as File).absolutePath else result.getAudioSource()}
        source type = ${result.getInputAudioType().name}
        
        Amplitudes:
        size: = ${result.amplitudesAsList().size}
        list: = ${result.amplitudesAsList()}
        amplitudes for second 1: = ${result.amplitudesForSecond(1)}
        json: = ${result.amplitudesAsJson()}
        single line sequence = ${result.amplitudesAsSequence(AmplitudaResult.SequenceFormat.SINGLE_LINE)}
        new line sequence = ${result.amplitudesAsSequence(AmplitudaResult.SequenceFormat.NEW_LINE)}
        custom delimiter sequence = ${result.amplitudesAsSequence(AmplitudaResult.SequenceFormat.SINGLE_LINE, " * ")}
    """.trimIndent()
  )
}

fun duration(millis: Int): String {
  val seconds = (millis / 1000) % 60
  val minutes = (millis / (1000 * 60)) % 60

  return String.format("%02d:%02d", minutes, seconds)
}
