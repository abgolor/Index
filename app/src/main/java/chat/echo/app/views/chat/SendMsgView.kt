package chat.echo.app.views.chat

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.text.InputType
import android.util.Log
import android.view.*
import android.view.inputmethod.*
import android.widget.EditText
import android.widget.TextView
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.text.BidiFormatter
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.core.widget.doAfterTextChanged
import androidx.core.widget.doOnTextChanged
import chat.echo.app.R
import chat.echo.app.SimplexApp
import chat.echo.app.model.ChatItem
import chat.echo.app.model.SharedPreference
import chat.echo.app.ui.theme.*
import chat.echo.app.views.chat.item.ItemAction
import chat.echo.app.views.helpers.*
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.*
import java.lang.reflect.Field
import kotlin.math.min

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SendMsgView(
  composeState: MutableState<ComposeState>,
  sendMessage: () -> Unit,
  onMessageChange: (String) -> Unit,
  textStyle: MutableState<TextStyle>
) {
  val cs = composeState.value
  val focusRequester = remember { FocusRequester() }
  val keyboard = LocalSoftwareKeyboardController.current
  LaunchedEffect(cs.contextItem) {
    if (cs.contextItem !is ComposeContextItem.QuotedItem) return@LaunchedEffect
    // In replying state
    focusRequester.requestFocus()
    delay(50)
    keyboard?.show()
  }
  val isRtl = remember(cs.message) { BidiFormatter.getInstance().isRtl(cs.message.subSequence(0, min(50, cs.message.length))) }
  Card(
    shape = RoundedCornerShape(15.dp),
    backgroundColor = SendMsgBackground
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      BasicTextField(
        value = cs.message,
        onValueChange = onMessageChange,
        decorationBox = { innerTextField ->
          CompositionLocalProvider(
            LocalLayoutDirection provides if (isRtl) LayoutDirection.Rtl else LocalLayoutDirection.current
          ) {
            Box(
              contentAlignment = Alignment.CenterStart
            ) {
              if (cs.message.isEmpty()) {
                Text(
                  text = generalGetString(R.string.message),
                  color = Color.Black,
                  fontSize = 15.sp
                )
              }
              innerTextField()
            }
          }
        },
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .focusRequester(focusRequester)
          .padding(start = 10.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
        textStyle = MaterialTheme.typography.body1.copy(color = Color.Black, fontSize = 15.sp),
        keyboardOptions = KeyboardOptions(
          capitalization = KeyboardCapitalization.Sentences,
          autoCorrect = true
        ),
        maxLines = 5,
        cursorBrush = SolidColor(HighOrLowlight)
      )
    }
  }
/* BasicTextField(
    value = cs.message,
    onValueChange = onMessageChange,
    textStyle = textStyle.value,
    maxLines = 16,
    keyboardOptions = KeyboardOptions.Default.copy(
      capitalization = KeyboardCapitalization.Sentences,
      autoCorrect = true
    ),
    modifier = Modifier.padding(vertical = 8.dp).focusRequester(focusRequester),
    cursorBrush = SolidColor(HighOrLowlight),
    decorationBox = { innerTextField ->
      Surface(
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colors.secondary)
      ) {
        Row(
          Modifier.background(MaterialTheme.colors.background),
          verticalAlignment = Alignment.Bottom
        ) {
          CompositionLocalProvider(
            LocalLayoutDirection provides if (isRtl) LayoutDirection.Rtl else LocalLayoutDirection.current
          ) {
            Box(
              Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
                .padding(top = 5.dp)
                .padding(bottom = 7.dp)
            ) {
              innerTextField()
            }
          }
          val icon = if (cs.editing) Icons.Filled.Check else Icons.Outlined.ArrowUpward
          val color = if (cs.sendEnabled()) MaterialTheme.colors.primary else HighOrLowlight
          if (cs.inProgress
            && (cs.preview is ComposePreview.ImagePreview || cs.preview is ComposePreview.FilePreview)
          ) {
            CircularProgressIndicator(
              Modifier
                .size(36.dp)
                .padding(4.dp),
              color = HighOrLowlight,
              strokeWidth = 3.dp
            )
          } else {
            Icon(
              icon,
              stringResource(R.string.icon_descr_send_message),
              tint = Color.White,
              modifier = Modifier
                .size(36.dp)
                .padding(4.dp)
                .clip(CircleShape)
                .background(color)
                .clickable {
                  if (cs.sendEnabled()) {
                    sendMessage()
                  }
                }
            )
          }
        }
      }
    }
  )*/
}

