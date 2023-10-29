package chat.echo.app.views.chatlist

import SectionDivider
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import chat.echo.app.R
import chat.echo.app.model.*
import chat.echo.app.ui.theme.*
import chat.echo.app.views.bottomsheet.HomepageSheetLayout
import chat.echo.app.views.chat.*
import chat.echo.app.views.helpers.*
import chat.echo.app.views.navbar.*
import chat.echo.app.views.usersettings.SettingsNewView
import com.google.accompanist.insets.navigationBarsWithImePadding
import de.charlex.compose.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

enum class HomepageBottomSheetType() {
  AddContactBottomSheet,
  SortChartBottomSheet
}

enum class SortChat() {
  SortByTime,
  SortByUnread
}

@Composable
fun ChatListView(chatModel: ChatModel, setPerformLA: (Boolean) -> Unit, stopped: Boolean) {
  val newChatSheetState by rememberSaveable(stateSaver = NewChatSheetState.saver()) { mutableStateOf(MutableStateFlow(NewChatSheetState.GONE)) }
  val navController = rememberNavController()
  val defaultIconButton: @Composable() (RowScope.() -> Unit) = {
    IconButton(onClick = {
      navController.navigate(BottomBarView.Contacts.route) {
        popUpTo(navController.graph.findStartDestination().id)
        launchSingleTop = true
      }
    }) {
      Icon(
        Icons.Filled.AddComment,
        generalGetString(R.string.tap_to_start_new_chat),
        tint = Color.White,
      )
    }
  }
  val showNewChatSheet = {
    newChatSheetState.value = NewChatSheetState.VISIBLE
  }
  val hideNewChatSheet: (animated: Boolean) -> Unit = { animated ->
    if (animated) newChatSheetState.value = NewChatSheetState.HIDING
    else newChatSheetState.value = NewChatSheetState.GONE
  }
  LaunchedEffect(chatModel.clearOverlays.value) {
    if (chatModel.clearOverlays.value && newChatSheetState.value.isVisible()) hideNewChatSheet(false)
  }
  var searchInList by rememberSaveable { mutableStateOf("") }
  val scaffoldState = rememberScaffoldState()
  val bottomSheetModalState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
  val coroutineScope = rememberCoroutineScope()
  var currentBottomSheet: HomepageBottomSheetType? by remember {
    mutableStateOf(null)
  }
  val openSheet: (HomepageBottomSheetType) -> Unit = {
    currentBottomSheet = it
    coroutineScope.launch {
      delay(100)
      bottomSheetModalState.show()
    }
  }
  val closeSheet: () -> Unit = {
    coroutineScope.launch {
      bottomSheetModalState.hide()
    }
  }
  val openAddContactBottomSheet: () -> Unit = {
    openSheet(HomepageBottomSheetType.AddContactBottomSheet)
  }
  val openSortChatBottomSheet: () -> Unit = {
    openSheet(HomepageBottomSheetType.SortChartBottomSheet)
  }

  if (!bottomSheetModalState.isVisible) {
    currentBottomSheet = null
  }


  ModalBottomSheetLayout(
    scrimColor = Color.Black.copy(alpha = 0.12F),
    modifier = Modifier.navigationBarsWithImePadding(),
    sheetContent = {
      if (currentBottomSheet != null) {
        HomepageSheetLayout(chatModel = chatModel, bottomSheetType = currentBottomSheet!!, sortChatMode = chatModel.sortChatMode, closeSheet = closeSheet)
      } else {
        Box(Modifier.size(1.dp))
      }
    },
    sheetState = bottomSheetModalState,
    sheetShape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
    content = {
      Scaffold(
        topBar = { ChatListToolbar(chatModel, setPerformLA, scaffoldState.drawerState, currentBottomSheet, stopped = stopped, navController = navController, openAddNewContactBottomSheet = openAddContactBottomSheet, openSortChatBottomSheet = openSortChatBottomSheet) { searchInList = it.trim() } },
        scaffoldState = scaffoldState,
        drawerGesturesEnabled = false,
        drawerContent = { },
        bottomBar = { BottomBar(navController = navController, chatModel = chatModel) }
      ) {
        Box(
          Modifier.padding(it)
        ) {
          Column(
            modifier = Modifier
              .fillMaxSize()
            //.background(MaterialTheme.colors.background)
          ) {
            BottomNavGraph(navController = navController, chatModel = chatModel, sortChatMode = chatModel.sortChatMode)
          }
        }
      }
    })
}

