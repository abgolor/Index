package chat.echo.app.views.chat.item

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.InsertPhoto
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.echo.app.R
import chat.echo.app.model.*
import chat.echo.app.ui.theme.*
import chat.echo.app.views.helpers.*

@Composable
fun CIPhotoView(
  file: CIFile?,
  imageProvider: () -> ImageGalleryProvider,
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
        Icons.Filled.InsertPhoto,
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
    if (file != null) {
      when (file.fileStatus) {
        CIFileStatus.RcvInvitation -> {
          return if (fileSizeValid()) {
            "Click to Download Photo"
          } else {
            "File too Large"
          }
        }
        CIFileStatus.RcvAccepted ->
          return "Downloading Photo"
        CIFileStatus.RcvComplete -> {
          return "Photo"
        }
        CIFileStatus.SndComplete -> {
          return "Photo"
        }
        CIFileStatus.SndStored -> {
          return "Photo Sent"
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
          AlertManager.shared.showAlertMsg(
            generalGetString(R.string.waiting_for_file),
            String.format(generalGetString(R.string.contact_sent_large_file), formatBytes(getMaxFileSize(file.fileProtocol)))
          )
        CIFileStatus.RcvComplete -> {
          ModalManager.shared.showCustomModal(animated = false) { close ->
            ImageFullScreenView(imageProvider, close, deleteMessage, chatItem)
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
