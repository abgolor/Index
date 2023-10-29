package chat.echo.app.views.newchat

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.util.Log
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import boofcv.abst.fiducial.QrCodeDetector
import boofcv.alg.color.ColorFormat
import boofcv.android.ConvertCameraImage
import boofcv.factory.fiducial.FactoryFiducial
import boofcv.struct.image.GrayU8
import chat.echo.app.TAG
import chat.echo.app.model.*
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.util.concurrent.*

// Adapted from learntodroid - https://gist.github.com/learntodroid/8f839be0b29d0378f843af70607bd7f5


@Composable
fun QRCodeScanner(onBarcode: (String) -> Unit) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  var preview by remember { mutableStateOf<Preview?>(null) }
  var lastAnalyzedTimeStamp = 0L
  var contactLink = ""


  val cameraProviderFuture by produceState<ListenableFuture<ProcessCameraProvider>?>(initialValue = null) {
    value = ProcessCameraProvider.getInstance(context)
  }

  DisposableEffect(lifecycleOwner) {
    onDispose {
      cameraProviderFuture?.get()?.unbindAll()
    }
  }

  AndroidView(
    factory = { AndroidViewContext ->
      PreviewView(AndroidViewContext).apply {
        this.scaleType = PreviewView.ScaleType.FILL_CENTER
        layoutParams = ViewGroup.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.MATCH_PARENT,
        )
        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
      }
    }
  ) { previewView ->
    val cameraSelector: CameraSelector = CameraSelector.Builder()
      .requireLensFacing(CameraSelector.LENS_FACING_BACK)
      .build()
    val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    cameraProviderFuture?.addListener({
      preview = Preview.Builder().build().also {
        it.setSurfaceProvider(previewView.surfaceProvider)
      }
      val detector: QrCodeDetector<GrayU8> = FactoryFiducial.qrcode(null, GrayU8::class.java)
      fun getQR(imageProxy: ImageProxy) {
        val currentTimeStamp = System.currentTimeMillis()
        if (currentTimeStamp - lastAnalyzedTimeStamp >= TimeUnit.SECONDS.toMillis(1)) {
          detector.process(imageProxyToGrayU8(imageProxy))
          val found = detector.detections
          val qr = found.firstOrNull()
          if (qr != null) {
           if (qr.message != contactLink) {
              contactLink = qr.message
              onBarcode(contactLink)
            }
          }
        }
      imageProxy.close()
      }
      val imageAnalyzer = ImageAnalysis.Analyzer { proxy -> getQR(proxy) }
      val imageAnalysis: ImageAnalysis = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .setImageQueueDepth(1)
        .build()
        .also { it.setAnalyzer(cameraExecutor, imageAnalyzer) }
      try {
        cameraProviderFuture?.get()?.unbindAll()
        cameraProviderFuture?.get()?.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis)
      } catch (e: Exception) {
        Log.d(TAG, "CameraPreview: ${e.localizedMessage}")
      }
    }, ContextCompat.getMainExecutor(context))
  }
}

internal inline fun <reified R : Any> String.convertToDataClass() =
  Json {
    ignoreUnknownKeys = true
  }.decodeFromString<R>(this)

@SuppressLint("UnsafeOptInUsageError")
private fun imageProxyToGrayU8(img: ImageProxy) : GrayU8? {
  val image = img.image
  if (image != null) {
    val outImg = GrayU8()
    ConvertCameraImage.imageToBoof(image, ColorFormat.GRAY, outImg, null)
    return outImg
  }
  return null
}