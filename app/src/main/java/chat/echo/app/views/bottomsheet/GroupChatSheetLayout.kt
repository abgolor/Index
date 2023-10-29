package chat.echo.app.views.bottomsheet

import androidx.compose.runtime.Composable
import chat.echo.app.model.*

enum class GroupChatBottomSheetType() {
  ShowChatSettings,
  ShowBurnerTimerSettings
}

@Composable
fun GroupChatSheetLayout(
  chatModel: ChatModel,
  chat: Chat,
  groupInfo: GroupInfo,
  toggleGroupChatNotification: () -> Unit,
  deleteGroup: () -> Unit,
  clearChat: () -> Unit,
  leaveGroup: () -> Unit,
  bottomSheetType: GroupChatBottomSheetType,
  closeSheet: () -> Unit,
  close: () -> Unit
) {
  when (bottomSheetType) {
    GroupChatBottomSheetType.ShowChatSettings ->
      GroupChatInfoSettingsBottomSheetView(
        hideBottomSheet = closeSheet,
        groupInfo = groupInfo,
        chatInfo = chat.chatInfo,
        toggleGroupChatNotification = toggleGroupChatNotification,
        deleteGroup = deleteGroup,
        clearChat = clearChat,
        leaveGroup = leaveGroup,
      )
    GroupChatBottomSheetType.ShowBurnerTimerSettings ->
      GroupChatBurnerKeyBottomSheetView(
        m = chatModel,
        chatId = chat.id,
        hideBottomSheet = closeSheet,
        close = close)
  }
}