package chat.echo.app.views.chat.item

import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import chat.echo.app.R
import chat.echo.app.model.*
import chat.echo.app.ui.theme.*
import chat.echo.app.views.helpers.*
import kotlinx.datetime.Clock
import java.io.File

@Composable
fun CIFileView(
  file: CIFile?,
  imageProvider: () -> ImageGalleryProvider,
  edited: Boolean,
  receiveFile: (Long) -> Unit,
  chatItem: ChatItem,
  deleteMessage: (Long, CIDeleteMode) -> Unit
) {
  val context = LocalContext.current
  val saveFileLauncher = rememberSaveFileLauncher(cxt = context, ciFile = file)
  val filePath = getLoadedFilePath(context, file)

  @Composable
  fun fileIcon(
    icon: ImageVector,
    innerIcon: ImageVector? = null,
    color: Color = if (isInDarkTheme()) FileDark else FileLight
  ) {
    Box(
      contentAlignment = Alignment.Center
    ) {
      Icon(
        icon,
        stringResource(R.string.icon_descr_file),
        Modifier.fillMaxSize(),
        tint = color
      )
      if (innerIcon != null) {
        Icon(
          innerIcon,
          stringResource(R.string.icon_descr_file),
          Modifier
            .size(32.dp)
            .padding(top = 12.dp),
          tint = Color.White
        )
      }
    }
  }

  fun fileSizeValid(): Boolean {
    if (file != null) {
      return file.fileSize <= getMaxFileSize(file.fileProtocol)
    }
    return false
  }

  @Composable
  fun fileType(): String {
    if (file != null){

      when (file.fileStatus) {
        CIFileStatus.RcvInvitation -> {
          return if (fileSizeValid()) {
            "Click to Download File"
          } else {
            "File too Large"
          }
        }
        CIFileStatus.RcvAccepted ->
          return "Downloading File"
        CIFileStatus.RcvComplete -> {

          val sourceFile = File(filePath)
          val sourceUri = FileProvider.getUriForFile(context, context.packageName + ".provider", sourceFile)
          val mediaTypeRaw = context.contentResolver.getType(sourceUri)
          if (filePath != null) {
              if (mediaTypeRaw?.startsWith("video") == true)
                 return "Video"
            return if (mediaTypeRaw?.startsWith("image") == true)
              "Photo"
            else
              file.fileName
          } else {
            return "File Not Found"
          }
        }
        CIFileStatus.SndComplete -> {
          val sourceFile = filePath?.let { File(it) }
          val sourceUri = sourceFile?.let { FileProvider.getUriForFile(context, context.packageName + ".provider", it) }
          val mediaTypeRaw = sourceUri?.let { context.contentResolver.getType(it) }
          if (filePath != null) {
            if (mediaTypeRaw?.startsWith("video") == true)
              return "Video"
            return if (mediaTypeRaw?.startsWith("image") == true)
              "Photo"
            else
              file.fileName
          } else {
            return "File Not Found"
          }
        }
        CIFileStatus.SndStored -> {
          val sourceFile = File(filePath)
          val sourceUri = FileProvider.getUriForFile(context, context.packageName + ".provider", sourceFile)
          val mediaTypeRaw = context.contentResolver.getType(sourceUri)
          if (filePath != null) {
            if (mediaTypeRaw?.startsWith("video") == true)
              return "Video Sent"
            return if (mediaTypeRaw?.startsWith("image") == true)
              "Photo Sent"
            else
              "File Sent"
          } else {
            return "File Not Found"
          }
        }
        else -> return ""
      }
    }
    return ""
  }

  fun fileAction() {
    if (file != null) {
      when (file.fileStatus) {
        CIFileStatus.RcvInvitation -> {
          if (fileSizeValid()) {
            receiveFile(file.fileId)
          } else {
            AlertManager.shared.showAlertMsg(
              generalGetString(R.string.large_file),
              String.format(generalGetString(R.string.contact_sent_large_file), formatBytes(getMaxFileSize(file.fileProtocol)))
            )
          }
        }
        CIFileStatus.RcvAccepted ->
          when (file.fileProtocol) {
            FileProtocol.XFTP ->
              AlertManager.shared.showAlertMsg(
                generalGetString(R.string.waiting_for_file),
                generalGetString(R.string.file_will_be_received_when_contact_completes_uploading)
              )
            FileProtocol.SMP ->
              AlertManager.shared.showAlertMsg(
                generalGetString(R.string.waiting_for_file),
                generalGetString(R.string.file_will_be_received_when_contact_is_online)
              )
          }
        CIFileStatus.RcvComplete -> {
          if (filePath != null) {

            val sourceFile = File(filePath)
            val sourceUri = FileProvider.getUriForFile(context, context.packageName + ".provider", sourceFile)
            val mediaTypeRaw = context.contentResolver.getType(sourceUri)

              if (mediaTypeRaw?.startsWith("video") == true) {
                ModalManager.shared.showCustomModal(animated = false) { close ->
                  VideoPlayerView(ciFile = file, chatItem = chatItem, close = close, deleteMessage = deleteMessage)
                }
              } else if (mediaTypeRaw?.startsWith("image") == true){
                ModalManager.shared.showCustomModal(animated = false) { close ->
                  ImageFullScreenView(imageProvider, close, deleteMessage, chatItem)
                }
              } else {
                saveFileLauncher.launch(file.fileName)
              }
          } else {
            Toast.makeText(context, generalGetString(R.string.file_not_found), Toast.LENGTH_SHORT).show()
          }
        }
        else -> {}
      }
    }
  }

  @Composable
  fun progressIndicator() {
    CircularProgressIndicator(
      Modifier.size(32.dp),
      color = if (isInDarkTheme()) FileDark else FileLight,
      strokeWidth = 4.dp
    )
  }

  @Composable
  fun fileIndicator() {
    Box(
      Modifier
        .size(42.dp)
        .clip(RoundedCornerShape(4.dp))
        .clickable(onClick = { fileAction() }),
      contentAlignment = Alignment.Center
    ) {
      if (file != null) {
        when (file.fileStatus) {
         is  CIFileStatus.SndStored -> {
            when (file.fileProtocol) {
              FileProtocol.XFTP -> progressIndicator()
              FileProtocol.SMP -> fileIcon(Icons.Filled.InsertDriveFile)
            }
          }
         is CIFileStatus.SndTransfer -> {
            when (file.fileProtocol) {
              FileProtocol.XFTP -> progressCircle(file.fileStatus.sndProgress, file.fileStatus.sndTotal)
              FileProtocol.SMP -> progressIndicator()
            }
          }
          is CIFileStatus.SndComplete -> fileIcon(Icons.Filled.InsertDriveFile, innerIcon = Icons.Filled.Check)
          is  CIFileStatus.SndCancelled -> fileIcon(Icons.Filled.InsertDriveFile, innerIcon = Icons.Outlined.Close)
          is CIFileStatus.SndError -> fileIcon(Icons.Filled.InsertDriveFile, innerIcon = Icons.Outlined.Close)
          is CIFileStatus.RcvInvitation ->
            if (fileSizeValid())
              fileIcon(Icons.Filled.InsertDriveFile, innerIcon = Icons.Outlined.ArrowDownward, color = MaterialTheme.colors.primary)
            else
              fileIcon(Icons.Filled.InsertDriveFile, innerIcon = Icons.Outlined.PriorityHigh, color = WarningOrange)
          is CIFileStatus.RcvAccepted -> fileIcon(Icons.Filled.InsertDriveFile, innerIcon = Icons.Outlined.MoreHoriz)
          is CIFileStatus.RcvTransfer -> progressIndicator()
          is  CIFileStatus.RcvComplete -> {
            if(filePath!= null){

              val sourceFile = File(filePath)
              val sourceUri = FileProvider.getUriForFile(context, context.packageName + ".provider", sourceFile)
              val mediaTypeRaw = context.contentResolver.getType(sourceUri)
              if (mediaTypeRaw?.startsWith("video") == true)
                fileIcon(icon = Icons.Filled.VideoFile)
              if (mediaTypeRaw?.startsWith("image") == true)
                fileIcon(icon = Icons.Filled.Photo)
              else
                fileIcon(Icons.Filled.InsertDriveFile)
            }
          }
         is  CIFileStatus.RcvCancelled -> fileIcon(Icons.Filled.InsertDriveFile, innerIcon = Icons.Outlined.Close)
         is  CIFileStatus.RcvError -> fileIcon(Icons.Filled.InsertDriveFile, innerIcon = Icons.Outlined.Close)
        }
      } else {
        fileIcon(Icons.Filled.InsertDriveFile)
      }
    }
  }

  Row(
    Modifier.padding(top = 4.dp, bottom = 6.dp, start = 6.dp, end = 12.dp),
    verticalAlignment = Alignment.Bottom,
    horizontalArrangement = Arrangement.spacedBy(2.dp)
  ) {
    fileIndicator()
    val metaReserve = if (edited)
      "                     "
    else
      "                 "
    if (file != null) {
      Column(
        horizontalAlignment = Alignment.Start
      ) {
        Text(
          fileType(),
          maxLines = 1
        )
        Text(
          formatBytes(file.fileSize) + metaReserve,
          color = HighOrLowlight,
          fontSize = 14.sp,
          maxLines = 1
        )
      }
    } else {
      Text(metaReserve)
    }
  }
}