@Composable
private fun OnboardingButtons(openNewChatSheet: () -> Unit) {
  Column(Modifier.fillMaxSize().padding(DEFAULT_PADDING), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.Bottom) {
    ConnectButton(generalGetString(R.string.tap_to_start_new_chat), openNewChatSheet)
    val color = MaterialTheme.colors.primary
    Canvas(modifier = Modifier.width(40.dp).height(10.dp), onDraw = {
      val trianglePath = Path().apply {
        moveTo(0.dp.toPx(), 0f)
        lineTo(16.dp.toPx(), 0.dp.toPx())
        lineTo(8.dp.toPx(), 10.dp.toPx())
        lineTo(0.dp.toPx(), 0.dp.toPx())
      }
      drawPath(
        color = color,
        path = trianglePath
      )
    })
    Spacer(Modifier.height(62.dp))
  }
}

@Composable
private fun ConnectButton(text: String, onClick: () -> Unit) {
  Button(
    onClick,
    shape = RoundedCornerShape(21.dp),
    colors = ButtonDefaults.textButtonColors(
      backgroundColor = MaterialTheme.colors.primary
    ),
    elevation = null,
    contentPadding = PaddingValues(horizontal = DEFAULT_PADDING, vertical = DEFAULT_PADDING_HALF),
    modifier = Modifier.height(42.dp)
  ) {
    Text(text, color = Color.White)
  }
}

@Composable
private fun ChatListToolbar(
  chatModel: ChatModel, setPerformLA: (Boolean) -> Unit, drawerState: DrawerState, currentBottomSheet: HomepageBottomSheetType?, stopped: Boolean, navController: NavHostController, openAddNewContactBottomSheet: () -> Unit, openSortChatBottomSheet: () -> Unit, onSearchValueChanged: (String) -> Unit,
) {
  var showSearch by rememberSaveable { mutableStateOf(false) }
  val coroutineScope = rememberCoroutineScope()
  val profileOf = chatModel.currentUser.value?.profile
  val hideSearchOnBack = { onSearchValueChanged(""); showSearch = false }
  if (showSearch) {
    BackHandler(onBack = hideSearchOnBack)
  }
  val barButtons = arrayListOf<@Composable RowScope.() -> Unit>()
  if (chatModel.currentPageRoute.value == BottomBarView.Contacts.route) {
    barButtons.clear()
    barButtons.add {
      IconButton({
        openAddNewContactBottomSheet()
      }) {
        Icon(
          painter = painterResource(id = R.drawable.ic_new_contact),
          stringResource(R.string.tap_to_start_new_chat),
          tint = MaterialTheme.colors.onSurface,
          modifier = Modifier.size(24.dp)
        )
      }
    }
  } else if (chatModel.currentPageRoute.value == BottomBarView.Chats.route) {
    barButtons.clear()
    barButtons.add {
      IconButton({
        navController.navigate(BottomBarView.Contacts.route) {
          popUpTo(navController.graph.findStartDestination().id)
          launchSingleTop = true
        }
      }) {
        Icon(
          painter = painterResource(R.drawable.ic_new_chat),
          stringResource(R.string.tap_to_start_new_chat),
          tint = MaterialTheme.colors.onSurface,
          modifier = Modifier.size(24.dp)
        )
      }
    }
  }
  DefaultTopAppBar(
    navigationButton = {
      BoxWithConstraints(Modifier.clickable {
        ModalManager.shared.showCustomModal { close ->
          SettingsNewView(
            chatModel,
            showModal = { modalView -> { ModalManager.shared.showModal { modalView(chatModel) } } },
            showSettingsModal = { modalView -> { ModalManager.shared.showModal(true) { modalView(chatModel) } } },
            showCustomModal = { modalView -> { ModalManager.shared.showCustomModal { close -> modalView(chatModel, close) } } },
            setPerformLA,
            close
          )
        }
      }) {
        ProfileImage(size = 50.dp, image = profileOf?.image, color = MaterialTheme.colors.onSurface)
        ServerIcon(Modifier.align(Alignment.BottomEnd))
      }
    },
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
      ) {
        Column() {
          Row(Modifier.clickable {
            openSortChatBottomSheet()
          }) {
            Text(
              chatModel.currentPageTitle.value,
              color = MaterialTheme.colors.onBackground,
              fontWeight = FontWeight.SemiBold,
            )
            Icon(
              Icons.Filled.ArrowDropDown,
              stringResource(R.string.expanded),
              tint = Color.White,
              modifier = Modifier.size(24.dp)
            )
          }
        }
        if (chatModel.incognito.value) {
          Icon(
            Icons.Filled.TheaterComedy,
            stringResource(R.string.incognito),
            tint = Indigo,
            modifier = Modifier.padding(start = 10.dp).size(26.dp)
          )
        }
      }
    },
    onTitleClick = null,
    showSearch = showSearch,
    onSearchValueChanged = onSearchValueChanged,
    buttons = barButtons,
    //backgroundColor = ToolbarDark
  )
  Divider(Modifier.padding(top = AppBarHeight))
}

