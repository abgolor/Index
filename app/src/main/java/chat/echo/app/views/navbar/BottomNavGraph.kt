package chat.echo.app.views.navbar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import chat.echo.app.R
import chat.echo.app.model.*
import chat.echo.app.ui.theme.*
import chat.echo.app.views.chatlist.*
import chat.echo.app.views.database.DatabaseView
import chat.echo.app.views.helpers.*
import chat.echo.app.views.newchat.CreateUserLinkView

@Composable
fun BottomNavGraph(
  chatModel: ChatModel,
  navController: NavHostController,
  sortChatMode: MutableState<SortChat>
) {
  NavHost(navController = navController, startDestination = chatModel.currentPageRoute.value) {
    composable(route = BottomBarView.Contacts.route) {
      val showSettingsModal: (@Composable (ChatModel) -> Unit) -> () -> Unit = { modalView -> { ModalManager.shared.showModal(true) { modalView(chatModel) } } }
      chatModel.currentPageRoute.value = BottomBarView.Contacts.route
      chatModel.currentPageTitle.value = BottomBarView.Contacts.title
      Column(
        modifier = Modifier
          .fillMaxSize()
          .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
          .background(Color.White)
      ) {
        Column {
          Text(
            text = generalGetString(R.string.app_name),
            modifier = Modifier
              .fillMaxWidth()
              .padding(10.dp),
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Black,
            textAlign = TextAlign.Center
          )
          Divider(color = DividerColor)
          Row(
            Modifier.fillMaxWidth()
              .padding(top = 10.dp, start = 10.dp, end = 10.dp)
          ) {
            Text(
              text = generalGetString(R.string.contacts) + ": " + chatModel.chats.filter { chat ->
                chat.chatInfo is ChatInfo.Direct || chat.chatInfo is ChatInfo.ContactRequest
              }.size,
              modifier = Modifier
                .align(Alignment.CenterVertically),
              fontSize = 12.sp,
              fontWeight = FontWeight.Normal,
              color = Color.Black
            )
            Spacer(Modifier.fillMaxWidth().weight(1f))
            Text(
              text = generalGetString(R.string.import_export_contacts),
              modifier = Modifier
                .align(Alignment.CenterVertically)
                .clickable(onClick = showSettingsModal { DatabaseView(it, showSettingsModal) }),
              fontSize = 11.sp,
              fontWeight = FontWeight.Medium,
              color = buttonEnabled
            )
          }
        }
        if (chatModel.chats.isNotEmpty()) {
          ContactList(chatModel, "")
        } else {
          Box(
            Modifier.fillMaxSize()
              .padding(bottom = 100.dp)
          ) {
            Column(Modifier.align(Alignment.Center)) {
              Icon(
                painter = painterResource(id = R.drawable.ic_contact_book),
                contentDescription = "icon",
                tint = Color.Black,
                modifier = Modifier
                  .padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 8.dp)
                  .size(100.dp)
                  .align(Alignment.CenterHorizontally)
              )
              Spacer(modifier = Modifier.size(10.dp))
              Button(
                onClick = {
                  ModalManager.shared.showCustomModal { close ->
                    CreateUserLinkView(model = chatModel, initialSelection = 1, pasteInstead = false, close = close)
                  }
                },
                modifier = Modifier
                  .height(55.64.dp)
                  .padding(8.dp),
                colors = ButtonDefaults.buttonColors(
                  backgroundColor = MaterialTheme.colors.background
                ),
                shape = RoundedCornerShape(5.dp)
              ) {
                Text(
                  text = generalGetString(R.string.invite_contacts_to_index),
                  textAlign = TextAlign.Center,
                  fontSize = 14.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = Color.White,
                  modifier = Modifier.padding(start = 15.dp, end = 15.dp)
                )
              }
            }
            /*if (!stopped && !newChatSheetState.collectAsState().value.isVisible()) {
             OnboardingButtons(showNewChatSheet)
           }*/
            //Text(stringResource(R.string.you_have_no_contacts), Modifier.align(Alignment.Center), color = HighOrLowlight)
          }
        }
      }
    }
    composable(route = BottomBarView.Chats.route) {
      chatModel.currentPageRoute.value = BottomBarView.Chats.route
      chatModel.currentPageTitle.value = BottomBarView.Chats.title

      val filter: (Chat) -> Boolean = { chat: Chat ->
        (chat.chatInfo is ChatInfo.Direct || chat.chatInfo is ChatInfo.Group) && chat.chatItems.isNotEmpty()
      }

      val chatList = chatModel.chats.filter(filter)

      Column(
        modifier = Modifier
          .fillMaxSize()
          .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
          .background(Color.White)
      ) {
        Column {
          Text(
            text = generalGetString(R.string.all),
            modifier = Modifier
              .fillMaxWidth()
              .padding(10.dp),
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Black,
            textAlign = TextAlign.Center
          )
          Divider(color = DividerColor)
          Row(
            Modifier.fillMaxWidth()
              .padding(top = 10.dp, start = 10.dp, end = 10.dp)
          ) {
            Text(
              text = generalGetString(R.string.chats) + ": " + chatList.size,
              modifier = Modifier
                .align(Alignment.CenterVertically),
              fontSize = 12.sp,
              fontWeight = FontWeight.Normal,
              color = Color.Black
            )
          }
        }
        if (chatList.isNotEmpty()) {
          ChatList(chatModel, sortChatMode, "")
        } else {
          Box(
            Modifier.fillMaxSize()
              .padding(bottom = 100.dp)
          ) {
            Column(Modifier.align(Alignment.Center)) {
              Icon(
                painter = painterResource(id = R.drawable.ic_chats),
                contentDescription = "icon",
                tint = Color.Black,
                modifier = Modifier
                  .padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 8.dp)
                  .size(100.dp)
                  .align(Alignment.CenterHorizontally)
              )
              Spacer(modifier = Modifier.size(10.dp))
              Text(
                text = generalGetString(R.string.you_dont_have_any_chats_yet),
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = Color.Black
              )
              Spacer(modifier = Modifier.size(5.dp))
              Text(
                text = generalGetString(R.string.start_a_chats_with_your_contacts),
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = Color.Black,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable {
                  navController.navigate(BottomBarView.Contacts.route) {
                    popUpTo(navController.graph.findStartDestination().id)
                    launchSingleTop = true
                  }
                }
              )
            }
            /*if (!stopped && !newChatSheetState.collectAsState().value.isVisible()) {
             OnboardingButtons(showNewChatSheet)
           }*/
            //Text(stringResource(R.string.you_have_no_contacts), Modifier.align(Alignment.Center), color = HighOrLowlight)
          }
        }
      }
    }
  }
}


fun isChatsAvailable(chatModel: ChatModel): Boolean {
  val filter: (Chat) -> Boolean = { chat: Chat ->
    chat.chatItems.isNotEmpty()
  }
  val chat = chatModel.chats.filter(filter)
  return chat.isNotEmpty()
}

fun getChatsSize(chatModel: ChatModel): Int {
  val filter: (Chat) -> Boolean = { chat: Chat ->
    chat.chatItems.isNotEmpty()
  }
  val chat = chatModel.chats.filter(filter)
  return chat.size
}

