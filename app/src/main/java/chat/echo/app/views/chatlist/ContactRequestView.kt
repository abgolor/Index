package chat.echo.app.views.chatlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Comment
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.echo.app.R
import chat.echo.app.model.*
import chat.echo.app.ui.theme.*
import chat.echo.app.views.helpers.*

@Composable
fun ContactRequestView(chatModelIncognito: Boolean, contactRequest: ChatInfo.ContactRequest,
accept: () -> Unit, reject: () -> Unit) {

  @Composable
  fun contactNameText(color: Color = Color.Unspecified) {

    Text(
      if(contactRequest.fullName != "") contactRequest.fullName else contactRequest.localDisplayName,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      style = TextStyle(fontSize = 16.sp),
      fontWeight = FontWeight.SemiBold,
      color = color
    )
  }

  @Composable
  fun chatPreviewTitle() {
    contactNameText(Color.Black)
  }

  Row(Modifier.background(Color.White)) {
    Box(contentAlignment = Alignment.BottomEnd) {
      ContactInfoImage(contactRequest, size = 48.dp, Color.Black)
    }
    Column(
      modifier = Modifier
        .padding(horizontal = 8.dp)
        .weight(1F)
        .align(Alignment.CenterVertically)
    ) {
      chatPreviewTitle()
    }
    Box(
      contentAlignment = Alignment.TopEnd
    ) {
      Row() {
        IconButton(onClick = accept) {
          Icon(
            Icons.Filled.CheckCircle,
            contentDescription = generalGetString(R.string.accept_contact_button),
            tint = Color.Black,
            modifier = Modifier
              .padding(horizontal = 3.dp)
              .padding(vertical = 1.dp)
              .size(26.dp)
          )
        }
        Spacer(modifier = Modifier.padding(end = 5.dp))
        IconButton(onClick = reject) {
          Icon(
            Icons.Filled.Cancel,
            contentDescription = generalGetString(R.string.reject_contact_button),
            tint = Color.Black,
            modifier = Modifier
              .padding(horizontal = 3.dp)
              .padding(vertical = 1.dp)
              .size(26.dp)
          )
        }
      }
    }
  }
}

@Composable
fun ContactRequestView(chatModelIncognito: Boolean, contactRequest: ChatInfo.ContactRequest) {
  Row {
    ChatInfoImage(contactRequest, size = 72.dp)
    Column(
      modifier = Modifier
        .padding(horizontal = 8.dp)
        .weight(1F)
    ) {
      Text(
        contactRequest.chatViewName,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.h3,
        fontWeight = FontWeight.Bold,
        color = if (chatModelIncognito) Indigo else MaterialTheme.colors.primary
      )
      Text(stringResource(R.string.contact_wants_to_connect_with_you), maxLines = 2, color = if (isInDarkTheme()) MessagePreviewDark else MessagePreviewLight)
    }
    val ts = getTimestampText(contactRequest.contactRequest.updatedAt)
    Column(
      Modifier.fillMaxHeight(),
      verticalArrangement = Arrangement.Top
    ) {
      Text(
        ts,
        color = HighOrLowlight,
        style = MaterialTheme.typography.body2,
        modifier = Modifier.padding(bottom = 5.dp)
      )
    }
  }
}
