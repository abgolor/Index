package chat.echo.app.views.chat.item

import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import chat.echo.app.*
import chat.echo.app.R
import chat.echo.app.model.*
import chat.echo.app.ui.theme.*
import chat.echo.app.views.helpers.*
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.StyledPlayerView
import java.io.File

@Composable
fun CIVideoView(
  file: CIFile?,
  edited: Boolean,
  receiveFile: (Long) -> Unit,
  chatItem: ChatItem,
  deleteMessage: (Long, CIDeleteMode) -> Unit
) {

  @Composable
  fun fileIcon(
    innerIcon: ImageVector? = null,
    color: Color = if (isInDarkTheme()) FileDark else FileLight
  ) {
    Box(
      contentAlignment = Alignment.Center
    ) {
      Icon(
        Icons.Filled.VideoFile,
        stringResource(R.string.icon_descr_video),
        Modifier.fillMaxSize(),
        tint = color
      )
      if (innerIcon != null) {
        Icon(
          innerIcon,
          stringResource(R.string.icon_descr_video),
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
    if (file != null) {
      when (file.fileStatus) {
        CIFileStatus.RcvInvitation -> {
          return if (fileSizeValid()) {
            "Click to Download Video"
          } else {
            "Video file too Large"
          }
        }
        CIFileStatus.RcvAccepted ->
          return "Downloading Video"
        CIFileStatus.RcvComplete -> {
          return "Video"
        }
        CIFileStatus.SndComplete -> {
          return "Video"
        }
        CIFileStatus.SndStored -> {
          return "Preparing Video"
        }
        CIFileStatus.RcvCancelled -> return "Video Downloading Cancelled"
        CIFileStatus.RcvError -> return "Video Downloading Failed"
        is CIFileStatus.RcvTransfer -> return "Downloading Video"
        CIFileStatus.SndCancelled -> return "Video Uploading Cancelled"
        CIFileStatus.SndError -> return "Video Uploading Failed"
        is CIFileStatus.SndTransfer -> return "Uploading Video"
      }
    }
    return ""
  }

  val context = LocalContext.current
  val filePath = remember(file) { getLoadedFilePath(SimplexApp.context, file) }

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
                generalGetString(R.string.waiting_for_video),
                generalGetString(R.string.video_will_be_received_when_contact_completes_uploading)
              )
            FileProtocol.SMP ->
              AlertManager.shared.showAlertMsg(
                generalGetString(R.string.waiting_for_video),
                generalGetString(R.string.video_will_be_received_when_contact_is_online)
              )
          }
        CIFileStatus.RcvComplete -> {
          ModalManager.shared.showCustomModal(animated = false) { close ->
            VideoPlayerView(ciFile = file, chatItem = chatItem, close = close, deleteMessage = deleteMessage)
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
          is CIFileStatus.SndStored ->
            when (file.fileProtocol) {
              FileProtocol.XFTP -> progressIndicator()
              FileProtocol.SMP -> {}
            }
          is CIFileStatus.SndTransfer -> progressIndicator()
          is CIFileStatus.SndComplete -> fileIcon(innerIcon = Icons.Filled.Check)
          is CIFileStatus.SndCancelled -> fileIcon(innerIcon = Icons.Outlined.Close)
          is CIFileStatus.SndError -> fileIcon(innerIcon = Icons.Outlined.Close)
          is CIFileStatus.RcvInvitation ->
            if (fileSizeValid())
              fileIcon(innerIcon = Icons.Outlined.ArrowDownward, color = MaterialTheme.colors.primary)
            else
              fileIcon(innerIcon = Icons.Outlined.PriorityHigh, color = WarningOrange)
          is CIFileStatus.RcvAccepted -> fileIcon(innerIcon = Icons.Outlined.MoreHoriz, color = MaterialTheme.colors.primary)
          is CIFileStatus.RcvTransfer -> progressIndicator()
          is CIFileStatus.RcvComplete -> {
            fileIcon()
          }
          CIFileStatus.RcvCancelled -> fileIcon(innerIcon = Icons.Outlined.Close)
          CIFileStatus.RcvError -> fileIcon(innerIcon = Icons.Outlined.Close)
        }
      } else {
        fileIcon()
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