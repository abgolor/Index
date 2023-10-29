package chat.echo.app.views.helpers.alerts

import android.graphics.fonts.Font
import android.graphics.fonts.FontFamily
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import chat.echo.app.R
import chat.echo.app.views.helpers.generalGetString
import com.airbnb.lottie.compose.*

@Composable
fun LoadingDialogLayout(modifier: Modifier = Modifier){
  Card(
    shape = RoundedCornerShape(10.dp),
    modifier = Modifier
      .padding(10.dp,5.dp,10.dp,10.dp),
    elevation = 8.dp
  ) {
    Column(
      modifier
        .background(Color.White)) {
      Column(modifier = Modifier.padding(10.dp),
        Arrangement.spacedBy(15.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Spacer(modifier = Modifier.padding(top = 10.dp))
        Loader()
        Spacer(modifier = Modifier.padding(top =20.dp))
        Text(
          text = generalGetString(R.string.please_wait),
          textAlign = TextAlign.Center,
          modifier = Modifier
            .padding(top = 5.dp, bottom = 5.dp)
            .fillMaxWidth(),
          fontWeight = FontWeight.Normal,
          fontSize = 14.sp,
          color = Color.Black
        )
      }
    }
  }
}

@Composable
fun Loader() {
  val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.anim_loading))

  LottieAnimation(
    composition,
    Modifier
      .requiredWidth(30.dp)
      .requiredHeight(30.dp)
      .scale(2f, 2f),
    iterations = LottieConstants.IterateForever,
    clipSpec = LottieClipSpec.Progress(0.5f, 0.75f),
    isPlaying = true
  )
}

