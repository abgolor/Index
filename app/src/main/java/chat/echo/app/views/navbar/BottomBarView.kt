package chat.echo.app.views.navbar

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.graphics.Color
import chat.echo.app.R
import chat.echo.app.views.helpers.generalGetString

sealed class BottomBarView(
  val route: String,
  val title: String,
  val icon: Int,
  val selectedOption: String
) {
  //for contacts
  object Contacts: BottomBarView(
    route = "contacts",
    title = "Contacts",
    icon = R.drawable.ic_contact_inactive,
    selectedOption = generalGetString(R.string.contacts))

  //for chats
  object Chats: BottomBarView(
    route = "chats",
    title = "Chats",
    icon = R.drawable.ic_chat_inactive,
    selectedOption = generalGetString(R.string.chats))

  //for search
  //for chats
  object Search: BottomBarView(
    route = "search",
    title = "Search",
    icon = R.drawable.ic_search,
    selectedOption = generalGetString(R.string.search))
}

