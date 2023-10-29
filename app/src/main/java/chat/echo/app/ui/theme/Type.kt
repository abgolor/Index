package chat.echo.app.ui.theme

import androidx.compose.material.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.sp
import chat.echo.app.R

// https://github.com/rsms/inter
/*val Inter = FontFamily(
  Font(R.font.inter_regular),
  Font(R.font.inter_italic, style = FontStyle.Italic),
  Font(R.font.inter_bold, weight = FontWeight.Bold),
  Font(R.font.inter_semi_bold, weight = FontWeight.SemiBold),
  Font(R.font.inter_medium, weight = FontWeight.Medium),
  Font(R.font.inter_light, weight = FontWeight.Light),
)*/

val Inter = FontFamily(
Font(R.font.poppins_regular),
Font(R.font.poppins_italic, style = FontStyle.Italic),
Font(R.font.poppins_bold, weight = FontWeight.Bold),
Font(R.font.poppins_semibold, weight = FontWeight.SemiBold),
Font(R.font.poppins_medium, weight = FontWeight.Medium),
Font(R.font.poppins_light, weight = FontWeight.Light),
)

// Set of Material typography styles to start with
val Typography = Typography(
  h1 = TextStyle(
    fontFamily = Inter,
    fontWeight = FontWeight.Bold,
    fontSize = 32.sp,
  ),
  h2 = TextStyle(
    fontFamily = Inter,
    fontWeight = FontWeight.Normal,
    fontSize = 24.sp
  ),
  h3 = TextStyle(
    fontFamily = Inter,
    fontWeight = FontWeight.Normal,
    fontSize = 18.5.sp
  ),
  body1 = TextStyle(
    fontFamily = Inter,
    fontWeight = FontWeight.Normal,
    fontSize = 16.sp
  ),
  body2 = TextStyle(
    fontFamily = Inter,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp
  ),
  button = TextStyle(
    fontFamily = Inter,
    fontWeight = FontWeight.Normal,
    fontSize = 16.sp,
  ),
  caption = TextStyle(
    fontFamily = Inter,
    fontWeight = FontWeight.Normal,
    fontSize = 18.sp
  )
)
