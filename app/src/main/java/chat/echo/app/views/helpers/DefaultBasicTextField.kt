package chat.echo.app.views.helpers

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.foundation.text.*
import androidx.compose.material.*
import androidx.compose.material.TextFieldDefaults.indicatorLine
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DefaultBasicTextField(
  modifier: Modifier,
  initialValue: String,
  placeholder: (@Composable () -> Unit)? = null,
  leadingIcon: (@Composable () -> Unit)? = null,
  focus: Boolean = false,
  color: Color = MaterialTheme.colors.onBackground,
  textStyle: TextStyle = TextStyle.Default,
  selectTextOnFocus: Boolean = false,
  keyboardOptions: KeyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
  keyboardActions: KeyboardActions = KeyboardActions(),
  onValueChange: (String) -> Unit,
) {
  val state = remember {
    mutableStateOf(TextFieldValue(initialValue))
  }
  val focusRequester = remember { FocusRequester() }
  val keyboard = LocalSoftwareKeyboardController.current

  LaunchedEffect(Unit) {
    if (!focus) return@LaunchedEffect
    delay(300)
    focusRequester.requestFocus()
    keyboard?.show()
  }
  val enabled = true
  val colors = TextFieldDefaults.textFieldColors(
    backgroundColor = Color.Unspecified,
    textColor = MaterialTheme.colors.onBackground,
    focusedIndicatorColor = Color.Unspecified,
    unfocusedIndicatorColor = Color.Unspecified,
  )
  val shape = MaterialTheme.shapes.small.copy(bottomEnd = ZeroCornerSize, bottomStart = ZeroCornerSize)
  val interactionSource = remember { MutableInteractionSource() }
  BasicTextField(
    value = state.value,
    modifier = modifier
      .background(colors.backgroundColor(enabled).value, shape)
      .indicatorLine(enabled, false, interactionSource, colors)
      .focusRequester(focusRequester)
      .onFocusChanged { focusState ->
        if (focusState.isFocused && selectTextOnFocus) {
          val text = state.value.text
          state.value = state.value.copy(
            selection = TextRange(0, text.length)
          )
        }
      }
      .defaultMinSize(
        minWidth = TextFieldDefaults.MinWidth,
        minHeight = TextFieldDefaults.MinHeight
      ),
    onValueChange = {
      state.value = it
      onValueChange(it.text)
    },
    cursorBrush = SolidColor(colors.cursorColor(false).value),
    visualTransformation = VisualTransformation.None,
    keyboardOptions = keyboardOptions,
    keyboardActions = KeyboardActions(onDone = {
      keyboard?.hide()
      keyboardActions.onDone?.invoke(this)
    }),
    singleLine = true,
    textStyle = textStyle.copy(
      color = color,
      fontWeight = FontWeight.Normal,
      fontSize = 16.sp
    ),
    interactionSource = interactionSource,
    decorationBox = @Composable { innerTextField ->
      TextFieldDefaults.TextFieldDecorationBox(
        value = state.value.text,
        innerTextField = innerTextField,
        placeholder = placeholder,
        singleLine = true,
        enabled = enabled,
        leadingIcon = leadingIcon,
        interactionSource = interactionSource,
        contentPadding = TextFieldDefaults.textFieldWithLabelPadding(start = 0.dp, end = 0.dp),
        visualTransformation = VisualTransformation.None,
        colors = colors
      )
    }
  )
}
