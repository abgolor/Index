package chat.echo.app.views.newchat.group

import SectionDivider
import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.echo.app.R
import chat.echo.app.model.ChatModel
import chat.echo.app.views.chat.group.AddGroupMembersView
import chat.echo.app.views.helpers.ModalManager
import chat.echo.app.views.helpers.generalGetString
import chat.echo.app.views.newchat.AddGroupView
import chat.echo.app.views.usersettings.*

@SuppressLint("UnrememberedMutableState")
@Composable
fun SelectGroupTypeView(
  chatModel: ChatModel,
  close:() -> Unit
){
  Column(
    modifier = Modifier
      .fillMaxSize()
      .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
      .background(Color.White)
  ) {
    Column(Modifier.fillMaxWidth()) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
          .background(Color.White)
      ) {
        Column(Modifier.fillMaxWidth()) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
          ) {
            IconButton(close) {
              Icon(
                Icons.Outlined.ArrowBackIos, stringResource(R.string.back), tint = Color.Black,
                modifier = Modifier.size(20.dp)
              )
            }
            Text(
              text = generalGetString(R.string.create),
              fontSize = 16.sp,
              modifier = Modifier.align(Alignment.Center),
              fontWeight = FontWeight.Medium,
              color = Color.Black
            )
          }
        }
        SettingsActionItem(
          Icons.Outlined.GroupAdd,
          stringResource(R.string.new_secret_group),
          {
            ModalManager.shared.showCustomModal { close -> AddGroupView(chatModel, isZeroKnowledge = false, close) }
          },
          Color.Black,
          Color.Black
        )
        SectionDivider()
        SettingsActionItem(
          Icons.Outlined.DynamicForm,
          stringResource(R.string.zero_knowledge_group),
          {
            ModalManager.shared.showCustomModal { close -> AddGroupView(chatModel, isZeroKnowledge = true, close) }
          },
          Color.Black,
          Color.Black
        )
        SectionDivider()
      }
    }
  }
  LaunchedEffect(key1 = chatModel.isGroupCreated.value){
    if(chatModel.isGroupCreated.value) {
      close.invoke()
    }
  }
}