@file:UseSerializers(UriSerializer::class)

package chat.echo.app.views.chat

import ComposeFileView
import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.media.audiofx.Visualizer
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.CallSuper
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import chat.echo.app.*
import chat.echo.app.R
import chat.echo.app.model.*
import chat.echo.app.ui.theme.DividerColor
import chat.echo.app.ui.theme.HighOrLowlight
import chat.echo.app.views.chat.item.*
import chat.echo.app.views.chatlist.setGroupMembers
import chat.echo.app.views.helpers.*
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.serialization.*
import kotlinx.serialization.Serializable
import linc.com.amplituda.Amplituda
import linc.com.amplituda.AmplitudaProcessingOutput
import org.openintents.openpgp.OpenPgpError
import org.openintents.openpgp.util.OpenPgpApi
import java.io.*
import java.nio.file.Files

@Serializable
sealed class ComposePreview {
  @Serializable object NoPreview: ComposePreview()
  @Serializable class CLinkPreview(val linkPreview: LinkPreview?): ComposePreview()
  @Serializable class ImagePreview(val images: List<String>): ComposePreview()
  @Serializable class MediaPreview(val images: List<String>, val content: List<UploadContent>): ComposePreview()
  @Serializable data class VoicePreview(val voice: String, val durationMs: Int, val finished: Boolean): ComposePreview()
  @Serializable class FilePreview(val fileName: String, val uri: Uri): ComposePreview()
}

@Serializable
sealed class ComposeContextItem {
  @Serializable object NoContextItem: ComposeContextItem()
  @Serializable class QuotedItem(val chatItem: ChatItem): ComposeContextItem()
  @Serializable class EditingItem(val chatItem: ChatItem): ComposeContextItem()
}

@Serializable
data class LiveMessage(
  val chatItem: ChatItem,
  val typedMsg: String,
  val sentMsg: String,
  val sent: Boolean
)

@Serializable
data class ComposeState(
  val message: String = "",
  val liveMessage: LiveMessage? = null,
  val preview: ComposePreview = ComposePreview.NoPreview,
  val contextItem: ComposeContextItem = ComposeContextItem.NoContextItem,
  val inProgress: Boolean = false,
  val useLinkPreviews: Boolean
) {
  constructor(editingItem: ChatItem, liveMessage: LiveMessage? = null, useLinkPreviews: Boolean): this(
    editingItem.content.text,
    liveMessage,
    chatItemPreview(editingItem),
    ComposeContextItem.EditingItem(editingItem),
    useLinkPreviews = useLinkPreviews
  )

  val editing: Boolean
    get() =
      when (contextItem) {
        is ComposeContextItem.EditingItem -> true
        else -> false
      }
  val sendEnabled: () -> Boolean
    get() = {
      val hasContent = when (preview) {
        is ComposePreview.MediaPreview -> true
        is ComposePreview.VoicePreview -> true
        is ComposePreview.FilePreview -> true
        else -> message.isNotEmpty() || liveMessage != null
      }
      hasContent && !inProgress
    }
  val endLiveDisabled: Boolean
    get() = liveMessage != null && message.isEmpty() && preview is ComposePreview.NoPreview && contextItem is ComposeContextItem.NoContextItem
  val linkPreviewAllowed: Boolean
    get() =
      when (preview) {
        is ComposePreview.MediaPreview -> false
        is ComposePreview.VoicePreview -> false
        is ComposePreview.FilePreview -> false
        else -> useLinkPreviews
      }
  val linkPreview: LinkPreview?
    get() =
      when (preview) {
        is ComposePreview.CLinkPreview -> preview.linkPreview
        else -> null
      }
  val attachmentDisabled: Boolean
    get() {
      if (editing || liveMessage != null) return true
      return when (preview) {
        ComposePreview.NoPreview -> false
        is ComposePreview.CLinkPreview -> false
        else -> true
      }
    }
  val empty: Boolean
    get() = message.isEmpty() && preview is ComposePreview.NoPreview

  companion object {
    fun saver(): Saver<MutableState<ComposeState>, *> = Saver(
      save = { json.encodeToString(serializer(), it.value) },
      restore = {
        mutableStateOf(json.decodeFromString(it))
      }
    )
  }
}

sealed class RecordingState {
  object NotStarted: RecordingState()
  class Started(val filePath: String, val progressMs: Int = 0): RecordingState()
  class Finished(val filePath: String, val durationMs: Int): RecordingState()

  val filePathNullable: String?
    get() = (this as? Started)?.filePath
}

