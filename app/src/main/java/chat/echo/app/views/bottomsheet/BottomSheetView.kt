package chat.echo.app.views.bottomsheet

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.echo.app.R
import chat.echo.app.ui.theme.HighOrLowlight
import chat.echo.app.ui.theme.TextHeader
import chat.echo.app.views.database.DatabaseView
import chat.echo.app.views.helpers.*
import chat.echo.app.views.newchat.CreateLinkTab
import chat.echo.app.views.newchat.CreateLinkView
import chat.echo.app.views.usersettings.*
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.accompanist.insets.navigationBarsWithImePadding
import kotlinx.coroutines.launch

@Composable
fun BottomSheetView(sheetContent: @Composable()
(ColumnScope.() -> Unit),   sheetState: ModalBottomSheetState, content: @Composable()
(ColumnScope.() -> Unit)){
  ProvideWindowInsets(windowInsetsAnimationsEnabled = true) {
    ModalBottomSheetLayout(
      scrimColor = Color.Black.copy(alpha = 0.12F),
      modifier = Modifier.navigationBarsWithImePadding(),
      sheetContent = {
        sheetContent
      },
      sheetState = sheetState,
      sheetShape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
      content = {
        content
      }
    )
  }
}