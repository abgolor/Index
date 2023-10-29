package chat.echo.app.views.newchat

import SectionDivider
import SectionItemView
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.ContentAlpha.disabled
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.echo.app.R
import chat.echo.app.contacts.AddContactsItemView
import chat.echo.app.model.*
import chat.echo.app.ui.theme.*
import chat.echo.app.views.helpers.*
import chat.echo.app.views.usersettings.UserAddressView
import com.google.accompanist.pager.*
import kotlinx.coroutines.launch
import java.util.*

enum class ConnectByIndexCodeTab {
  SCAN_TO_CONNECT,
  MY_INDEX_CODE,
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun CreateUserLinkView(model: ChatModel, initialSelection: Int, pasteInstead: Boolean, close: () -> Unit) {
  val scaffoldState = rememberScaffoldState()
  val selection = remember { mutableStateOf(initialSelection) }
  val isPasteInstead = remember{ mutableStateOf(pasteInstead)}
  val connReqInvitation = remember { mutableStateOf("") }
  val index = remember{ mutableStateOf(initialSelection)}

  val tabTitles = listOf(if(!isPasteInstead.value) stringResource(id = R.string.scan_index_code) else generalGetString(R.string.paste_index_code),
    stringResource(id = R.string.my_index_code)
  )

  val pagerState = rememberPagerState(initialPage = initialSelection)

  Scaffold(
    topBar = {
      Column() {
        CreateIndexCodeToolbar(close = close, centered = true)
        Divider(color = PreviewTextColor)
      }
    },
    scaffoldState = scaffoldState,
    drawerGesturesEnabled = false,
    backgroundColor = Color.White
  ) {
    Box(modifier = Modifier.padding(it)) {
      Column(
        Modifier
          .fillMaxWidth()
          .fillMaxHeight()
          .padding(start = 10.dp, end = 10.dp)
          .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start
      ) {
        CreateLinkTabView(selection = selection, tabTitles = tabTitles, pagerState = pagerState)
        HorizontalPager(
          modifier = Modifier.weight(1f),
          count = tabTitles.size,
          state = pagerState) {tabIndex ->
          Column(Modifier.weight(1f)) {
            when (tabIndex) {
               0 -> {
                //AddContactView(model, connReqInvitation.value, model.incognito.value)
                if (!isPasteInstead.value) {
                  ScanToConnectView(model, isPasteInstead = isPasteInstead) {
                  }
                } else {
                  PasteToConnectView(model, isPasteInstead = isPasteInstead){

                  }
                }
              }
              1 -> {
                UserAddressView(model)
              }
            }
          }
        }
        //MemberRoleView(roles = roles, selectedRole = selectedRole, onSelected = onSelected)
      }
    }
  }
}

@Composable
fun CreateIndexCodeToolbar(close: () -> Unit, centered: Boolean) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .height(AppBarHeight)
      .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
      .background(Color.White)
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth(),
      contentAlignment = Alignment.CenterStart
    ) {
      Row(
        Modifier
          .fillMaxHeight()
          .width(TitleInsetWithIcon - AppBarHorizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        IconButton(close) {
          Icon(
            Icons.Outlined.ArrowBackIos, stringResource(R.string.back), tint = Color.Black,
            modifier = Modifier.size(22.dp)
          )
        }
      }
      val startPadding = TitleInsetWithIcon
      val endPadding = (0 * 50f).dp
      Box(
        Modifier
          .fillMaxWidth()
          .padding(
            start = if (centered) kotlin.math.max(startPadding.value, endPadding.value).dp else startPadding,
            end = if (centered) kotlin.math.max(startPadding.value, endPadding.value).dp else endPadding
          ),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = generalGetString(R.string.generate_index_link),
          fontSize = 18.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.align(Alignment.Center),
          fontWeight = FontWeight.Medium,
          color = Color.Black
        )
      }
    }
  }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun CreateLinkTabView(
  selection: MutableState<Int>,
  pagerState: PagerState,
  tabTitles: List<String>
) {

  val coroutine = rememberCoroutineScope()

  TabRow(
    selectedTabIndex = pagerState.currentPage,
    indicator = { tabPositions ->
      TabRowDefaults.Indicator(
        Modifier.pagerTabIndicatorOffset(
          pagerState,
          tabPositions
        )
      )
    },
    backgroundColor = Color.White,
    contentColor = Color.Black,
  ) {
    tabTitles.forEachIndexed { index, s ->
      Tab(
        selected = pagerState.currentPage == index,
        onClick = {
        coroutine.launch {
          pagerState.animateScrollToPage(index)
        }
      },
        text =  {
          Text(
            s, fontSize = 14.sp,
            fontWeight = FontWeight.Medium
          )
        },
        selectedContentColor = Color.Black,
        unselectedContentColor = HighOrLowlight)
    }
  }
}

@Composable
fun MemberRoleItemView(
  memberRoleText: String,
  textColor: Color,
  onSelected: () -> Unit,
  checked: Boolean
) {
  val icon: ImageVector
  val iconColor: Color
  if (checked) {
    icon = Icons.Filled.CheckCircle
    iconColor = Color.Black
  } else {
    icon = Icons.Outlined.Circle
    iconColor = PreviewTextColor
  }

  Row(Modifier.background(Color.White)
    .clickable {
      onSelected()
    }) {
    SectionItemView() {
      Text(memberRoleText, color = textColor)
      Spacer(Modifier.fillMaxWidth().weight(1f))
      Icon(
        icon,
        contentDescription = if (checked) generalGetString(R.string.member_role_checked) else generalGetString(R.string.member_role_unchecked),
        modifier = Modifier.size(26.dp),
        tint = iconColor
      )
    }
  }
}