@Composable
fun SendMsgView(
  composeState: MutableState<ComposeState>,
  showVoiceRecordIcon: Boolean,
  recState: MutableState<RecordingState>,
  isDirectChat: Boolean,
  liveMessageAlertShown: SharedPreference<Boolean>,
  needToAllowVoiceToContact: Boolean,
  allowedVoiceByPrefs: Boolean,
  userIsObserver: Boolean,
  userCanSend: Boolean,
  allowVoiceToContact: () -> Unit,
  sendMessage: () -> Unit,
  sendLiveMessage: (suspend () -> Unit)? = null,
  updateLiveMessage: (suspend () -> Unit)? = null,
  cancelLiveMessage: (() -> Unit)? = null,
  onMessageChange: (String) -> Unit,
  textStyle: MutableState<TextStyle>
) {
  Box(Modifier.padding(vertical = 8.dp)) {
    val cs = composeState.value
    val showProgress = cs.inProgress && (cs.preview is ComposePreview.MediaPreview || cs.preview is ComposePreview.FilePreview)
    val showVoiceButton = cs.message.isEmpty() && showVoiceRecordIcon && !composeState.value.editing &&
        cs.liveMessage == null && (cs.preview is ComposePreview.NoPreview || recState.value is RecordingState.Started)
    val showDeleteTextButton = rememberSaveable { mutableStateOf(false) }
    NativeKeyboard(composeState, textStyle, showDeleteTextButton, userIsObserver, onMessageChange)
    // Disable clicks on text field
    if (cs.preview is ComposePreview.VoicePreview || !userCanSend) {
      Box(Modifier
        .matchParentSize()
        .clickable(enabled = !userCanSend, indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = {
          AlertManager.shared.showAlertMsg(
            title = generalGetString(R.string.observer_cant_send_message_title),
            text = generalGetString(R.string.observer_cant_send_message_desc)
          )
        })
      )
    }
    if (showDeleteTextButton.value) {
      DeleteTextButton(composeState)
    }
    Box(Modifier.align(Alignment.BottomEnd)) {
      val sendButtonSize = remember { Animatable(36f) }
      val sendButtonAlpha = remember { Animatable(1f) }
      val permissionsState = rememberMultiplePermissionsState(listOf(Manifest.permission.RECORD_AUDIO))
      val scope = rememberCoroutineScope()
      LaunchedEffect(Unit) {
        // Making LiveMessage alive when screen orientation was changed
        if (cs.liveMessage != null && sendLiveMessage != null && updateLiveMessage != null) {
          startLiveMessage(scope, sendLiveMessage, updateLiveMessage, sendButtonSize, sendButtonAlpha, composeState, liveMessageAlertShown)
        }
      }
      when {
        showProgress -> ProgressIndicator()
        showVoiceButton -> {
          Row(verticalAlignment = Alignment.CenterVertically) {
            val stopRecOnNextClick = remember { mutableStateOf(false) }
            when {
              needToAllowVoiceToContact || !allowedVoiceByPrefs || !userCanSend -> {
                DisallowedVoiceButton(userCanSend) {
                  if (needToAllowVoiceToContact) {
                    showNeedToAllowVoiceAlert(allowVoiceToContact)
                  } else if (!allowedVoiceByPrefs) {
                    showDisabledVoiceAlert(isDirectChat)
                  }
                }
              }
              !permissionsState.allPermissionsGranted ->
                VoiceButtonWithoutPermission { permissionsState.launchMultiplePermissionRequest() }
              else ->
                RecordVoiceView(recState, stopRecOnNextClick)
            }
            if (sendLiveMessage != null
              && updateLiveMessage != null
              && (cs.preview !is ComposePreview.VoicePreview || !stopRecOnNextClick.value)
              && cs.contextItem is ComposeContextItem.NoContextItem) {
              Spacer(Modifier.width(10.dp))
              StartLiveMessageButton(userCanSend) {
                if (composeState.value.preview is ComposePreview.NoPreview) {
                  startLiveMessage(scope, sendLiveMessage, updateLiveMessage, sendButtonSize, sendButtonAlpha, composeState, liveMessageAlertShown)
                }
              }
            }
          }
        }
        cs.liveMessage?.sent == false && cs.message.isEmpty() -> {
          CancelLiveMessageButton {
            cancelLiveMessage?.invoke()
          }
        }
        else -> {
          val cs = composeState.value
          val icon = if (cs.editing || cs.liveMessage != null) Icons.Filled.Check else Icons.Outlined.ArrowUpward
          val disabled = !cs.sendEnabled() ||
              (!allowedVoiceByPrefs && cs.preview is ComposePreview.VoicePreview) ||
              cs.endLiveDisabled
          if (cs.liveMessage == null &&
            cs.preview !is ComposePreview.VoicePreview && !cs.editing &&
            cs.contextItem is ComposeContextItem.NoContextItem &&
            sendLiveMessage != null && updateLiveMessage != null
          ) {
            val showDropdown = rememberSaveable { mutableStateOf(false) }
            SendMsgButton(icon, sendButtonSize, sendButtonAlpha, !disabled, sendMessage) { showDropdown.value = true }

            DefaultDropdownMenu(
              showDropdown,
            ) {
              ItemAction(
                generalGetString(R.string.send_live_message),
                Icons.Filled.Bolt,
                onClick = {
                  startLiveMessage(scope, sendLiveMessage, updateLiveMessage, sendButtonSize, sendButtonAlpha, composeState, liveMessageAlertShown)
                  showDropdown.value = false
                }
              )
            }
          } else {
            if(cs.inProgress){
              CircularProgressIndicator(
                Modifier
                  .size(28.dp)
                  .padding(4.dp),
                color = Color.Black,
                strokeWidth = 3.dp
              )
            } else {
              SendMsgButton(icon, sendButtonSize, sendButtonAlpha, !disabled, sendMessage)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SendMsgButton(
  icon: ImageVector,
  sizeDp: Animatable<Float, AnimationVector1D>,
  alpha: Animatable<Float, AnimationVector1D>,
  enabled: Boolean,
  sendMessage: () -> Unit,
  onLongClick: (() -> Unit)? = null
) {
  val interactionSource = remember { MutableInteractionSource() }
  Box(
    modifier = Modifier.requiredSize(36.dp)
      .combinedClickable(
        onClick = sendMessage,
        onLongClick = onLongClick,
        enabled = enabled,
        role = Role.Button,
        interactionSource = interactionSource,
        indication = rememberRipple(bounded = false, radius = 24.dp)
      ),
    contentAlignment = Alignment.Center
  ) {

    Icon(
      icon,
      stringResource(R.string.icon_descr_send_message),
      tint = Color.White,
      modifier = Modifier
        .size(sizeDp.value.dp)
        .padding(4.dp)
        .alpha(alpha.value)
        .clip(CircleShape)
        .background(if (enabled) MaterialTheme.colors.primary else HighOrLowlight)
        .padding(3.dp)
    )
  }
}

@Composable
private fun NativeKeyboard(
  composeState: MutableState<ComposeState>,
  textStyle: MutableState<TextStyle>,
  showDeleteTextButton: MutableState<Boolean>,
  userIsObserver: Boolean,
  onMessageChange: (String) -> Unit
) {
  val cs = composeState.value
  val textColor = MaterialTheme.colors.onBackground
  val tintColor = MaterialTheme.colors.secondary
  val padding = PaddingValues(12.dp, 7.dp, 45.dp, 0.dp)
  val paddingStart = with(LocalDensity.current) { 12.dp.roundToPx() }
  val paddingTop = with(LocalDensity.current) { 7.dp.roundToPx() }
  val paddingEnd = with(LocalDensity.current) { 45.dp.roundToPx() }
  val paddingBottom = with(LocalDensity.current) { 7.dp.roundToPx() }
  var showKeyboard by remember { mutableStateOf(false) }
  LaunchedEffect(cs.contextItem) {
    if (cs.contextItem is ComposeContextItem.QuotedItem) {
      delay(100)
      showKeyboard = true
    } else if (cs.contextItem is ComposeContextItem.EditingItem) {
      // Keyboard will not show up if we try to show it too fast
      delay(300)
      showKeyboard = true
    }
  }

  AndroidView(modifier = Modifier, factory = {
    val editText = @SuppressLint("AppCompatCustomView") object: EditText(it) {
      override fun setOnReceiveContentListener(
        mimeTypes: Array<out String>?,
        listener: OnReceiveContentListener?
      ) {
        super.setOnReceiveContentListener(mimeTypes, listener)
      }

      override fun onCreateInputConnection(editorInfo: EditorInfo): InputConnection {
        val connection = super.onCreateInputConnection(editorInfo)
        EditorInfoCompat.setContentMimeTypes(editorInfo, arrayOf("image/*"))
        val onCommit = InputConnectionCompat.OnCommitContentListener { inputContentInfo, _, _ ->
          try {
            inputContentInfo.requestPermission()
          } catch (e: Exception) {
            return@OnCommitContentListener false
          }
          SimplexApp.context.chatModel.sharedContent.value = SharedContent.Media("", listOf(inputContentInfo.contentUri))
          true
        }
        return InputConnectionCompat.createWrapper(connection, editorInfo, onCommit)
      }
    }
    editText.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    editText.maxLines = 16
    editText.inputType = InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or editText.inputType
    editText.setTextColor(textColor.toArgb())
    editText.textSize = textStyle.value.fontSize.value
    val drawable = it.getDrawable(R.drawable.send_msg_view_background)!!
    DrawableCompat.setTint(drawable, tintColor.toArgb())
    editText.background = drawable
    editText.setPadding(paddingStart, paddingTop, paddingEnd, paddingBottom)
    editText.setText(cs.message)
    if (Build.VERSION.SDK_INT >= 29) {
      editText.textCursorDrawable?.let { DrawableCompat.setTint(it, HighOrLowlight.toArgb()) }
    } else {
      try {
        val f: Field = TextView::class.java.getDeclaredField("mCursorDrawableRes")
        f.isAccessible = true
        f.set(editText, R.drawable.edit_text_cursor)
      } catch (e: Exception) {
        Log.e(chat.echo.app.TAG, e.stackTraceToString())
      }
    }
    editText.doOnTextChanged { text, _, _, _ -> onMessageChange(text.toString()) }
    editText.doAfterTextChanged { text -> if (composeState.value.preview is ComposePreview.VoicePreview && text.toString() != "") editText.setText("") }
    editText
  }) {
    it.setTextColor(textColor.toArgb())
    it.textSize = textStyle.value.fontSize.value
    DrawableCompat.setTint(it.background, tintColor.toArgb())
    it.isFocusable = composeState.value.preview !is ComposePreview.VoicePreview
    it.isFocusableInTouchMode = it.isFocusable
    if (cs.message != it.text.toString()) {
      it.setText(cs.message)
      // Set cursor to the end of the text
      it.setSelection(it.text.length)
    }
    if (showKeyboard) {
      it.requestFocus()
      val imm: InputMethodManager = SimplexApp.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
      imm.showSoftInput(it, InputMethodManager.SHOW_IMPLICIT)
      showKeyboard = false
    }
    showDeleteTextButton.value = it.lineCount >= 4
  }
  if (composeState.value.preview is ComposePreview.VoicePreview) {
    ComposeOverlay(R.string.voice_message_send_text, textStyle, padding)
  } else if (userIsObserver) {
    ComposeOverlay(R.string.you_are_observer, textStyle, padding)
  }
}

@Composable
private fun ComposeOverlay(textId: Int, textStyle: MutableState<TextStyle>, padding: PaddingValues) {
  Text(
    generalGetString(textId),
    Modifier.padding(padding),
    color = HighOrLowlight,
    style = textStyle.value.copy(fontStyle = FontStyle.Italic)
  )
}

@Composable
private fun BoxScope.DeleteTextButton(composeState: MutableState<ComposeState>) {
  IconButton(
    { composeState.value = composeState.value.copy(message = "") },
    Modifier.align(Alignment.TopEnd).size(36.dp)
  ) {
    Icon(Icons.Filled.Close, null, Modifier.padding(7.dp).size(36.dp), tint = HighOrLowlight)
  }
}

@Composable fun RecordVoiceView(recState: MutableState<RecordingState>, stopRecOnNextClick: MutableState<Boolean>) {
  val rec: Recorder = remember { RecorderNative(MAX_VOICE_SIZE_FOR_SENDING) }
  DisposableEffect(Unit) { onDispose { rec.stop() } }
  val stopRecordingAndAddAudio: () -> Unit = {
    recState.value.filePathNullable?.let {
      recState.value = RecordingState.Finished(it, rec.stop())
    }
  }
  if (stopRecOnNextClick.value) {
    LaunchedEffect(recState.value) {
      if (recState.value is RecordingState.NotStarted) {
        stopRecOnNextClick.value = false
      }
    }
    // Lock orientation to current orientation because screen rotation will break the recording
    LockToCurrentOrientationUntilDispose()
    StopRecordButton(stopRecordingAndAddAudio)
  } else {
    val startRecording: () -> Unit = {
      recState.value = RecordingState.Started(
        filePath = rec.start {progress: Int?, finished: Boolean  ->
          val state = recState.value
          if (state is RecordingState.Started && progress != null) {
            recState.value = if (!finished)
              RecordingState.Started(state.filePath, progress)
            else
              RecordingState.Finished(state.filePath, progress)
          }
        },
      )
    }
    val interactionSource = interactionSourceWithTapDetection(
      onPress = { if (recState.value is RecordingState.NotStarted) startRecording() },
      onClick = {
        if (stopRecOnNextClick.value) {
          stopRecordingAndAddAudio()
        } else {
          // tapped and didn't hold a finger
          stopRecOnNextClick.value = true
        }
      },
      onCancel = stopRecordingAndAddAudio,
      onRelease = stopRecordingAndAddAudio
    )
    RecordVoiceButton(interactionSource)
  }
}

@Composable
private fun DisallowedVoiceButton(enabled: Boolean, onClick: () -> Unit) {
  IconButton(onClick, Modifier.size(36.dp), enabled = enabled) {
    Icon(
      Icons.Outlined.KeyboardVoice,
      stringResource(R.string.icon_descr_record_voice_message),
      tint = HighOrLowlight,
      modifier = Modifier
        .size(36.dp)
        .padding(4.dp)
    )
  }
}

@Composable
private fun VoiceButtonWithoutPermission(onClick: () -> Unit) {
  IconButton(onClick, Modifier.size(36.dp)) {
    Icon(
      Icons.Filled.KeyboardVoice,
      stringResource(R.string.icon_descr_record_voice_message),
      tint = MaterialTheme.colors.primary,
      modifier = Modifier
        .size(34.dp)
        .padding(4.dp)
    )
  }
}

@Composable
private fun LockToCurrentOrientationUntilDispose() {
  val context = LocalContext.current
  DisposableEffect(Unit) {
    val activity = context as Activity
    val manager = context.getSystemService(Activity.WINDOW_SERVICE) as WindowManager
    val rotation = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) manager.defaultDisplay.rotation else activity.display?.rotation
    activity.requestedOrientation = when (rotation) {
      android.view.Surface.ROTATION_90 -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
      android.view.Surface.ROTATION_180 -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
      android.view.Surface.ROTATION_270 -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
      else -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }
    // Unlock orientation
    onDispose { activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
  }
}

@Composable
private fun StopRecordButton(onClick: () -> Unit) {
  IconButton(onClick, Modifier.size(36.dp)) {
    Icon(
      Icons.Filled.Stop,
      stringResource(R.string.icon_descr_record_voice_message),
      tint = MaterialTheme.colors.primary,
      modifier = Modifier
        .size(36.dp)
        .padding(4.dp)
    )
  }
}

@Composable
private fun RecordVoiceButton(interactionSource: MutableInteractionSource) {
  IconButton({}, Modifier.size(36.dp), interactionSource = interactionSource) {
    Icon(
      Icons.Filled.KeyboardVoice,
      stringResource(R.string.icon_descr_record_voice_message),
      tint = MaterialTheme.colors.primary,
      modifier = Modifier
        .size(34.dp)
        .padding(4.dp)
    )
  }
}

@Composable
private fun ProgressIndicator() {
  CircularProgressIndicator(Modifier.size(36.dp).padding(4.dp), color = HighOrLowlight, strokeWidth = 3.dp)
}

@Composable
private fun CancelLiveMessageButton(
  onClick: () -> Unit
) {
  IconButton(onClick, Modifier.size(36.dp)) {
    Icon(
      Icons.Filled.Close,
      stringResource(R.string.icon_descr_cancel_live_message),
      tint = MaterialTheme.colors.primary,
      modifier = Modifier
        .size(36.dp)
        .padding(4.dp)
    )
  }
}


@Composable
private fun StartLiveMessageButton(enabled: Boolean, onClick: () -> Unit) {
  val interactionSource = remember { MutableInteractionSource() }
  Box(
    modifier = Modifier.requiredSize(36.dp)
      .clickable(
        onClick = onClick,
        enabled = enabled,
        role = Role.Button,
        interactionSource = interactionSource,
        indication = rememberRipple(bounded = false, radius = 24.dp)
      ),
    contentAlignment = Alignment.Center
  ) {
    Icon(
      Icons.Filled.Bolt,
      stringResource(R.string.icon_descr_send_message),
      tint = if (enabled) MaterialTheme.colors.primary else HighOrLowlight,
      modifier = Modifier
        .size(36.dp)
        .padding(4.dp)
    )
  }
}

private fun startLiveMessage(
  scope: CoroutineScope,
  send: suspend () -> Unit,
  update: suspend () -> Unit,
  sendButtonSize: Animatable<Float, AnimationVector1D>,
  sendButtonAlpha: Animatable<Float, AnimationVector1D>,
  composeState: MutableState<ComposeState>,
  liveMessageAlertShown: SharedPreference<Boolean>
) {
  fun run() {
    scope.launch {
      while (composeState.value.liveMessage != null) {
        sendButtonSize.animateTo(if (sendButtonSize.value == 36f) 32f else 36f, tween(700, 50))
      }
      sendButtonSize.snapTo(36f)
    }
    scope.launch {
      while (composeState.value.liveMessage != null) {
        sendButtonAlpha.animateTo(if (sendButtonAlpha.value == 1f) 0.75f else 1f, tween(700, 50))
      }
      sendButtonAlpha.snapTo(1f)
    }
    scope.launch {
      delay(3000)
      while (composeState.value.liveMessage != null) {
        update()
        delay(3000)
      }
    }
  }

  fun start() = withBGApi {
    if (composeState.value.liveMessage == null) {
      send()
    }
    run()
  }

  if (liveMessageAlertShown.state.value) {
    start()
  } else {
    AlertManager.shared.showAlertDialog(
      title = generalGetString(R.string.live_message),
      text = generalGetString(R.string.send_live_message_desc),
      confirmText = generalGetString(R.string.send_verb),
      onConfirm = {
        liveMessageAlertShown.set(true)
        start()
      })
  }
}

private fun showNeedToAllowVoiceAlert(onConfirm: () -> Unit) {
  AlertManager.shared.showAlertDialog(
    title = generalGetString(R.string.allow_voice_messages_question),
    text = generalGetString(R.string.you_need_to_allow_to_send_voice),
    confirmText = generalGetString(R.string.allow_verb),
    dismissText = generalGetString(R.string.cancel_verb),
    onConfirm = onConfirm,
  )
}

private fun showDisabledVoiceAlert(isDirectChat: Boolean) {
  AlertManager.shared.showAlertMsg(
    title = generalGetString(R.string.voice_messages_prohibited),
    text = generalGetString(
      if (isDirectChat)
        R.string.ask_your_contact_to_enable_voice
      else
        R.string.only_group_owners_can_enable_voice
    )
  )
}

@Preview(showBackground = true)
@Preview(
  uiMode = Configuration.UI_MODE_NIGHT_YES,
  showBackground = true,
  name = "Dark Mode"
)
@Composable
fun PreviewSendMsgView() {
  val smallFont = MaterialTheme.typography.body1.copy(color = MaterialTheme.colors.onBackground)
  val textStyle = remember { mutableStateOf(smallFont) }
  SimpleXTheme {
    SendMsgView(
      composeState = remember { mutableStateOf(ComposeState(useLinkPreviews = true)) },
      sendMessage = {},
      onMessageChange = { _ -> },
      textStyle = textStyle,
    )
  }
}

@Preview(showBackground = true)
@Preview(
  uiMode = Configuration.UI_MODE_NIGHT_YES,
  showBackground = true,
  name = "Dark Mode"
)
@Composable
fun PreviewSendMsgViewEditing() {
  val smallFont = MaterialTheme.typography.body1.copy(color = MaterialTheme.colors.onBackground)
  val textStyle = remember { mutableStateOf(smallFont) }
  val composeStateEditing = ComposeState(editingItem = ChatItem.getSampleData(),  useLinkPreviews = true)
  SimpleXTheme {
    SendMsgView(
      composeState = remember { mutableStateOf(composeStateEditing) },
      sendMessage = {},
      onMessageChange = { _ -> },
      textStyle = textStyle
    )
  }
}

@Preview(showBackground = true)
@Preview(
  uiMode = Configuration.UI_MODE_NIGHT_YES,
  showBackground = true,
  name = "Dark Mode"
)
@Composable
fun PreviewSendMsgViewInProgress() {
  val smallFont = MaterialTheme.typography.body1.copy(color = MaterialTheme.colors.onBackground)
  val textStyle = remember { mutableStateOf(smallFont) }
  val composeStateInProgress = ComposeState(preview = ComposePreview.FilePreview("test.txt", getAppFileUri("test.txt")), inProgress = true, useLinkPreviews = true)
  SimpleXTheme {
    SendMsgView(
      composeState = remember { mutableStateOf(composeStateInProgress) },
      sendMessage = {},
      onMessageChange = { _ -> },
      textStyle = textStyle
    )
  }
}
