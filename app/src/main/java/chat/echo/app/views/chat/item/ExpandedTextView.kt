package chat.echo.app.views.chat.item

import android.graphics.fonts.FontStyle
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import java.lang.Exception

const val DEFAULT_MINIMUM_TEXT_LINE = 7

@Composable
fun ExpandableText(
  modifier: Modifier = Modifier,
  textModifier: Modifier = Modifier,
  style: TextStyle = LocalTextStyle.current,
  fontStyle: FontStyle? = null,
  text: String,
  collapsedMaxLine: Int = DEFAULT_MINIMUM_TEXT_LINE,
  showMoreText: String = "... Show More",
  showMoreStyle: SpanStyle = SpanStyle(fontWeight = FontWeight.W500),
  showLessText: String = "\n Show Less",
  showLessStyle: SpanStyle = showMoreStyle,
  isDecrypted: MutableState<Boolean>,
  textAlign: TextAlign? = null,
  showMenu: MutableState<Boolean>
) {
  var isExpanded by remember { mutableStateOf(false) }
  var clickable by remember { mutableStateOf(false) }
  var lastCharIndex by remember { mutableStateOf(0) }
  Box(modifier = Modifier
    .combinedClickable(
      onDoubleClick = {
        isExpanded = !isExpanded
      },
      onClick = {
        isDecrypted.value = true
      },
      onLongClick = {
        showMenu.value = true
      }
    )
    .then(modifier)
  ) {
    Text(
      modifier = textModifier
        .fillMaxWidth()
        .animateContentSize(),
      text = buildAnnotatedString {
        if (clickable) {
          if (isExpanded) {
            append(text)
            withStyle(style = showLessStyle) { append(showLessText) }
          } else {
            try {
              val adjustText = text.substring(startIndex = 0, endIndex = lastCharIndex)
                .dropLast(showMoreText.length)
                .dropLastWhile { Character.isWhitespace(it) || it == '.' }
              append(adjustText)
              withStyle(style = showMoreStyle) { append(showMoreText) }
            } catch (e: Exception){
              e.printStackTrace()
            }
          }
        } else {
          append(text)
        }
      },
      maxLines = if (isExpanded) Int.MAX_VALUE else collapsedMaxLine,
      //fontStyle = fontStyle,
      onTextLayout = { textLayoutResult ->
        if (!isExpanded && textLayoutResult.hasVisualOverflow) {
          clickable = true
          lastCharIndex = textLayoutResult.getLineEnd(collapsedMaxLine - 1)
        }
      },
      style = style,
      textAlign = textAlign
    )
  }
}