@Composable
fun ServerIcon(/*networkStatus: Chat.NetworkStatus*/modifier: Modifier) {
  Box(modifier.size(18.dp)) {
    Icon(Icons.Filled.Circle, stringResource(R.string.icon_descr_server_status_connected), tint = MaterialTheme.colors.primaryVariant)
    /*   when (networkStatus) {
         is Chat.NetworkStatus.Connected ->
           Icon(Icons.Filled.Circle, stringResource(R.string.icon_descr_server_status_connected), tint = MaterialTheme.colors.primaryVariant)
         is Chat.NetworkStatus.Disconnected ->
           Icon(Icons.Filled.Pending, stringResource(R.string.icon_descr_server_status_disconnected), tint = HighOrLowlight)
         is Chat.NetworkStatus.Error ->
           Icon(Icons.Filled.Error, stringResource(R.string.icon_descr_server_status_error), tint = HighOrLowlight)
         else -> Icon(Icons.Outlined.Circle, stringResource(R.string.icon_descr_server_status_pending), tint = HighOrLowlight)
       }*/
  }
}

@Composable fun ChatList(chatModel: ChatModel, sortChat: MutableState<SortChat>, search: String) {
  val filter: (Chat) -> Boolean = { chat: Chat ->
    chat.chatInfo.displayName.lowercase().contains(search.lowercase()) ||
        chat.chatInfo.fullName.lowercase().contains(search.lowercase()) || searchTags(chat.chatInfo.localAlias, search)
  }

  val chatList = chatModel.chats
    .filter { chat ->
      (if (chat.chatInfo is ChatInfo.Direct) {
        chat.chatItems.isNotEmpty()
      } else chat.chatInfo is ChatInfo.Group)
    }

  val sortChatByUnread: (Chat) -> Boolean = { chat: Chat ->
    Log.i("TAG", "ChatList: chat unread count is " + chat.chatStats.unreadCount)
    chat.chatStats.unreadCount > 0
  }
  val chats by remember(search) {
    derivedStateOf {
      if (search.isEmpty())
        if (sortChat.value == SortChat.SortByUnread)
           chatModel.chats.sortedByDescending {
             it.chatStats.unreadCount
           }
        else
          chatList
      else
        chatModel.chats.filter(filter)
    }
  }
  LazyColumn(
    modifier = Modifier.fillMaxWidth()
  ) {
    itemsIndexed(chats) { index, chat ->
      val state = rememberRevealState()
      val coroutineScope = rememberCoroutineScope()
      RevealSwipe(
        modifier = Modifier.padding(vertical = 5.dp, horizontal = 10.dp),
        closeOnContentClick = true,
        contentClickHandledExtern = true,
        state = state,
        maxRevealDp = if (state.currentValue == RevealValue.FullyRevealedStart) 125.dp else 75.dp,
        directions = setOf(
          RevealDirection.StartToEnd,
          RevealDirection.EndToStart
        ),
        hiddenContentStart = {
          IconButton(
            modifier = Modifier.background(LightBlue).fillMaxHeight(1f),
            onClick = {
              coroutineScope.launch {
                changeNtfsStatePerChat(!chat.chatInfo.ntfsEnabled, mutableStateOf(chat.chatInfo.ntfsEnabled), chat, chatModel)
                state.resetFast()
              }
            }) {
            Icon(
              modifier = Modifier.padding(horizontal = 25.dp),
              imageVector = if (chat.chatInfo.ntfsEnabled) Icons.Outlined.NotificationsOff else Icons.Outlined.Notifications,
              tint = Color.White,
              contentDescription = if (chat.chatInfo.ntfsEnabled) generalGetString(R.string.mute_chat) else generalGetString(R.string.unmute_chat)
            )
          }
        },
        hiddenContentEnd = {
          Row(Modifier.fillMaxHeight(1f)) {
            IconButton(
              modifier = Modifier.background(WarningOrange).fillMaxHeight(1f),
              onClick = {
                coroutineScope.launch {
                  clearChatDialog(chat.chatInfo, chatModel)
                  state.resetFast()
                }
              }) {
              Icon(
                modifier = Modifier.padding(horizontal = 20.dp),
                imageVector = Icons.Outlined.Restore,
                tint = Color.White,
                contentDescription = if (chat.chatInfo.ntfsEnabled) generalGetString(R.string.mute_chat) else generalGetString(R.string.unmute_chat)
              )
            }
            IconButton(
              modifier = Modifier.background(DeleteRed).fillMaxHeight(1f),
              onClick = {
                coroutineScope.launch {
                  deleteContactDialog(chat.chatInfo, chatModel)
                  state.resetFast()
                }
              }) {
              Icon(
                modifier = Modifier.padding(horizontal = 20.dp),
                imageVector = Icons.Outlined.Delete,
                tint = Color.White,
                contentDescription = generalGetString(R.string.delete_chat)
              )
            }
          }
        }
      ) {
        ChatListNavLinkView(chat, chatModel)
      }
      if (index < chats.lastIndex) {
        SectionDivider()
      }
    }
  }
}