fun chatItemPreview(chatItem: ChatItem): ComposePreview {
  val fileName = chatItem.file?.fileName ?: ""
  return when (val mc = chatItem.content.msgContent) {
    is MsgContent.MCText -> ComposePreview.NoPreview
    is MsgContent.MCLink -> ComposePreview.CLinkPreview(linkPreview = mc.preview)
    is MsgContent.MCImage -> ComposePreview.ImagePreview(images = listOf(mc.image))
    is MsgContent.MCFile -> ComposePreview.FilePreview(fileName, getAppFileUri(fileName))
    else -> ComposePreview.NoPreview
  }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ComposeView(
  chatModel: ChatModel,
  chat: Chat,
  onChatUpdated: (Chat) -> Unit,
  composeState: MutableState<ComposeState>,
  attachmentOption: MutableState<AttachmentOption?>,
  showChooseAttachment: () -> Unit,
) {
  val context = LocalContext.current
  val linkUrl = remember { mutableStateOf<String?>(null) }
  val prevLinkUrl = remember { mutableStateOf<String?>(null) }
  val pendingLinkUrl = remember { mutableStateOf<String?>(null) }
  val cancelledLinks = remember { mutableSetOf<String>() }
  val useLinkPreviews = chatModel.controller.appPrefs.privacyLinkPreviews.get()
  val maxFileSize: Long = getMaxFileSize(FileProtocol.XFTP)
  val smallFont = MaterialTheme.typography.body1.copy(color = MaterialTheme.colors.onBackground)
  val textStyle = remember { mutableStateOf(smallFont) }
  val scope = rememberCoroutineScope()
  // attachments
  val chosenContent = remember { mutableStateOf<List<UploadContent>>(emptyList()) }
  val chosenFile = remember { mutableStateOf<Uri?>(null) }
  val photoUri = remember { mutableStateOf<Uri?>(null) }
  val photoTmpFile = remember { mutableStateOf<File?>(null) }
  val keyboardController = LocalSoftwareKeyboardController.current
  var resultLauncher: ManagedActivityResultLauncher<IntentSenderRequest, ActivityResult>? = null

  class ComposeTakePicturePreview: ActivityResultContract<Void?, Bitmap?>() {
    @CallSuper
    override fun createIntent(context: Context, input: Void?): Intent {
      photoTmpFile.value = File.createTempFile("image", ".bmp", SimplexApp.context.filesDir)
      photoUri.value = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.provider", photoTmpFile.value!!)
      return Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        .putExtra(MediaStore.EXTRA_OUTPUT, photoUri.value)
    }

    override fun getSynchronousResult(
      context: Context,
      input: Void?
    ): SynchronousResult<Bitmap?>? = null

    override fun parseResult(resultCode: Int, intent: Intent?): Bitmap? {
      val photoUriVal = photoUri.value
      val photoTmpFileVal = photoTmpFile.value
      return if (resultCode == Activity.RESULT_OK && photoUriVal != null && photoTmpFileVal != null) {
        val source = ImageDecoder.createSource(SimplexApp.context.contentResolver, photoUriVal)
        val bitmap = ImageDecoder.decodeBitmap(source)
        photoTmpFileVal.delete()
        bitmap
      } else {
        Log.e(TAG, "Getting image from camera cancelled or failed.")
        photoTmpFile.value?.delete()
        null
      }
    }
  }

  val OPEN_KEY_CHAIN_AVAILABLE_PUBLIC_KEYS = "open_key_available_public_keys"
  val MESSAGE_TO_ENCRYPT = "open_key_message_to_encrypt"
  val cameraLauncher = rememberCameraLauncher { uri: Uri? ->
    if (uri != null) {
      val bitmap: Bitmap? = getBitmapFromUri(uri)
      if (bitmap != null) {
        val imagePreview = resizeImageToStrSize(bitmap, maxDataSize = 14000)
        composeState.value = composeState.value.copy(preview = ComposePreview.MediaPreview(listOf(imagePreview), listOf(UploadContent.SimpleImage(uri))))
      }
    }
  }
  /*
  val cameraLauncher = rememberLauncherForActivityResult(contract = ComposeTakePicturePreview()) { bitmap: Bitmap? ->
    if (bitmap != null) {
      val imagePreview = resizeImageToStrSize(bitmap, maxDataSize = 14000)
      chosenContent.value = listOf(UploadContent.SimpleImage(bitmap))
      composeState.value = composeState.value.copy(preview = ComposePreview.ImagePreview(listOf(imagePreview)))
    }
  }*/
  val cameraPermissionLauncher = rememberPermissionLauncher { isGranted: Boolean ->
    if (isGranted) {
      cameraLauncher.launch(null)
    } else {
      Toast.makeText(context, generalGetString(R.string.toast_permission_denied), Toast.LENGTH_SHORT).show()
    }
  }
  val processPickedMedia = { uris: List<Uri>, text: String? ->
    val content = ArrayList<UploadContent>()
    val imagesPreview = ArrayList<String>()
    uris.forEach { uri ->
      var bitmap: Bitmap? = null
      val isImage = MimeTypeMap.getSingleton().getMimeTypeFromExtension(getFileName(SimplexApp.context, uri)?.split(".")?.last())?.contains("image/") == true
      when {
        isImage -> {
          // Image
          val drawable = getDrawableFromUri(uri)
          bitmap = if (drawable != null) getBitmapFromUri(uri) else null
          val isAnimNewApi = Build.VERSION.SDK_INT >= 28 && drawable is AnimatedImageDrawable
          val isAnimOldApi = Build.VERSION.SDK_INT < 28 &&
              (getFileName(SimplexApp.context, uri)?.endsWith(".gif") == true || getFileName(SimplexApp.context, uri)?.endsWith(".webp") == true)
          if (isAnimNewApi || isAnimOldApi) {
            // It's a gif or webp
            val fileSize = getFileSize(context, uri)
            if (fileSize != null && fileSize <= maxFileSize) {
              content.add(UploadContent.AnimatedImage(uri))
            } else {
              bitmap = null
              AlertManager.shared.showAlertMsg(
                generalGetString(R.string.large_file),
                String.format(generalGetString(R.string.maximum_supported_file_size), formatBytes(maxFileSize))
              )
            }
          } else {
            content.add(UploadContent.SimpleImage(uri))
          }
        }
        else -> {
          // Video
          val res = getBitmapFromVideo(uri)
          bitmap = res.preview
          val durationMs = res.duration
          content.add(UploadContent.Video(uri, durationMs?.div(1000)?.toInt() ?: 0))
        }
      }
      if (bitmap != null) {
        imagesPreview.add(resizeImageToStrSize(bitmap, maxDataSize = 14000))
      }
    }
    if (imagesPreview.isNotEmpty()) {
      composeState.value = composeState.value.copy(message = text ?: composeState.value.message, preview = ComposePreview.MediaPreview(imagesPreview, content))
    }
  }
  val processPickedFile = { uri: Uri?, text: String? ->
    if (uri != null) {
      val fileSize = getFileSize(context, uri)
      if (fileSize != null && fileSize <= maxFileSize) {
        val fileName = getFileName(SimplexApp.context, uri)
        if (fileName != null) {
          composeState.value = composeState.value.copy(message = text ?: composeState.value.message, preview = ComposePreview.FilePreview(fileName, uri))
        }
      } else {
        AlertManager.shared.showAlertMsg(
          generalGetString(R.string.large_file),
          String.format(generalGetString(R.string.maximum_supported_file_size), formatBytes(maxFileSize))
        )
      }
    }
  }
  val galleryImageLauncher = rememberLauncherForActivityResult(contract = PickMultipleImagesFromGallery()) { processPickedMedia(it, null) }
  val galleryImageLauncherFallback = rememberGetMultipleContentsLauncher { processPickedMedia(it, null) }
  val galleryVideoLauncher = rememberLauncherForActivityResult(contract = PickMultipleVideosFromGallery()) { processPickedMedia(it, null) }
  val galleryVideoLauncherFallback = rememberGetMultipleContentsLauncher { processPickedMedia(it, null) }
  val filesLauncher = rememberGetContentLauncher { processPickedFile(it, null) }
  val recState: MutableState<RecordingState> = remember { mutableStateOf(RecordingState.NotStarted) }




  LaunchedEffect(attachmentOption.value) {
    when (attachmentOption.value) {
      AttachmentOption.CameraPhoto -> {
        when (PackageManager.PERMISSION_GRANTED) {
          ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) -> {
            cameraLauncher.launchWithFallback()
          }
          else -> {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
          }
        }
        attachmentOption.value = null
      }
      AttachmentOption.GalleryImage -> {
        try {
          galleryImageLauncher.launch(0)
        } catch (e: ActivityNotFoundException) {
          galleryImageLauncherFallback.launch("image/*")
        }
        attachmentOption.value = null
      }
      AttachmentOption.GalleryVideo -> {
        try {
          galleryVideoLauncher.launch(0)
        } catch (e: ActivityNotFoundException) {
          galleryVideoLauncherFallback.launch("video/*")
        }
        attachmentOption.value = null
      }
      AttachmentOption.File -> {
        filesLauncher.launch("*/*")
        attachmentOption.value = null
      }
      else -> {}
    }
  }

  fun isSimplexLink(link: String): Boolean =
    link.startsWith("https://simplex.chat", true) || link.startsWith("http://simplex.chat", true)

  fun parseMessage(msg: String): String? {
    val parsedMsg = runBlocking { chatModel.controller.apiParseMarkdown(msg) }
    val link = parsedMsg?.firstOrNull { ft -> ft.format is Format.Uri && !cancelledLinks.contains(ft.text) && !isSimplexLink(ft.text) }
    return link?.text
  }

  fun loadLinkPreview(url: String, wait: Long? = null) {
    if (pendingLinkUrl.value == url) {
      composeState.value = composeState.value.copy(preview = ComposePreview.CLinkPreview(null))
      withApi {
        if (wait != null) delay(wait)
        val lp = getLinkPreview(url)
        if (lp != null && pendingLinkUrl.value == url) {
          composeState.value = composeState.value.copy(preview = ComposePreview.CLinkPreview(lp))
          pendingLinkUrl.value = null
        } else if (pendingLinkUrl.value == url) {
          composeState.value = composeState.value.copy(preview = ComposePreview.NoPreview)
          pendingLinkUrl.value = null
        }
      }
    }
  }

  fun showLinkPreview(s: String) {
    prevLinkUrl.value = linkUrl.value
    linkUrl.value = parseMessage(s)
    val url = linkUrl.value
    if (url != null) {
      if (url != composeState.value.linkPreview?.uri && url != pendingLinkUrl.value) {
        pendingLinkUrl.value = url
        loadLinkPreview(url, wait = if (prevLinkUrl.value == url) null else 1500L)
      }
    } else {
      composeState.value = composeState.value.copy(preview = ComposePreview.NoPreview)
    }
  }

  fun resetLinkPreview() {
    linkUrl.value = null
    prevLinkUrl.value = null
    pendingLinkUrl.value = null
    cancelledLinks.clear()
  }

  fun checkLinkPreview(): MsgContent {
    val cs = composeState.value
    return when (val composePreview = cs.preview) {
      is ComposePreview.CLinkPreview -> {
        val url = parseMessage(cs.message)
        val lp = composePreview.linkPreview
        if (lp != null && url == lp.uri) {
          MsgContent.MCLink(cs.message, preview = lp)
        } else {
          MsgContent.MCText(cs.message)
        }
      }
      else -> MsgContent.MCText(cs.message)
    }
  }

  fun updateMsgContent(msgContent: MsgContent): MsgContent {
    val cs = composeState.value
    return when (msgContent) {
      is MsgContent.MCText -> MsgContent.MCText(cs.message)
      is MsgContent.MCLink -> checkLinkPreview()
      is MsgContent.MCImage -> MsgContent.MCImage(cs.message, image = msgContent.image)
      is MsgContent.MCVideo -> MsgContent.MCVideo(cs.message, image = msgContent.image, duration = msgContent.duration)
      is MsgContent.MCVoice -> MsgContent.MCVoice(cs.message, duration = msgContent.duration)
      is MsgContent.MCFile -> MsgContent.MCFile(cs.message)
      is MsgContent.MCPublicKey -> MsgContent.MCPublicKey(cs.message)
      is MsgContent.MCGroupPublicKey -> MsgContent.MCGroupPublicKey("", cs.message, msgContent.neededBy)
      is MsgContent.MCPublicKeyRequest -> MsgContent.MCPublicKeyRequest(cs.message, msgContent.publicKey, msgContent.requestedBy)
      is MsgContent.MCUnknown -> MsgContent.MCUnknown(type = msgContent.type, text = cs.message, json = msgContent.json)
    }
  }

  fun clearState(live: Boolean = false) {
    if (live) {
      composeState.value = composeState.value.copy(inProgress = false)
    } else {
      composeState.value = ComposeState(useLinkPreviews = useLinkPreviews)
      resetLinkPreview()
    }
    recState.value = RecordingState.NotStarted
    textStyle.value = smallFont
    chatModel.removeLiveDummy()
  }

  fun deleteUnusedFiles() {
    chatModel.filesToDelete.forEach { it.delete() }
    chatModel.filesToDelete.clear()
  }
  /*
    fun sendMessage(cs: ComposeState, cInfo: ChatInfo) {
      composeState.value = composeState.value.copy(inProgress = true)
      val coroutineScope = CoroutineScope(Dispatchers.IO)
      when (val contextItem = cs.contextItem) {
        is ComposeContextItem.EditingItem -> {
          val ei = contextItem.chatItem
          val oldMsgContent = ei.content.msgContent
          if (oldMsgContent != null) {
            withApi {
              val updatedItem = chatModel.controller.apiUpdateChatItem(
                type = cInfo.chatType,
                id = cInfo.apiId,
                itemId = ei.meta.itemId,
                mc = updateMsgContent(oldMsgContent)
              )
              if (updatedItem != null) chatModel.upsertChatItem(cInfo, updatedItem.chatItem)
              clearState()
            }
          }
        }
        else -> {
          val msgs: ArrayList<MsgContent> = ArrayList()
          val files: ArrayList<String> = ArrayList()
          if (cInfo is ChatInfo.Direct) {
            coroutineScope.launch {
              withContext(Dispatchers.IO) {
                val contactInfo = chatModel.controller.apiContactInfo(cInfo.apiId)
                if (contactInfo != null) {
                  when (val preview = cs.preview) {
                    ComposePreview.NoPreview -> {
                      msgs.add(MsgContent.MCText(cs.message))
                    }
                    is ComposePreview.CLinkPreview -> msgs.add(checkLinkPreview())
                    is ComposePreview.ImagePreview -> {
                      chosenContent.value.forEachIndexed { index, it ->
                        val file = when (it) {
                          is UploadContent.SimpleImage -> saveImage(context, it.uri)
                          is UploadContent.AnimatedImage -> saveAnimImage(context, it.uri)
                          is UploadContent.Video -> saveFileFromUri(context, it.uri)
                        }
                        if (file != null) {
                          files.add(file)
                          if (it is UploadContent.Video) {
                            msgs.add(MsgContent.MCVideo(if (preview.content.lastIndex == index) msgText else "", preview.images[index], it.duration))
                          } else {
                            msgs.add(MsgContent.MCImage(if (preview.content.lastIndex == index) msgText else "", preview.images[index]))
                          }
                        }
                      }
                    }
                    is ComposePreview.FilePreview -> {
                      val chosenFileVal = chosenFile.value
                      if (chosenFileVal != null) {
                        val file = saveFileFromUri(context, chosenFileVal)
                        if (file != null) {
                          files.add((file))
                          msgs.add(MsgContent.MCFile(if (msgs.isEmpty()) cs.message else ""))
                        }
                      }
                    }
                  }
                  val quotedItemId: Long? = when (contextItem) {
                    is ComposeContextItem.QuotedItem -> contextItem.chatItem.id
                    else -> null
                  }
                  if (msgs.isNotEmpty()) {
                    withApi {
                      msgs.forEachIndexed { index, content ->
                        if (index > 0) delay(100)
                        val aChatItem = chatModel.controller.apiSendMessage(
                          type = cInfo.chatType,
                          id = cInfo.apiId,
                          file = files.getOrNull(index),
                          quotedItemId = if (index == 0) quotedItemId else null,
                          mc = content
                        )
                        if (aChatItem != null) {
                          chatModel.addChatItem(cInfo, aChatItem.chatItem)
                        }
                      }
                      clearState()
                    }
                  } else {
                    clearState()
                  }
                }
              }
            }
          } else {
            when (val preview = cs.preview) {
              ComposePreview.NoPreview -> {
                msgs.add(MsgContent.MCText(cs.message))
              }
              is ComposePreview.CLinkPreview -> msgs.add(checkLinkPreview())
              is ComposePreview.ImagePreview -> {
                chosenContent.value.forEachIndexed { index, it ->
                  val file = when (it) {
                    is UploadContent.SimpleImage -> saveImage(context, it.bitmap)
                    is UploadContent.AnimatedImage -> saveAnimImage(context, it.uri)
                  }
                  if (file != null) {
                    files.add(file)
                    msgs.add(MsgContent.MCImage(if (msgs.isEmpty()) cs.message else "", preview.images[index]))
                  }
                }
              }
              is ComposePreview.FilePreview -> {
                val chosenFileVal = chosenFile.value
                if (chosenFileVal != null) {
                  val file = saveFileFromUri(context, chosenFileVal)
                  if (file != null) {
                    files.add((file))
                    msgs.add(MsgContent.MCFile(if (msgs.isEmpty()) cs.message else ""))
                  }
                }
              }
            }
            val quotedItemId: Long? = when (contextItem) {
              is ComposeContextItem.QuotedItem -> contextItem.chatItem.id
              else -> null
            }
            if (msgs.isNotEmpty()) {
              withApi {
                msgs.forEachIndexed { index, content ->
                  if (index > 0) delay(100)
                  val aChatItem = chatModel.controller.apiSendMessage(
                    type = cInfo.chatType,
                    id = cInfo.apiId,
                    file = files.getOrNull(index),
                    quotedItemId = if (index == 0) quotedItemId else null,
                    mc = content
                  )
                  if (aChatItem != null) {
                    chatModel.addChatItem(cInfo, aChatItem.chatItem)
                  }
                }
                clearState()
              }
            } else {
              clearState()
            }
          }
        }
      }
    }

    fun sendMessage() {
      composeState.value = composeState.value.copy(inProgress = false)
      val cInfo = chat.chatInfo
      val cs = composeState.value
      when (val contextItem = cs.contextItem) {
        is ComposeContextItem.EditingItem -> {
          val ei = contextItem.chatItem
          val oldMsgContent = ei.content.msgContent
          if (oldMsgContent != null) {
            withApi {
              val updatedItem = chatModel.controller.apiUpdateChatItem(
                type = cInfo.chatType,
                id = cInfo.apiId,
                itemId = ei.meta.itemId,
                mc = updateMsgContent(oldMsgContent)
              )
              if (updatedItem != null) chatModel.upsertChatItem(cInfo, updatedItem.chatItem)
              clearState()
            }
          }
          Log.i(TAG, "sendMessage:  old message content is " + oldMsgContent.toString())
        }
        else -> {
          val msgs: ArrayList<MsgContent> = ArrayList()
          val files: ArrayList<String> = ArrayList()
          when (val preview = cs.preview) {
            ComposePreview.NoPreview -> msgs.add(MsgContent.MCText(msgText))
            is ComposePreview.CLinkPreview -> msgs.add(checkLinkPreview())
            is ComposePreview.MediaPreview -> {
              preview.content.forEachIndexed { index, it ->
                val file = when (it) {
                  is UploadContent.SimpleImage -> saveImage(context, it.uri)
                  is UploadContent.AnimatedImage -> saveAnimImage(context, it.uri)
                  is UploadContent.Video -> saveFileFromUri(context, it.uri)
                }
                if (file != null) {
                  files.add(file)
                  if (it is UploadContent.Video) {
                    msgs.add(MsgContent.MCVideo(if (preview.content.lastIndex == index) msgText else "", preview.images[index], it.duration))
                  } else {
                    msgs.add(MsgContent.MCImage(if (preview.content.lastIndex == index) msgText else "", preview.images[index]))
                  }
                }
              }
            }
            is ComposePreview.VoicePreview -> {
              val tmpFile = File(preview.voice)
              AudioPlayer.stop(tmpFile.absolutePath)
              val actualFile = File(getAppFilePath(SimplexApp.context, tmpFile.name.replaceAfter(RecorderNative.extension, "")))
              withContext(Dispatchers.IO) {
                Files.move(tmpFile.toPath(), actualFile.toPath())
              }
              files.add(actualFile.name)
              deleteUnusedFiles()
              msgs.add(MsgContent.MCVoice(if (msgs.isEmpty()) msgText else "", preview.durationMs / 1000))
            }
            is ComposePreview.FilePreview -> {
              val file = saveFileFromUri(context, preview.uri)
              if (file != null) {
                files.add((file))
                msgs.add(MsgContent.MCFile(if (msgs.isEmpty()) msgText else ""))
              }
            }
          }
          val quotedItemId: Long? = when (contextItem) {
            is ComposeContextItem.QuotedItem -> contextItem.chatItem.id
            else -> null
          }
          Log.i(TAG, "sendMessage: quoted id is " + quotedItemId.toString())
          if (msgs.isNotEmpty()) {
            withApi {
              msgs.forEachIndexed { index, content ->
                if (index > 0) delay(100)
                val aChatItem = chatModel.controller.apiSendMessage(
                  type = cInfo.chatType,
                  id = cInfo.apiId,
                  file = files.getOrNull(index),
                  quotedItemId = if (index == 0) quotedItemId else null,
                  mc = content
                )
                if (aChatItem != null) chatModel.addChatItem(cInfo, aChatItem.chatItem)
              }
              clearState()
            }
          } else {
            clearState()
          }
        }
      }
    }
  */

  suspend fun send(cInfo: ChatInfo, mc: MsgContent, quoted: Long?, file: String? = null, live: Boolean = false): ChatItem? {
    val aChatItem = chatModel.controller.apiSendMessage(
      type = cInfo.chatType,
      id = cInfo.apiId,
      file = file,
      quotedItemId = quoted,
      mc = mc,
      live = live
    )
    if (aChatItem != null) chatModel.addChatItem(cInfo, aChatItem.chatItem)
    return aChatItem?.chatItem
  }

  suspend fun sendMessageAsync(text: String?, live: Boolean): ChatItem? {
    val cInfo = chat.chatInfo
    val cs = composeState.value
    var sent: ChatItem?
    val msgText = text ?: cs.message

    fun sending() {
      composeState.value = composeState.value.copy(inProgress = true)
    }

    fun checkLinkPreview(): MsgContent {
      return when (val composePreview = cs.preview) {
        is ComposePreview.CLinkPreview -> {
          val url = parseMessage(msgText)
          val lp = composePreview.linkPreview
          if (lp != null && url == lp.uri) {
            MsgContent.MCLink(msgText, preview = lp)
          } else {
            MsgContent.MCText(msgText)
          }
        }
        else -> MsgContent.MCText(msgText)
      }
    }

    fun updateMsgContent(msgContent: MsgContent): MsgContent {
      return when (msgContent) {
        is MsgContent.MCText -> checkLinkPreview()
        is MsgContent.MCLink -> checkLinkPreview()
        is MsgContent.MCImage -> MsgContent.MCImage(msgText, image = msgContent.image)
        is MsgContent.MCVideo -> MsgContent.MCVideo(msgText, image = msgContent.image, duration = msgContent.duration)
        is MsgContent.MCVoice -> MsgContent.MCVoice(msgText, duration = msgContent.duration)
        is MsgContent.MCFile -> MsgContent.MCFile(msgText)
        is MsgContent.MCPublicKey -> MsgContent.MCPublicKey(msgText)
        is MsgContent.MCGroupPublicKey -> MsgContent.MCGroupPublicKey("", msgText, msgContent.neededBy)
        is MsgContent.MCPublicKeyRequest -> MsgContent.MCPublicKeyRequest(msgText, msgContent.publicKey, msgContent.requestedBy)
        is MsgContent.MCUnknown -> MsgContent.MCUnknown(type = msgContent.type, text = msgText, json = msgContent.json)
      }
    }

    suspend fun updateMessage(ei: ChatItem, cInfo: ChatInfo, live: Boolean): ChatItem? {
      val oldMsgContent = ei.content.msgContent
      if (oldMsgContent != null) {
        val updatedItem = chatModel.controller.apiUpdateChatItem(
          type = cInfo.chatType,
          id = cInfo.apiId,
          itemId = ei.meta.itemId,
          mc = updateMsgContent(oldMsgContent),
          live = live
        )
        if (updatedItem != null) chatModel.upsertChatItem(cInfo, updatedItem.chatItem)
        return updatedItem?.chatItem
      }
      return null
    }

    val liveMessage = cs.liveMessage
    if (!live) {
      if (liveMessage != null) composeState.value = cs.copy(liveMessage = null)
      sending()
    }

    if (cs.contextItem is ComposeContextItem.EditingItem) {
      val ei = cs.contextItem.chatItem
      sent = updateMessage(ei, cInfo, live)
    } else if (liveMessage != null && liveMessage.sent) {
      sent = updateMessage(liveMessage.chatItem, cInfo, live)
    } else {
      val msgs: ArrayList<MsgContent> = ArrayList()
      val files: ArrayList<String> = ArrayList()
      when (val preview = cs.preview) {
        ComposePreview.NoPreview -> msgs.add(MsgContent.MCText(msgText))
        is ComposePreview.CLinkPreview -> msgs.add(checkLinkPreview())
        is ComposePreview.MediaPreview -> {
          preview.content.forEachIndexed { index, it ->
            val file = when (it) {
              is UploadContent.SimpleImage -> saveImage(context, it.uri)
              is UploadContent.AnimatedImage -> saveAnimImage(context, it.uri)
              is UploadContent.Video -> saveFileFromUri(context, it.uri)
            }
            if (file != null) {
              files.add(file)
              if (it is UploadContent.Video) {
                msgs.add(MsgContent.MCVideo(if (preview.content.lastIndex == index) msgText else "", preview.images[index], it.duration))
              } else {
                msgs.add(MsgContent.MCImage(if (preview.content.lastIndex == index) msgText else "", preview.images[index]))
              }
            }
          }
        }
        is ComposePreview.VoicePreview -> {
          val tmpFile = File(preview.voice)
          AudioPlayer.stop(tmpFile.absolutePath)
          val actualFile = File(getAppFilePath(SimplexApp.context, tmpFile.name.replaceAfter(RecorderNative.extension, "")))
          withContext(Dispatchers.IO) {
            Files.move(tmpFile.toPath(), actualFile.toPath())
          }
          files.add(actualFile.name)
          deleteUnusedFiles()
          msgs.add(MsgContent.MCVoice(if (msgs.isEmpty()) msgText else "", preview.durationMs / 1000))
        }
        is ComposePreview.FilePreview -> {
          val file = saveFileFromUri(context, preview.uri)
          if (file != null) {
            files.add((file))
            msgs.add(MsgContent.MCFile(if (msgs.isEmpty()) msgText else ""))
          }
        }
        else -> {}
      }
      val quotedItemId: Long? = when (cs.contextItem) {
        is ComposeContextItem.QuotedItem -> cs.contextItem.chatItem.id
        else -> null
      }
      sent = null
      msgs.forEachIndexed { index, content ->
        if (index > 0) delay(100)
        sent = send(
          cInfo, content, if (index == 0) quotedItemId else null, files.getOrNull(index),
          if (content !is MsgContent.MCVoice && index == msgs.lastIndex) live else false
        )
      }
      if (sent == null &&
        (cs.preview is ComposePreview.MediaPreview ||
            cs.preview is ComposePreview.FilePreview ||
            cs.preview is ComposePreview.VoicePreview)
      ) {
        sent = send(cInfo, MsgContent.MCText(msgText), quotedItemId, null, live)
      }
    }
    clearState(live)
    return sent
  }

  fun sendMessage() {
    withBGApi {
      sendMessageAsync(null, false)
    }
  }

  fun onMessageChange(s: String) {
      composeState.value = composeState.value.copy(message = s)
      if (isShortEmoji(s)) {
        textStyle.value = if (s.codePoints().count() < 4) largeEmojiFont else mediumEmojiFont
      } else {
        textStyle.value = smallFont
        if (composeState.value.linkPreviewAllowed) {
          if (s.isNotEmpty()) showLinkPreview(s)
          else resetLinkPreview()
        }
    }

  }

  fun encryptMessage(userId: Array<String?>) {
    val data = Intent()
    data.action = OpenPgpApi.ACTION_ENCRYPT
    data.putExtra(OpenPgpApi.EXTRA_USER_IDS, userId)
    data.putExtra(OpenPgpApi.EXTRA_REQUEST_ASCII_ARMOR, true)
    val inputStream: InputStream = ByteArrayInputStream(composeState.value.message.trim().toByteArray(charset("UTF-8")))
    val outputStream = ByteArrayOutputStream()
    val api = OpenPgpApi(chatModel.activity!!.applicationContext, chatModel.mServiceConnection!!.service)
    Log.i(TAG, "encryptMessage: here is called for encrypted")
    val callBack = OpenPgpApi.IOpenPgpCallback { result ->
      if (result != null) {
        when (result.getIntExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR)) {
          OpenPgpApi.RESULT_CODE_SUCCESS -> {
            try {
              composeState.value = composeState.value.copy(message = outputStream.toString())
              sendMessage()
              resetLinkPreview()
            } catch (e: UnsupportedEncodingException) {
              AlertManager.shared.showAlertMsg(
                title = generalGetString(R.string.unable_to_encrypt),
                text = "Message cannot be encrypted because: ${e.message}.\n\n Please contact developer.",
                confirmText = generalGetString(R.string.ok),
                onConfirm = {
                  AlertManager.shared.hideAlert()
                }
              )
            }
          }
          OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED -> {
            if (!chatModel.controller.appPrefs.is_openkeychain_authenticated.get()!!) {
              val pi = result.getParcelableExtra<PendingIntent>(OpenPgpApi.RESULT_INTENT)
              try {
                resultLauncher!!.launch(IntentSenderRequest.Builder(pi!!).build())
              } catch (e: IntentSender.SendIntentException) {
                Log.e(TAG, "SendIntentException", e)
              }
            } else {
              if (chat.chatInfo is ChatInfo.Direct) {
                var contactBioInfo = if (chat.chatInfo.localAlias != "") {
                  json.decodeFromString(ContactBioInfoSerializer, chat.chatInfo.localAlias)
                } else {
                  ContactBioInfo.ContactBioExtra("", "", publicKey = "", openKeyChainID = "")
                }

                if (contactBioInfo.openKeyChainID != "" && contactBioInfo.publicKey != "") {
                  AlertManager.shared.showAlertDialog(
                    title = generalGetString(R.string.unable_to_encrypt),
                    text = generalGetString(R.string.open_key_chain_id_revoked_expired_deleted),
                    confirmText = generalGetString(R.string.request_new_public_key),
                    onConfirm = {
                      val contactBioExtra = ContactBioInfo.ContactBioExtra(contactBioInfo.tag, contactBioInfo.notes, "", "")
                      setContactAlias(chat.chatInfo.apiId, json.encodeToString(ContactBioInfoSerializer, contactBioExtra), chatModel, onChatUpdated)
                      val content = MsgContent.MCPublicKeyRequest(generalGetString(R.string.encrypted_message_cannot_be_sent), chatModel.controller.appPrefs.publicKey.get()!!, "null")
                      withApi {
                        val command = chatModel.controller.apiSendMessage(chat.chatInfo.chatType, chat.chatInfo.apiId, null, null, content)
                        if (command != null) {
                          chatModel.addChatItem(chat.chatInfo, command.chatItem)
                          keyboardController?.hide()
                          AlertManager.shared.hideAlert()
                        }
                      }
                    },
                    dismissText = generalGetString(R.string.no),
                  )
                }
              }
            }
          }
          OpenPgpApi.RESULT_CODE_ERROR -> {
            val error: OpenPgpError? = result.getParcelableExtra(OpenPgpApi.RESULT_ERROR)
            if (error != null) {
              AlertManager.shared.showAlertDialog(
                title = generalGetString(R.string.unable_to_encrypt),
                text = generalGetString(R.string.unknown_error_occurred) + "\n Possible Error: " + error.message,
                confirmText = generalGetString(R.string.ok),
                onConfirm = { AlertManager.shared.hideAlert() },
              )
              Log.d(TAG, "onReturn: error is " + error.message)
            }
          }
        }
      }
    }
    api.executeApiAsync(data, inputStream, outputStream, callBack)
  }

  fun refreshUserPublicKeys(contacts: MutableList<ContactInfo>, keyLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>) {
    val intent = Intent(Intent.ACTION_MAIN)
    val ECHO_CHAT_OPEN_KEYCHAIN_CLONE = "org.sufficientlysecure.keychain"
    val USER_PUBLIC_KEYS = "user_public_keys"
    val gson = Gson()
    val publicKeysToImport = gson.toJson(contacts)

    intent.component = ComponentName.unflattenFromString(ECHO_CHAT_OPEN_KEYCHAIN_CLONE + ".debug" + "/" + ECHO_CHAT_OPEN_KEYCHAIN_CLONE + ".ui.AutomaticallyImportKey")
    intent.addCategory(Intent.CATEGORY_LAUNCHER)
    intent.putExtra(USER_PUBLIC_KEYS, publicKeysToImport)
    keyLauncher.launch(intent)
  }

  fun getGroupMembers(): List<GroupMember> {
      return chatModel.groupMembers
        .filter { it.memberStatus != GroupMemberStatus.MemLeft && it.memberStatus != GroupMemberStatus.MemRemoved }
        .sortedBy { it.displayName.lowercase() }
  }

  fun getUnencryptedUsers(): List<GroupMember> {
      return chatModel.groupMembers
        .filter { it.memberStatus != GroupMemberStatus.MemLeft && it.memberStatus != GroupMemberStatus.MemRemoved }
        .filter {
          it.memberProfile.localAlias == "" ||
              (it.memberProfile.localAlias != "" && json.decodeFromString(ContactBioInfoSerializer, it.memberProfile.localAlias).openKeyChainID == "")
        }
        .sortedBy { it.displayName.lowercase() }
  }

  fun getEncryptedUsers(): List<GroupMember> {
      return chatModel.groupMembers
        .filter { it.memberStatus != GroupMemberStatus.MemLeft && it.memberStatus != GroupMemberStatus.MemRemoved }
        .filter { it.memberProfile.localAlias != "" && json.decodeFromString(ContactBioInfoSerializer, it.memberProfile.localAlias).openKeyChainID != "" }
        .sortedBy { it.displayName.lowercase() }
  }

  fun sendGroupMessages(groupInfo: GroupInfo, encryptedGroupMembers: List<GroupMember>) {
    val publicKeysToEncryptMessage = arrayOfNulls<String>(1 + encryptedGroupMembers.size)
    publicKeysToEncryptMessage[0] = chatModel.controller.appPrefs.openKeyChainID.get()
    encryptedGroupMembers.forEachIndexed { index, groupMember ->
      var contactBioInfo = json.decodeFromString(ContactBioInfoSerializer, groupMember.memberProfile.localAlias)
      publicKeysToEncryptMessage[index + 1] = contactBioInfo.openKeyChainID
    }
    encryptMessage(publicKeysToEncryptMessage)
  }

  fun prepEncryptedMessage(chatModel: ChatModel, chatInfo: ChatInfo) {
    if (chatInfo is ChatInfo.Direct) {
      var contactBioInfo = if (chatInfo.localAlias != "") {
        json.decodeFromString(ContactBioInfoSerializer, chatInfo.localAlias)
      } else {
        ContactBioInfo.ContactBioExtra("", "", publicKey = "", openKeyChainID = "")
      }

      Log.i(TAG, "prepEncryptedMessage: compose info is " + contactBioInfo.publicKey + " " + contactBioInfo.openKeyChainID)

      if (contactBioInfo.openKeyChainID != "" && contactBioInfo.publicKey != "") {
        val publicKeysToEncryptMessage = arrayOfNulls<String>(2)
        publicKeysToEncryptMessage[0] = chatModel.controller.appPrefs.openKeyChainID.get()
        publicKeysToEncryptMessage[1] = contactBioInfo.openKeyChainID
        encryptMessage(publicKeysToEncryptMessage)
      }

      if (contactBioInfo.publicKey != "" && contactBioInfo.openKeyChainID == "") {
        AlertManager.shared.showAlertDialog(
          title = generalGetString(R.string.unable_to_encrypt),
          text = generalGetString(R.string.open_key_chain_public_key_found),
          confirmText = generalGetString(R.string.yes),
          onConfirm = {
            val content = MsgContent.MCPublicKeyRequest(generalGetString(R.string.encrypted_message_cannot_be_sent), chatModel.controller.appPrefs.publicKey.get()!!, "null")
            withApi {
              val command = chatModel.controller.apiSendMessage(chat.chatInfo.chatType, chat.chatInfo.apiId, null, null, content)
              if (command != null) {
                chatModel.addChatItem(chat.chatInfo, command.chatItem)
                keyboardController?.hide()
                AlertManager.shared.hideAlert()
              }
            }
          },
          dismissText = generalGetString(R.string.no),
        )
      }

      if (contactBioInfo.publicKey == "" && contactBioInfo.openKeyChainID == "") {
        AlertManager.shared.showAlertMsg(
          title = generalGetString(R.string.unable_to_encrypt),
          text = generalGetString(R.string.unable_to_encrypt_message),
          confirmText = generalGetString(R.string.request_public_key),
          onConfirm = {
            val content = MsgContent.MCPublicKeyRequest(generalGetString(R.string.encrypted_message_cannot_be_sent), chatModel.controller.appPrefs.publicKey.get()!!, "null")
            withApi {
              val command = chatModel.controller.apiSendMessage(chat.chatInfo.chatType, chat.chatInfo.apiId, null, null, content)
              if (command != null) {
                chatModel.addChatItem(chat.chatInfo, command.chatItem)
                keyboardController?.hide()
                AlertManager.shared.hideAlert()
              }
            }
          }
        )
      }
    }
    if (chatInfo is ChatInfo.Group) {
      withApi {
        setGroupMembers(chatInfo.groupInfo, chatModel)
        val unencryptedMembers = getUnencryptedUsers()
        val groupMembers = getGroupMembers()
        if (unencryptedMembers.isNotEmpty()) {
          AlertManager.shared.showAlertDialog(
            title = generalGetString(R.string.unable_to_encrypt),
            text = String.format(
              generalGetString(R.string.message_not_encrypted_for_all_members), unencryptedMembers.size, getGroupMembers().size, if (groupMembers.size >= 2) {
                generalGetString(R.string.member_plural)
              } else {
                generalGetString(R.string.member_singular)
              }
            ),
            confirmText = generalGetString(R.string.request_public_key),
            onConfirm = {
              val content = MsgContent.MCPublicKeyRequest(generalGetString(R.string.encrypted_message_cannot_be_sent), chatModel.controller.appPrefs.publicKey.get()!!, chatInfo.groupInfo.membership.memberId)
              withApi {
                val listOfUnencryptedMembers = getUnencryptedUsers()
                for (groupMember in listOfUnencryptedMembers) {
                  val command = chatModel.controller.apiSendMessage(ChatType.Group, chatInfo.apiId, null, null, content)
                  if (command != null) {
                    chatModel.addChatItem(chat.chatInfo, command.chatItem)
                  }
                }
                keyboardController?.hide()
                AlertManager.shared.hideAlert()
              }
            },
            dismissText = generalGetString(R.string.send_anyway),
            onDismiss = {
              sendGroupMessages(chatInfo.groupInfo, getEncryptedUsers())
            }
          )
        } else {
          sendGroupMessages(chatInfo.groupInfo, getEncryptedUsers())
        }
      }
    }
    /*    chatModel.localContactViewModel?.processLocalContact(chatModel.currentUser.value?.userId.toString(),
        chatInfo.id
        ) {
          val localGroup = it[0]
          val gson = Gson()
          if (chatInfo is ChatInfo.Group) {
            if (localGroup.encryptedMembers != "") {
              val myType = object: TypeToken<MutableList<LocalContact>>() {}.type
              val encryptedLocalContacts = gson.fromJson<MutableList<LocalContact>>(localGroup.encryptedMembers, myType)
              val publicKeysToEncryptMessage = arrayOfNulls<String>(encryptedLocalContacts.size + 1)
              for (i in 0 until encryptedLocalContacts.size) {
                publicKeysToEncryptMessage[i] = encryptedLocalContacts[i].openKeyChainEmail
              }
              publicKeysToEncryptMessage[publicKeysToEncryptMessage.size - 1] = it[1].openKeyChainEmail
              encryptMessage(publicKeysToEncryptMessage, localGroup.burnerTime)
            } else if (localGroup != null && localGroup.encryptedMembers == "") {
              val publicKeyToEncryptMessage = arrayOfNulls<String>(1)
              publicKeyToEncryptMessage.set(0, it.get(1).openKeyChainEmail)
              encryptMessage(publicKeyToEncryptMessage, localGroup.burnerTime)
            }
          }
          if (chat.chatInfo is ChatInfo.Direct) {
            val currentUser = it[0]
            val localContact = it[1]
            if (localContact != null && localContact.openKeyChainEmail != "" && currentUser.openKeyChainEmail != null) {
              val publicKeysToEncryptMessage = arrayOfNulls<String>(2)
              publicKeysToEncryptMessage[0] = currentUser.openKeyChainEmail
              publicKeysToEncryptMessage[1] = localContact.openKeyChainEmail
              encryptMessage(publicKeysToEncryptMessage, localContact.burnerTime)
            }
          }
        }*/
  }

  resultLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
    val EXTRA_IS_AUTHENTICATED = "is_authenticated"

    if (result.resultCode == Activity.RESULT_OK) {
      val data = result.data
      if (data != null) {
        val isOpenKeyChainAuthenticated = data.getBooleanExtra(EXTRA_IS_AUTHENTICATED, false);
        if (isOpenKeyChainAuthenticated) {
          chatModel.controller.appPrefs.is_openkeychain_authenticated.set(isOpenKeyChainAuthenticated)
          prepEncryptedMessage(chatModel, chat.chatInfo)
        } else {
          AlertManager.shared.showAlertMsg(
            title = generalGetString(R.string.unable_to_encrypt),
            text = generalGetString(R.string.open_key_chain_not_allowed),
            confirmText = generalGetString(R.string.ok),
            onConfirm = {
              AlertManager.shared.hideAlert()
            }
          )
        }
      } else {
        Log.i(TAG, "ComposeView: data is null")
      }
    } else {
      AlertManager.shared.showAlertMsg(
        title = generalGetString(R.string.unable_to_encrypt),
        text = generalGetString(R.string.open_key_chain_not_allowed),
        confirmText = generalGetString(R.string.ok),
        onConfirm = {
          AlertManager.shared.hideAlert()
        }
      )
    }
  }

  fun cancelLinkPreview() {
    val uri = composeState.value.linkPreview?.uri
    if (uri != null) {
      cancelledLinks.add(uri)
    }
    pendingLinkUrl.value = null
    composeState.value = composeState.value.copy(preview = ComposePreview.NoPreview)
  }

  fun cancelImages() {
    composeState.value = composeState.value.copy(preview = ComposePreview.NoPreview)
    chosenContent.value = emptyList()
  }

  fun cancelFile() {
    composeState.value = composeState.value.copy(preview = ComposePreview.NoPreview)
    chosenFile.value = null
  }

  fun truncateToWords(s: String): String {
    var acc = ""
    val word = StringBuilder()
    for (c in s) {
      if (c.isLetter() || c.isDigit()) {
        word.append(c)
      } else {
        acc = acc + word.toString() + c
        word.clear()
      }
    }
    return acc
  }

  suspend fun sendLiveMessage() {
    val cs = composeState.value
    val typedMsg = cs.message
    if ((cs.sendEnabled() || cs.contextItem is ComposeContextItem.QuotedItem) && (cs.liveMessage == null || !cs.liveMessage.sent)) {
      val ci = sendMessageAsync(typedMsg, live = true)
      if (ci != null) {
        composeState.value = composeState.value.copy(liveMessage = LiveMessage(ci, typedMsg = typedMsg, sentMsg = typedMsg, sent = true))
      }
    } else if (cs.liveMessage == null) {
      val cItem = chatModel.addLiveDummy(chat.chatInfo)
      composeState.value = composeState.value.copy(liveMessage = LiveMessage(cItem, typedMsg = typedMsg, sentMsg = typedMsg, sent = false))
    }
  }

  fun liveMessageToSend(lm: LiveMessage, t: String): String? {
    val s = if (t != lm.typedMsg) truncateToWords(t) else t
    return if (s != lm.sentMsg) s else null
  }

  suspend fun updateLiveMessage() {
    val typedMsg = composeState.value.message
    val liveMessage = composeState.value.liveMessage
    if (liveMessage != null) {
      val sentMsg = liveMessageToSend(liveMessage, typedMsg)
      if (sentMsg != null) {
        val ci = sendMessageAsync(sentMsg, live = true)
        if (ci != null) {
          composeState.value = composeState.value.copy(liveMessage = LiveMessage(ci, typedMsg = typedMsg, sentMsg = sentMsg, sent = true))
        }
      } else if (liveMessage.typedMsg != typedMsg) {
        composeState.value = composeState.value.copy(liveMessage = liveMessage.copy(typedMsg = typedMsg))
      }
    }
  }

  fun cancelLiveMessage() {
    composeState.value = composeState.value.copy(liveMessage = null)
    chatModel.removeLiveDummy()
  }

  @Composable
  fun previewView() {
    when (val preview = composeState.value.preview) {
      ComposePreview.NoPreview -> {}
      is ComposePreview.CLinkPreview -> ComposeLinkView(preview.linkPreview, ::cancelLinkPreview)
      is ComposePreview.MediaPreview -> ComposeImageView(
        preview,
        ::cancelImages,
        cancelEnabled = !composeState.value.editing
      )
      is ComposePreview.FilePreview -> ComposeFileView(
        preview.fileName,
        ::cancelFile,
        cancelEnabled = !composeState.value.editing
      )
      else -> {}
    }
  }

  @Composable
  fun contextItemView() {
    val isZeroKnowledge = if (chat.chatInfo is ChatInfo.Group) chat.chatInfo.groupInfo.displayName.startsWith("*", ignoreCase = false) else false
    when (val contextItem = composeState.value.contextItem) {
      ComposeContextItem.NoContextItem -> {}
      is ComposeContextItem.QuotedItem -> ContextItemView(contextItem.chatItem, Icons.Outlined.Reply, isZeroKnowledge) {
        composeState.value = composeState.value.copy(contextItem = ComposeContextItem.NoContextItem)
      }
      is ComposeContextItem.EditingItem -> ContextItemView(contextItem.chatItem, Icons.Filled.Edit, isZeroKnowledge) {
        clearState()
      }
    }
  }

  val mainScope = MainScope()

  LaunchedEffect(chatModel.sharedContent.value) {
    when (val shared = chatModel.sharedContent.value) {
      is SharedContent.Text -> mainScope.launch { onMessageChange(shared.text)  }
      is SharedContent.Media -> processPickedMedia(shared.uris, shared.text)
      is SharedContent.File -> processPickedFile(shared.uri, shared.text)
      null -> {}
    }
    chatModel.sharedContent.value = null
  }

 /* Column(Modifier.background(Color.White)) {
    Divider(color = DividerColor)
    contextItemView()
    when {
      composeState.value.editing && composeState.value.preview is ComposePreview.FilePreview -> {}
      else -> previewView()
    }
    Row(
      modifier = Modifier.padding(start = 4.dp, end = 8.dp, top = 15.dp, bottom = 15.dp),
      verticalAlignment = Alignment.Bottom,
      horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
      val attachEnabled = !composeState.value.editing
      val wipeCommandEnabled = composeState.value.sendEnabled
      val stopRecOnNextClick = remember { mutableStateOf(false) }
      val recState: MutableState<RecordingState> = remember { mutableStateOf(RecordingState.NotStarted) }
      val rec: Recorder = remember { RecorderNative(MAX_VOICE_SIZE_FOR_SENDING) }
      val startRecording: () -> Unit = {
        recState.value = RecordingState.Started(
          filePath = rec.start { progress: Int?, finished: Boolean ->
            val state = recState.value
            if (state is RecordingState.Started && progress != null) {
              recState.value = if (!finished) {
                RecordingState.Started(state.filePath, progress)
              } else {
                RecordingState.Finished(state.filePath, progress)
              }
            }
          },
        )
      }
      val stopRecordingAndAddAudio: () -> Unit = {
        recState.value.filePathNullable?.let {
          recState.value = RecordingState.Finished(it, rec.stop())
        }
      }



      if (recState.value is RecordingState.NotStarted) {
        Box(Modifier.padding(start = 5.dp, end = 5.dp)) {
          Icon(
            Icons.Filled.AttachFile,
            contentDescription = stringResource(R.string.attach),
            tint = if (attachEnabled) Color.Black else Color.Gray,
            modifier = Modifier
              .size(28.dp)
              .clip(CircleShape)
              .clickable {
                if (attachEnabled) {
                  showChooseAttachment()
                }
              }
          )
        }
        if (chat.chatInfo is ChatInfo.Direct) {
          Box(
            Modifier
              .padding(start = 5.dp, end = 5.dp)
          ) {
            Icon(
              Icons.Outlined.DeleteSweep,
              contentDescription = stringResource(R.string.wipe_command),
              tint = if (wipeCommandEnabled()) Color.Black else Color.Gray,
              modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .clickable {
                  if (wipeCommandEnabled()) {
                    sendMessage()
                  }
                }
            )
          }
        }
        Box(
          modifier = Modifier
            .padding(start = 5.dp, end = 5.dp)
            .fillMaxWidth()
            .weight(1f)
        ) {
          SendMsgView(
            composeState,
            sendMessage = {
              if (chat.chatInfo is ChatInfo.Group) {
                chatModel.localContactViewModel?.findLocalContact(chatModel.currentUser.value?.userId.toString(), chat.chatInfo.groupInfo.id)
                prepEncryptedMessage(chatModel, chat.chatInfo)
              } else if (chat.chatInfo is ChatInfo.Direct) {
                prepEncryptedMessage(chatModel, chat.chatInfo)
              }
              resetLinkPreview()
            },
            ::onMessageChange,
            textStyle
          )
        }
       *//* val icon = if (composeState.value.editing) Icons.Filled.Check else Icons.Outlined.Send
        val color = if (composeState.value.sendEnabled()) Color.Black else HighOrLowlight*//*
        val icon = if (composeState.value.liveMessage?.sent == false && composeState.value.message.isEmpty()) {
          Icons.Filled.Cancel
        } else {
          if (composeState.value.editing) Icons.Filled.Check else Icons.Outlined.Send
        }
        val voiceMessageIcon = if (composeState.value.editing) Icons.Filled.Check else Icons.Outlined.Mic
        val color = if (composeState.value.sendEnabled()) Color.Black else HighOrLowlight
        val showVoiceButton = composeState.value.message.isEmpty() && !composeState.value.editing &&
            composeState.value.liveMessage == null && (composeState.value.preview is ComposePreview.NoPreview || recState.value is RecordingState.Started)
        val permissionsState = rememberMultiplePermissionsState(listOf(Manifest.permission.RECORD_AUDIO))
        val sendButtonSize = remember { Animatable(36f) }
        val sendButtonAlpha = remember { Animatable(1f) }
        val contact = remember { derivedStateOf { (chatModel.getContactChat(chat.chatInfo.apiId)?.chatInfo as? ChatInfo.Direct)?.contact } }

        Box(Modifier.padding(start = 5.dp, end = 5.dp)) {
          Icon(
            icon,
            contentDescription = stringResource(R.string.send),
            tint = color,
            modifier = Modifier
              .size(28.dp)
              .clip(CircleShape)
              .clickable {
                if (composeState.value.sendEnabled()) {
                  prepEncryptedMessage(chatModel, chat.chatInfo)
                  resetLinkPreview()
                }
              }
          )
        }
      } else {
        RecordVoiceMessageView(
          chatModel,
          composeState,
          recState,
          sendMessage = { sendMessage() },
          stopRecordingAndAddAudio = stopRecordingAndAddAudio
        )
      }
    }
  }*/

  Column(Modifier.background(Color.White)) {

    Divider(color = DividerColor)
    contextItemView()
    when {
      composeState.value.editing && composeState.value.preview is ComposePreview.FilePreview -> {}
      else -> previewView()
    }
    Row(
      modifier = Modifier.padding(start = 4.dp, end = 8.dp, top = 15.dp, bottom = 15.dp),
      verticalAlignment = Alignment.Bottom,
      horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
      val attachEnabled = !composeState.value.editing
      val wipeCommandEnabled = composeState.value.sendEnabled
      val stopRecOnNextClick = remember { mutableStateOf(false) }
      val recState: MutableState<RecordingState> = remember { mutableStateOf(RecordingState.NotStarted) }
      val rec: Recorder = remember { RecorderNative(MAX_VOICE_SIZE_FOR_SENDING) }
      val startRecording: () -> Unit = {
        recState.value = RecordingState.Started(
          filePath = rec.start { progress: Int?, finished: Boolean ->
            val state = recState.value
            if (state is RecordingState.Started && progress != null) {
              recState.value = if (!finished) {
                RecordingState.Started(state.filePath, progress)
              } else {
                RecordingState.Finished(state.filePath, progress)
              }
            }
          },
        )
      }
      val stopRecordingAndAddAudio: () -> Unit = {
        recState.value.filePathNullable?.let {
          recState.value = RecordingState.Finished(it, rec.stop())
        }
      }



      if (recState.value is RecordingState.NotStarted) {
        Box(Modifier.padding(start = 2.dp, end = 2.dp)) {
          Icon(
            Icons.Filled.AttachFile,
            contentDescription = stringResource(R.string.attach),
            tint = if (attachEnabled) Color.Black else Color.Gray,
            modifier = Modifier
              .size(28.dp)
              .clip(CircleShape)
              .clickable {
                if (attachEnabled) {
                  showChooseAttachment()
                }
              }
          )
        }
        if (chat.chatInfo is ChatInfo.Direct) {
          Box(
            Modifier
              .padding(start = 5.dp, end = 5.dp)
          ) {
            if(composeState.value.inProgress){
              CircularProgressIndicator(
                Modifier
                  .size(28.dp)
                  .padding(4.dp),
                color = Color.Black,
                strokeWidth = 3.dp
              )
            } else {
              Icon(
                Icons.Outlined.DeleteSweep,
                contentDescription = stringResource(R.string.wipe_command),
                tint = if (wipeCommandEnabled()) Color.Black else Color.Gray,
                modifier = Modifier
                  .size(28.dp)
                  .clip(CircleShape)
                  .clickable {
                    if (wipeCommandEnabled()) {
                      sendMessage()
                    }
                  }
              )
            }
          }
        }
        Box(
          modifier = Modifier
            .padding(start = 2.dp, end = 2.dp)
            .fillMaxWidth()
            .weight(1f)
        ) {
          SendMsgView(
            composeState,
            sendMessage = {
              if (chat.chatInfo is ChatInfo.Group) {
                chatModel.localContactViewModel?.findLocalContact(chatModel.currentUser.value?.userId.toString(), chat.chatInfo.groupInfo.id)
                prepEncryptedMessage(chatModel, chat.chatInfo)
              } else if (chat.chatInfo is ChatInfo.Direct) {
                prepEncryptedMessage(chatModel, chat.chatInfo)
              }
              resetLinkPreview()
            },
            ::onMessageChange,
            textStyle
          )
        }
        val icon = if (composeState.value.liveMessage?.sent == false && composeState.value.message.isEmpty()) {
          Icons.Filled.Cancel
        } else {
          if (composeState.value.editing) Icons.Filled.Check else Icons.Outlined.Send
        }
        val voiceMessageIcon = if (composeState.value.editing) Icons.Filled.Check else Icons.Outlined.Mic
        val color = if (composeState.value.sendEnabled()) Color.Black else HighOrLowlight
        val showVoiceButton = composeState.value.message.isEmpty() && !composeState.value.editing &&
            composeState.value.liveMessage == null && (composeState.value.preview is ComposePreview.NoPreview || recState.value is RecordingState.Started)
        val permissionsState = rememberMultiplePermissionsState(listOf(Manifest.permission.RECORD_AUDIO))
        val sendButtonSize = remember { Animatable(36f) }
        val sendButtonAlpha = remember { Animatable(1f) }
        val contact = remember { derivedStateOf { (chatModel.getContactChat(chat.chatInfo.apiId)?.chatInfo as? ChatInfo.Direct)?.contact } }



        if (showVoiceButton) {
          Box(Modifier.padding(start = 2.dp, end = 2.dp)) {
            if (!permissionsState.allPermissionsGranted) {
              IconButton(
                { permissionsState.launchMultiplePermissionRequest() },
                Modifier.size(30.dp)
              ) {
                Icon(
                  voiceMessageIcon,
                  contentDescription = stringResource(R.string.voice_message),
                  tint = Color.Black,
                  modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                )
              }
            } else {
              IconButton({
                stopRecOnNextClick.value = true
                if (recState.value is RecordingState.NotStarted) startRecording()
              }, Modifier.size(30.dp)) {
                Icon(
                  voiceMessageIcon,
                  contentDescription = stringResource(R.string.voice_message),
                  tint = Color.Black,
                  modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                )
              }
            }
          }

          if (composeState.value.preview !is ComposePreview.VoicePreview
            && composeState.value.contextItem is ComposeContextItem.NoContextItem
          ) {
            Box(Modifier.padding(start = 2.dp, end = 2.dp)) {
              Icon(
                Icons.Filled.Bolt,
                contentDescription = stringResource(R.string.live_message),
                tint = if (attachEnabled) Color.Black else Color.Gray,
                modifier = Modifier
                  .size(28.dp)
                  .clip(CircleShape)
                  .clickable {
                    startLiveMessage(
                      scope,
                      ::sendLiveMessage,
                      ::updateLiveMessage,
                      sendButtonSize,
                      sendButtonAlpha,
                      composeState,
                      chatModel.controller.appPrefs.liveMessageAlertShown
                    )
                  }
              )
            }
          }
        } else {
          Box(Modifier.padding(start = 2.dp, end = 2.dp)) {
            IconButton(
              { /*sendMessage()*/
                if (composeState.value.liveMessage?.sent == false && composeState.value.message.isEmpty()) {
                  cancelLiveMessage()
                } else {
                  prepEncryptedMessage(chatModel, chat.chatInfo)
                }
              },
              Modifier.size(30.dp)
            ) {
              Icon(
                icon,
                contentDescription = stringResource(R.string.icon_descr_send_message),
                tint = color,
                modifier = Modifier
                  .size(28.dp)
                  .clip(CircleShape)
              )
            }
          }
        }
      } else {
        RecordVoiceMessageView(
          chatModel,
          composeState,
          recState,
          sendMessage = { sendMessage() },
          stopRecordingAndAddAudio = stopRecordingAndAddAudio
        )
      }
    }
    /*  Column(
        Modifier.fillMaxWidth().background(Color.White)
      ) {


       */
    /* Row(
          modifier = Modifier
          //.fillMaxWidth()
          //.height(45.dp)
          .padding(start = 10.dp, end = 10.dp, top = 5.dp, bottom = 5.dp),
          verticalAlignment = Alignment.CenterVertically){

          val attachEnabled = !composeState.value.editing
          val wipeCommandEnabled = composeState.value.sendEnabled

          Box(Modifier.padding( start = 5.dp, end = 5.dp)) {
            Icon(
              Icons.Outlined.Send,
              contentDescription = stringResource(R.string.send),
              tint = if (wipeCommandEnabled()) Color.Black else Color.Gray,
              modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .clickable {
                  if (wipeCommandEnabled()) {
                    sendMessage()
                  }
                }
            )
          }
          Box(Modifier.padding( start = 5.dp, end = 5.dp)) {
            Icon(
              Icons.Outlined.DeleteSweep,
              contentDescription = stringResource(R.string.send),
              tint = if (wipeCommandEnabled()) Color.Black else Color.Gray,
              modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .clickable {
                  if (wipeCommandEnabled()) {
                    sendMessage()
                  }
                }
            )
          }
          SendMsgView(
            composeState,
            modifier = Modifier
              //.align(CenterHorizontally)
              .padding(start = 15.dp, end = 15.dp, top = 15.dp, bottom = 15.dp)
              .fillMaxWidth()
              .weight(1f),
            sendMessage = {
              if (chat.chatInfo is ChatInfo.Group) {
                chatModel.localContactViewModel?.findLocalContact(chatModel.currentUser.value?.userId.toString(), chat.chatInfo.groupInfo.id)
                prepEncryptedMessage(chatModel, chat.chatInfo)
              }
              else if (chat.chatInfo is ChatInfo.Direct) {
                prepEncryptedMessage(chatModel, chat.chatInfo)
              }
              resetLinkPreview()
            },
            ::onMessageChange,
            textStyle
          )
          Box(Modifier.padding( start = 5.dp, end = 5.dp)) {
            Icon(
              Icons.Outlined.DeleteSweep,
              contentDescription = stringResource(R.string.send),
              tint = if (wipeCommandEnabled()) Color.Black else Color.Gray,
              modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .clickable {
                  if (wipeCommandEnabled()) {
                    sendMessage()
                  }
                }
            )
          }
        }*//*
 */
    /*   Card(
        shape = RoundedCornerShape(10.dp),
        backgroundColor = MaterialTheme.colors.background,
        modifier = Modifier
          .align(Alignment.CenterHorizontally)
          .padding(start = 30.dp, end = 30.dp)
          .fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .height(45.dp)
            .padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          BasicTextField(
            value = search.value,
            onValueChange = { search.value = it },
            decorationBox = { innerTextField ->
              Box(
                contentAlignment = Alignment.CenterStart
              ) {
                if (search.value.isEmpty()) {
                  Text(
                    text = generalGetString(R.string.search),
                  )
                }
                innerTextField()
              }
            },
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f)
              .focusRequester(focusRequester)
              .onFocusChanged {
                if (it.hasFocus) {
                  focusManager.clearFocus()
                  chatModel.showSearchView.value = true
                }
              },
            textStyle = MaterialTheme.typography.body1.copy(color = MaterialTheme.colors.onSurface),
            keyboardOptions = KeyboardOptions(
              capitalization = KeyboardCapitalization.None,
              autoCorrect = false
            ),
            singleLine = true,
            cursorBrush = SolidColor(HighOrLowlight)
          )
          Spacer(
            modifier = Modifier
              .padding(end = 5.dp)
          )
          IconButton(onClick = { *//**//*TODO*//**//* }) {
          Icon(
            Icons.Outlined.Mood,
            contentDescription = "icon",
            tint = Color.White,
            modifier = Modifier
              .size(20.dp)
          )
        }
      }
    }*/
    /*
    }*/
    /*  Column (Modifier.background(Color.White)) {
        contextItemView()
        when {
          composeState.value.editing && composeState.value.preview is ComposePreview.FilePreview -> {}
          else -> previewView()
        }
        Row(
          modifier = Modifier.padding(start = 4.dp, end = 8.dp, top = 20.dp, bottom = 20.dp),
          verticalAlignment = Alignment.Bottom,
          horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
          val attachEnabled = !composeState.value.editing
          val wipeCommandEnabled = composeState.value.sendEnabled
          Box(Modifier.padding(start = 5.dp, end = 5.dp)) {
            Icon(
              Icons.Filled.AttachFile,
              contentDescription = stringResource(R.string.attach),
              tint = if (attachEnabled) MaterialTheme.colors.primary else Color.Gray,
              modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .clickable {
                  if (attachEnabled) {
                    showChooseAttachment()
                  }
                }
            )
          }
          Box(modifier = Modifier
            .padding(start = 5.dp, end = 5.dp)
            .fillMaxWidth()
            .weight(1f)){
            SendMsgView(
              composeState,
              sendMessage = {
                if (chat.chatInfo is ChatInfo.Group) {
                  chatModel.localContactViewModel?.findLocalContact(chatModel.currentUser.value?.userId.toString(), chat.chatInfo.groupInfo.id)
                  prepEncryptedMessage(chatModel, chat.chatInfo)
                }
                else if (chat.chatInfo is ChatInfo.Direct) {
                  prepEncryptedMessage(chatModel, chat.chatInfo)
                }
                resetLinkPreview()
              },
              ::onMessageChange,
              textStyle
            )
          }
          Box(Modifier
            .padding(start = 5.dp, end = 5.dp)) {
            Icon(
              Icons.Outlined.DeleteSweep,
              contentDescription = stringResource(R.string.wipe_command),
              tint = if (wipeCommandEnabled()) MaterialTheme.colors.primary else Color.Gray,
              modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .clickable {
                  if (wipeCommandEnabled()) {
                    sendMessage()
                  }
                }
            )
          }
          Box(Modifier.padding( start = 5.dp, end = 5.dp)) {
            Icon(
              Icons.Outlined.Send,
              contentDescription = stringResource(R.string.send),
              tint = if (wipeCommandEnabled()) Color.Black else Color.Gray,
              modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .clickable {
                  if (wipeCommandEnabled()) {
                    sendMessage()
                  }
                }
            )
          }
        }
      }*/
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

class PickMultipleImagesFromGallery: ActivityResultContract<Int, List<Uri>>() {
  override fun createIntent(context: Context, input: Int) =
    Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI).apply {
      putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
      type = "image/*"
    }

  override fun parseResult(resultCode: Int, intent: Intent?): List<Uri> =
    if (intent?.data != null)
      listOf(intent.data!!)
    else if (intent?.clipData != null)
      with(intent.clipData!!) {
        val uris = ArrayList<Uri>()
        for (i in 0 until kotlin.math.min(itemCount, 10)) {
          val uri = getItemAt(i).uri
          if (uri != null) uris.add(uri)
        }
        if (itemCount > 10) {
          AlertManager.shared.showAlertMsg(R.string.images_limit_title, R.string.images_limit_desc)
        }
        uris
      }
    else
      emptyList()
}

class PickMultipleVideosFromGallery: ActivityResultContract<Int, List<Uri>>() {
  override fun createIntent(context: Context, input: Int) =
    Intent(Intent.ACTION_PICK, MediaStore.Video.Media.INTERNAL_CONTENT_URI).apply {
      putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
      type = "video/*"
    }

  override fun parseResult(resultCode: Int, intent: Intent?): List<Uri> =
    if (intent?.data != null)
      listOf(intent.data!!)
    else if (intent?.clipData != null)
      with(intent.clipData!!) {
        val uris = ArrayList<Uri>()
        for (i in 0 until kotlin.math.min(itemCount, 10)) {
          val uri = getItemAt(i).uri
          if (uri != null) uris.add(uri)
        }
        if (itemCount > 10) {
          AlertManager.shared.showAlertMsg(R.string.videos_limit_title, R.string.videos_limit_desc)
        }
        uris
      }
    else
      emptyList()
}

class PickFromGallery: ActivityResultContract<Int, Uri?>() {
  override fun createIntent(context: Context, input: Int) =
    Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI).apply {
      type = "image/*"
    }

  override fun parseResult(resultCode: Int, intent: Intent?): Uri? = intent?.data
}
