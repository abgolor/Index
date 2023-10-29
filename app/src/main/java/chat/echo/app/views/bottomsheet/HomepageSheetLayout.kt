package chat.echo.app.views.bottomsheet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import chat.echo.app.model.ChatModel
import chat.echo.app.views.chatlist.HomepageBottomSheetType
import chat.echo.app.views.chatlist.SortChat
import chat.echo.app.views.helpers.AddContactsBottomSheetView

@Composable
fun HomepageSheetLayout(
  chatModel: ChatModel,
  bottomSheetType: HomepageBottomSheetType,
  sortChatMode: MutableState<SortChat>,
  closeSheet: () -> Unit
) {
  when(bottomSheetType){
    HomepageBottomSheetType.AddContactBottomSheet ->
      AddContactsBottomSheetView(hideBottomSheet = closeSheet, chatModel = chatModel)
    HomepageBottomSheetType.SortChartBottomSheet ->
      SortChatBottomSheetView(sortChatMode = sortChatMode, hideBottomSheet = closeSheet)
  }
}