fun searchTags(localAlias: String, search: String): Boolean {
  if (localAlias != "") {
    val contactBioInfo = json.decodeFromString(ContactBioInfoSerializer, localAlias)
    return contactBioInfo.tag.lowercase().contains(search.lowercase())
  }
  return false
}

@Composable fun ContactList(chatModel: ChatModel, search: String) {
  val filter: (Chat) -> Boolean = { chat: Chat ->
    chat.chatInfo.localDisplayName.lowercase().contains(search.lowercase()) ||
        chat.chatInfo.fullName.lowercase().contains(search.lowercase()) || searchTags(chat.chatInfo.localAlias, search)
  }
  val chats by remember(search) {
    derivedStateOf {
      if (search.isEmpty()) chatModel.chats
        .filter { chat ->
          (chat.chatInfo is ChatInfo.Direct || chat.chatInfo is ChatInfo.ContactRequest)
        }
      else chatModel.chats.filter(filter)
        .filter { chat ->
          chat.chatInfo is ChatInfo.Direct || chat.chatInfo is ChatInfo.ContactRequest
        }
    }
  }
  val alphabets = listOf("A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "#")

  LazyColumn(
    modifier = Modifier.fillMaxWidth()
  ) {
    items(alphabets) { alphabet ->
      var isAlphabetAvailable = false
      val alphabetFilter: (Chat) -> Boolean = { chat: Chat ->
        isAlphabetAvailable = if (chat.chatInfo.fullName != "")
          chat.chatInfo.fullName.first().toString().lowercase() == alphabet.lowercase() else
          chat.chatInfo.localDisplayName.first().toString().lowercase() == alphabet.lowercase()
        isAlphabetAvailable
      }
      val filteredChats = chats.filter(alphabetFilter)
      if (filteredChats.isNotEmpty()) {
        Spacer(modifier = Modifier.padding(top = 5.dp))
        SubHeaderContacts(title = alphabet)
        Spacer(modifier = Modifier.padding(bottom = 2.dp))
        filteredChats.forEachIndexed { index, chat ->
          val coroutineScope = rememberCoroutineScope()
          val state = rememberRevealState()
          if (chat.chatInfo is ChatInfo.Direct) {
            RevealSwipe(
              modifier = Modifier.padding(top = 2.dp, bottom = 2.dp, start = 5.dp, end = 5.dp),
              directions = setOf(
                RevealDirection.EndToStart
              ),
              state = state,
              hiddenContentEnd = {
                IconButton(
                  modifier = Modifier.background(DeleteRed).fillMaxHeight(1f),
                  onClick = {
                    coroutineScope.launch {
                      deleteContactDialog(chat.chatInfo, chatModel)
                      state.resetFast()
                    }
                  }) {
                  Icon(
                    modifier = Modifier.padding(horizontal = 25.dp),
                    imageVector = Icons.Outlined.Delete,
                    tint = Color.White,
                    contentDescription = generalGetString(R.string.delete_chat)
                  )
                }
              }
            ) {
              ContactListNavLinkView(chat = chat, chatModel = chatModel)
            }
          } else {
            ContactListNavLinkView(chat = chat, chatModel = chatModel)
          }
          if (index < filteredChats.lastIndex) {
            SectionDivider(Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
          }
        }
      }
    }
  }
}
