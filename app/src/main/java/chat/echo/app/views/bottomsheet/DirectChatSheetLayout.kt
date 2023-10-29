package chat.echo.app.views.bottomsheet

import androidx.compose.runtime.Composable
import chat.echo.app.model.Chat
import chat.echo.app.model.ChatModel

enum class DirectChatBottomSheetType() {
  ShowChatSettings,
  ShowBurnerTimerSettings
}

@Composable
fun DirectChatSheetLayout(
  chatModel: ChatModel,
  chat: Chat,
  sharePublicKey: () -> Unit,
  toggleChatNotification: () -> Unit,
  deleteContact: () -> Unit,
  clearChat: () -> Unit,
  bottomSheetType: DirectChatBottomSheetType,
  closeSheet: () -> Unit,
  close: () -> Unit
) {
  when (bottomSheetType) {
    DirectChatBottomSheetType.ShowChatSettings ->
      DirectChatInfoSettingsBottomSheetView(
        hideBottomSheet = closeSheet,
        chatInfo = chat.chatInfo,
        sharePublicKey = sharePublicKey,
        toggleChatNotification = toggleChatNotification,
        clearChat = clearChat,
        deleteContact = deleteContact
      )
    DirectChatBottomSheetType.ShowBurnerTimerSettings ->
      DirectChatBurnerKeyBottomSheetView(
        chatModel = chatModel,
        chatInfo = chat.chatInfo,
        onBurnerTimeChanged = {
          closeSheet()
          close()
        },
        hideBottomSheet = closeSheet
      )
  }
}