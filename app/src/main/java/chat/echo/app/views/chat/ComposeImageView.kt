package chat.echo.app.views.chat

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import chat.echo.app.R
import chat.echo.app.ui.theme.DEFAULT_PADDING_HALF
import chat.echo.app.views.chat.item.SentColorLight
import chat.echo.app.views.helpers.UploadContent
import chat.echo.app.views.helpers.base64ToBitmap

/*
@Composable
fun ComposeImageView(images: List<String>, cancelImages: () -> Unit, cancelEnabled: Boolean) {
  Row(
    Modifier
      .padding(top = 8.dp)
      .background(SentColorLight),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    LazyRow(
      Modifier.weight(1f).padding(start = DEFAULT_PADDING_HALF, end = if (cancelEnabled) 0.dp else DEFAULT_PADDING_HALF),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(DEFAULT_PADDING_HALF),
    ) {
      items(images.size) { index ->
        val imageBitmap = base64ToBitmap(images[index]).asImageBitmap()
        Image(
          imageBitmap,
          "preview image",
          modifier = Modifier.widthIn(max = 80.dp).height(60.dp)
        )
      }
    }
    if (cancelEnabled) {
      IconButton(onClick = cancelImages) {
        Icon(
          Icons.Outlined.Close,
          contentDescription = stringResource(R.string.icon_descr_cancel_image_preview),
          tint = MaterialTheme.colors.primary,
        )
      }
    }
  }
}
*/

@Composable
fun ComposeImageView(media: ComposePreview.MediaPreview, cancelImages: () -> Unit, cancelEnabled: Boolean) {
  Row(
    Modifier
      .padding(top = 8.dp)
      .background(Color.Black),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    LazyRow(
      Modifier.weight(1f).padding(start = DEFAULT_PADDING_HALF, end = if (cancelEnabled) 0.dp else DEFAULT_PADDING_HALF),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(DEFAULT_PADDING_HALF),
    ) {
      itemsIndexed(media.images) { index, item ->
        val content = media.content[index]
        if (content is UploadContent.Video) {
          Box(contentAlignment = Alignment.Center) {
            val imageBitmap = base64ToBitmap(item).asImageBitmap()
            Image(
              imageBitmap,
              "preview video",
              modifier = Modifier.widthIn(max = 80.dp).height(60.dp)
            )
            Icon(
              Icons.Default.Videocam,
              "preview video",
              Modifier
                .size(20.dp),
              tint = Color.White
            )
          }
        } else {
          val imageBitmap = base64ToBitmap(item).asImageBitmap()
          Image(
            imageBitmap,
            "preview image",
            modifier = Modifier.widthIn(max = 80.dp).height(60.dp)
          )
        }
      }
    }
    if (cancelEnabled) {
      IconButton(onClick = cancelImages) {
        Icon(
          Icons.Outlined.Close,
          contentDescription = stringResource(R.string.icon_descr_cancel_image_preview),
          tint = Color.White,
        )
      }
    }
  }
}