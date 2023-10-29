package chat.echo.app.views.chat.item

import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import chat.echo.app.R
import chat.echo.app.views.helpers.*
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import coil.size.Size
import com.google.accompanist.pager.*
import kotlinx.coroutines.launch
import androidx.compose.ui.viewinterop.AndroidView
import chat.echo.app.model.*
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.*
import java.io.File

interface ImageGalleryProvider {
  val initialIndex: Int
  val totalImagesSize: MutableState<Int>
  fun getImage(index: Int): Pair<Bitmap, Uri>?
  fun currentPageChanged(index: Int)
  fun scrollToStart()
  fun onDismiss(index: Int)
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun ImageFullScreenView(imageProvider: () -> ImageGalleryProvider, close: () -> Unit,
  deleteMessage: (Long, CIDeleteMode) -> Unit, chatItem: ChatItem) {
  val provider = remember { imageProvider() }
  val pagerState = rememberPagerState(provider.initialIndex)
  val goBack = { provider.onDismiss(pagerState.currentPage); close()
    deleteMessage(chatItem.id, CIDeleteMode.cidmInternal)
  }
  BackHandler(onBack = goBack)
  // Pager doesn't ask previous page at initialization step who knows why. By not doing this, prev page is not checked and can be blank,
  // which makes this blank page visible for a moment. Prevent it by doing the check ourselves
  LaunchedEffect(Unit) {
    if (provider.getImage(provider.initialIndex - 1) == null) {
       provider.scrollToStart()
       pagerState.scrollToPage(0)
    }
  }
  val scope = rememberCoroutineScope()
  HorizontalPager(count = remember { provider.totalImagesSize }.value, state = pagerState) { index ->
    Column(
      Modifier
        .fillMaxSize()
        .background(Color.Black)
        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = goBack)
    ) {
      var settledCurrentPage by remember { mutableStateOf(pagerState.currentPage) }
      LaunchedEffect(pagerState) {
        snapshotFlow {
          if (!pagerState.isScrollInProgress) pagerState.currentPage else settledCurrentPage
        }.collect {
          settledCurrentPage = it
        }
      }
      LaunchedEffect(settledCurrentPage) {
        // Make this pager with infinity scrolling with only 3 pages at a time when left and right pages constructs in real time
        if (settledCurrentPage != provider.initialIndex)
          provider.currentPageChanged(index)
      }
      val image = provider.getImage(index)
      if (image == null) {
        // No such image. Let's shrink total pages size or scroll to start of the list of pages to remove blank page automatically
        scope.launch {
          when (settledCurrentPage) {
            index - 1 -> provider.totalImagesSize.value = settledCurrentPage + 1
            index + 1 -> {
              provider.scrollToStart()
              pagerState.scrollToPage(0)
            }
          }
        }
      } else {
        val (imageBitmap: Bitmap, uri: Uri) = image
        var scale by remember { mutableStateOf(1f) }
        var translationX by remember { mutableStateOf(0f) }
        var translationY by remember { mutableStateOf(0f) }
        LaunchedEffect(settledCurrentPage) {
          scale = 1f
          translationX = 0f
          translationY = 0f
        }
        // I'm making a new instance of imageLoader here because if I use one instance in multiple places
        // after end of composition here a GIF from the first instance will be paused automatically which isn't what I want
        val imageLoader = ImageLoader.Builder(LocalContext.current)
          .components {
            if (Build.VERSION.SDK_INT >= 28) {
              add(ImageDecoderDecoder.Factory())
            } else {
              add(GifDecoder.Factory())
            }
          }
          .build()
        Image(
          rememberAsyncImagePainter(
            ImageRequest.Builder(LocalContext.current).data(data = uri).size(Size.ORIGINAL).build(),
            placeholder = BitmapPainter(imageBitmap.asImageBitmap()), // show original image while it's still loading by coil
            imageLoader = imageLoader
          ),
          contentDescription = stringResource(R.string.image_descr),
          contentScale = ContentScale.Fit,
          modifier = Modifier
            .graphicsLayer(
              scaleX = scale,
              scaleY = scale,
              translationX = translationX,
              translationY = translationY,
            )
            .pointerInput(Unit) {
              detectTransformGestures(
                onGesture = { _, pan, gestureZoom, _ ->
                  scale = (scale * gestureZoom).coerceIn(1f, 20f)
                  if (scale > 1) {
                    translationX += pan.x * scale
                    translationY += pan.y * scale
                  } else {
                    translationX = 0f
                    translationY = 0f
                  }
                }
              )
            }
            .fillMaxSize(),
        )
      }
    }
  }
}



@Composable
fun VideoPlayerView(ciFile: CIFile?, chatItem: ChatItem, close: () -> Unit, deleteMessage: (Long, CIDeleteMode) -> Unit) {
  val mContext = LocalContext.current
  val goBack = {close()
    deleteMessage(chatItem.id, CIDeleteMode.cidmInternal)
  }
  BackHandler(onBack = goBack)
  Column(Modifier.fillMaxSize()
    .background(Color.Black)
    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = goBack)
    , horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center){
    val filePath = getLoadedFilePath(mContext, ciFile)
    if (filePath != null) {
      val file = File(filePath)
      val fileUrl = Uri.fromFile(file).toString()

      val exoPlayer = remember (mContext){
        ExoPlayer.Builder(mContext)
          .build()
          .apply {
             val defaultDataSourceFactory = DefaultDataSource.Factory(mContext)
            val dataSourceFactory : DataSource.Factory = DefaultDataSource.Factory(
              mContext,
              defaultDataSourceFactory
            )
            val source = ProgressiveMediaSource.Factory(dataSourceFactory)
              .createMediaSource(MediaItem.fromUri(fileUrl))

            setMediaSource(source)
            prepare()
        }
      }

      exoPlayer.playWhenReady = true

      AndroidView(factory = {
        PlayerView(mContext).apply {
          player = exoPlayer
        }
      })


      /*AndroidView(factory = {context ->

      PlayerView(context).apply {
        player = mExoPlayer
      }
      })*/
    } else {
      Toast.makeText(mContext, generalGetString(R.string.file_not_found), Toast.LENGTH_SHORT).show()
    }
  }
}