@Composable
fun progressCircle(progress: Long, total: Long) {
  val angle = 360f * (progress.toDouble() / total.toDouble()).toFloat()
  val strokeWidth = with(LocalDensity.current) { 3.dp.toPx() }
  val strokeColor = if (isInDarkTheme()) FileDark else FileLight
  Surface(
    Modifier.drawRingModifier(angle, strokeColor, strokeWidth),
    color = Color.Transparent,
    shape = MaterialTheme.shapes.small.copy(CornerSize(percent = 50))
  ) {
    Box(Modifier.size(32.dp))
  }
}


class ChatItemProvider: PreviewParameterProvider<ChatItem> {
  private val sentFile = ChatItem(
    chatDir = CIDirection.DirectSnd(),
    meta = CIMeta.getSample(1, Clock.System.now(), "", CIStatus.SndSent(), itemEdited = true),
    content = CIContent.SndMsgContent(msgContent = MsgContent.MCFile("")),
    quotedItem = null,
    file = CIFile.getSample(fileStatus = CIFileStatus.SndComplete)
  )
  private val fileChatItemWtFile = ChatItem(
    chatDir = CIDirection.DirectRcv(),
    meta = CIMeta.getSample(1, Clock.System.now(), "", CIStatus.RcvRead(), ),
    content = CIContent.RcvMsgContent(msgContent = MsgContent.MCFile("")),
    quotedItem = null,
    file = null
  )
  override val values = listOf(
    sentFile,
    ChatItem.getFileMsgContentSample(),
    ChatItem.getFileMsgContentSample(fileName = "some_long_file_name_here", fileStatus = CIFileStatus.RcvInvitation),
    ChatItem.getFileMsgContentSample(fileStatus = CIFileStatus.RcvAccepted),
    ChatItem.getFileMsgContentSample(fileStatus = CIFileStatus.RcvTransfer(rcvProgress = 7, rcvTotal = 10)),
    ChatItem.getFileMsgContentSample(fileStatus = CIFileStatus.RcvCancelled),
    ChatItem.getFileMsgContentSample(fileSize = 1_000_000_000, fileStatus = CIFileStatus.RcvInvitation),
    ChatItem.getFileMsgContentSample(text = "Hello there", fileStatus = CIFileStatus.RcvInvitation),
    ChatItem.getFileMsgContentSample(text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.", fileStatus = CIFileStatus.RcvInvitation),
    fileChatItemWtFile
  ).asSequence()
}

/*@Preview
@Composable
fun PreviewCIFileFramedItemView(@PreviewParameter(ChatItemProvider::class) chatItem: ChatItem) {
  val showMenu = remember { mutableStateOf(false) }
  val decrypted = remember { mutableStateOf(false) }
  val message = remember { mutableStateOf("Hello there.") }
  SimpleXTheme {
    FramedItemView(ChatInfo.Direct.sampleData, chatItem, showMenu = showMenu, receiveFile = {}, deleteMessage = { _, _ -> }, decrypted = decrypted)
  }
}*/
