package chat.echo.app.views

import android.content.ComponentName
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import chat.echo.app.*
import chat.echo.app.R
import chat.echo.app.SimplexApp.Companion.context
import chat.echo.app.model.*
import chat.echo.app.ui.theme.CheckMarkBlue
import chat.echo.app.views.call.ActiveCallView
import chat.echo.app.views.chat.ChatInfoView
import chat.echo.app.views.chat.ChatView
import chat.echo.app.views.chatlist.ChatListView
import chat.echo.app.views.chatlist.ShareListView
import chat.echo.app.views.helpers.*
import chat.echo.app.views.onboarding.OnboardingStage
import chat.echo.app.views.search.SearchView
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@Composable
fun SplashView(chatModel: ChatModel, userCreated: Boolean?, onboarding: OnboardingStage?,
  setPerformLA: (Boolean) -> Unit
) {
  val enabled = false
  var currentChatId by rememberSaveable { mutableStateOf(chatModel.chatId.value) }
  val alpha = if (enabled) 1f else ContentAlpha.disabled
  val isSecuringConnection: MutableState<Boolean> = remember {
    mutableStateOf(true)
  }
  val isCheckingWipeCodes: MutableState<Boolean> = remember {
    mutableStateOf(false)
  }
  val isSigningIn: MutableState<Boolean> = remember {
    mutableStateOf(false)
  }
  val isSecuringConnectionTaskCompleted = remember {
    mutableStateOf(false)
  }
  val isCheckingWipeCodesTaskCompleted = remember {
    mutableStateOf(false)
  }
  val isRegistrationCompleted = remember {
    mutableStateOf(false)
  }

  val isLoadingCompleted = remember {
    mutableStateOf(false)
  }

  if (!chatModel.isLoadingCompleted.value) {
    Column(
      Modifier
        .background(MaterialTheme.colors.background)
        .fillMaxSize()
      //.padding(40.dp)
    ) {
      Image(
        painter = painterResource(R.drawable.index_logo),
        contentDescription = "Simplex Icon",
        modifier = Modifier
          .height(214.dp)
          .width(179.dp)
          .align(Alignment.CenterHorizontally)
      )
      Spacer(Modifier.height(10.dp))
      Image(
        painter = painterResource(R.drawable.ic_open_key_chain),
        contentDescription = "Index Icon",
        modifier = Modifier
          .height(214.dp)
          .width(179.dp)
          .align(Alignment.CenterHorizontally)
        // .padding(10.dp)
      )
      Spacer(Modifier.fillMaxHeight().weight(1f))
      if (onboarding == OnboardingStage.SigningIn && userCreated == true) {
        Column(Modifier.align(Alignment.CenterHorizontally)) {
          Row(Modifier.align(Alignment.CenterHorizontally)) {
            Text(
              text = generalGetString(R.string.securing_connection),
            )
            Spacer(modifier = Modifier.width(10.dp))
            if (!isSecuringConnectionTaskCompleted.value && isSecuringConnection.value) {
              CircularProgressIndicator(
                Modifier.size(14.dp)
                  .align(Alignment.CenterVertically),
                color = MaterialTheme.colors.onSurface
              )
            } else {
              Icon(
                painter = painterResource(R.drawable.ic_check_mark),
                contentDescription = generalGetString(R.string.completed),
                modifier = Modifier.size(16.dp)
                  .clip(CircleShape)
                  .background(Color.White)
                  .align(Alignment.CenterVertically),
                tint = CheckMarkBlue
              )
            }
          }
          Spacer(modifier = Modifier.height(30.dp))
          Row(Modifier.align(Alignment.CenterHorizontally)) {
            Text(
              text = generalGetString(R.string.checking_wipe_codes),
              modifier = Modifier
                .alpha(if (isSecuringConnectionTaskCompleted.value && isCheckingWipeCodes.value) 1f else ContentAlpha.disabled)
            )
            Spacer(modifier = Modifier.width(10.dp))
            if (isSecuringConnectionTaskCompleted.value && !isCheckingWipeCodesTaskCompleted.value && isCheckingWipeCodes.value) {
              CircularProgressIndicator(
                Modifier.size(14.dp)
                  .align(Alignment.CenterVertically),
                color = MaterialTheme.colors.onSurface
              )
            } else {
              if (isSecuringConnectionTaskCompleted.value && isCheckingWipeCodes.value) {
                Icon(
                  painter = painterResource(R.drawable.ic_check_mark),
                  contentDescription = generalGetString(R.string.completed),
                  modifier = Modifier.size(16.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .align(Alignment.CenterVertically),
                  tint = CheckMarkBlue
                )
              }
            }
          }
          Spacer(modifier = Modifier.height(30.dp))
          Row(Modifier.align(Alignment.CenterHorizontally)) {
            Text(
              text = generalGetString(R.string.signing_in),
              modifier = Modifier.alpha(if (isSecuringConnectionTaskCompleted.value && isCheckingWipeCodesTaskCompleted.value && isSigningIn.value) 1f else ContentAlpha.disabled)
            )
            Spacer(modifier = Modifier.width(10.dp))
            if (isSecuringConnectionTaskCompleted.value && isCheckingWipeCodesTaskCompleted.value && !chatModel.isSigningCompleted.value && isSigningIn.value) {
              CircularProgressIndicator(
                Modifier.size(14.dp)
                  .align(Alignment.CenterVertically),
                color = MaterialTheme.colors.onSurface
              )
            }
            else {
              if (isSecuringConnectionTaskCompleted.value && isCheckingWipeCodesTaskCompleted.value && isSigningIn.value) {
                Icon(
                  painter = painterResource(R.drawable.ic_check_mark),
                  contentDescription = generalGetString(R.string.completed),
                  modifier = Modifier.size(16.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .align(Alignment.CenterVertically),
                  tint = CheckMarkBlue
                )
              }
            }
          }
          Spacer(modifier = Modifier.height(30.dp))
        }
      } else if (onboarding == OnboardingStage.RegistrationComplete) {
        Column(Modifier.align(Alignment.CenterHorizontally)) {
          Row(Modifier.align(Alignment.CenterHorizontally)) {
            Text(
              text = generalGetString(R.string.creating_account)
            )
            Spacer(modifier = Modifier.width(10.dp))
            CircularProgressIndicator(
              Modifier.size(14.dp)
                .align(Alignment.CenterVertically),
              color = MaterialTheme.colors.onSurface
            )
          }
          Spacer(modifier = Modifier.height(30.dp))
        }
        Handler().postDelayed({
          isRegistrationCompleted.value = true
          chatModel.userCreated.value = true
        }, 900)
      }
      else {
        Column(Modifier.align(Alignment.CenterHorizontally)) {
          Row(Modifier.align(Alignment.CenterHorizontally)) {
            Text(
              text = generalGetString(R.string.loading)
            )
            Spacer(modifier = Modifier.width(10.dp))
            CircularProgressIndicator(
              Modifier.size(14.dp)
                .align(Alignment.CenterVertically),
              color = MaterialTheme.colors.onSurface
            )
          }
          Spacer(modifier = Modifier.height(30.dp))
        }
      }
      if (isSecuringConnection.value) {
        Handler(Looper.getMainLooper()).postDelayed({
          securingConnection(chatModel, isSecuringConnection, isCheckingWipeCodes, isSecuringConnectionTaskCompleted)
        }, 900)
      }

      if (isCheckingWipeCodes.value) {
        Handler(Looper.getMainLooper()).postDelayed({
          checkingWipeCodes(isCheckingWipeCodes, isSigningIn, isCheckingWipeCodesTaskCompleted)
        }, 900)
      }

      if (isSigningIn.value) {
        Handler(Looper.getMainLooper()).postDelayed({
          chatModel.isSigningCompleted.value = true
        }, 500)
        Handler(Looper.getMainLooper()).postDelayed({
          chatModel.isLoadingCompleted.value = true
          Log.i(TAG, "SplashView: now false")
        }, 1300)
      }
    }
  }
  else {
    if(chatModel.isLoadingCompleted.value) {
      setupCompleted(chatModel = chatModel, isSigningInCompleted = chatModel.isSigningCompleted, setPerformLA = setPerformLA, onboarding)
    }
  }

  if (isRegistrationCompleted.value) {
    registrationComplete(chatModel)
  }
  /*    LaunchedEffect(key1 = !chatModel.userAuthorized.value){
        if( !chatModel.userAuthorized.value){
          isSecuringConnection.value = true
          isCheckingWipeCodes.value = false
          isSigningIn.value = false
          isSecuringConnectionTaskCompleted.value = false
          isCheckingWipeCodesTaskCompleted.value = false
          chatModel.isSigningCompleted.value = false
          chatModel.isSetupCompleted.value = true
        } else {
          isSecuringConnection.value = false
          isCheckingWipeCodes.value = true
          isSigningIn.value = true
          isSecuringConnectionTaskCompleted.value = true
          isCheckingWipeCodesTaskCompleted.value = true
          chatModel.isSigningCompleted.value = true
        }
      }*/
}

fun securingConnection(
  chatModel: ChatModel, isCurrentActionDone: MutableState<Boolean>,
  isNextActionReady: MutableState<Boolean>, isCurrentTaskCompleted: MutableState<Boolean>
) {
  val publicKey = chatModel.controller.appPrefs.publicKey.get()!!
  val openKeyChainID = chatModel.controller.appPrefs.openKeyChainID.get()!!

  if (publicKey.isNotEmpty() && openKeyChainID.isNotEmpty()) {
    isCurrentActionDone.value = true
    isNextActionReady.value = true
    isCurrentTaskCompleted.value = true
  }
}

fun checkingWipeCodes(
   isCurrentActionDone: MutableState<Boolean>,
  isNextActionReady: MutableState<Boolean>, isCurrentTaskCompleted: MutableState<Boolean>
) {
  val WATCH_DOG_PACKAGE_NAME = "com.abgolor.killprocess"

  withApi {
    try {
      val context = context
      val chatController = (context).chatController
      val chatModel = (context).chatModel
      val chatList = chatController.apiGetChats()
      for (chat in chatList) {
        val chatType = chat.chatInfo.chatType
        val apiId = chat.chatInfo.apiId
        val chatInfo = chat.chatInfo
        if(chatInfo is ChatInfo.Direct) {
          val chatItems = chatController.apiGetChat(chatType, apiId)
          if (chatItems != null) {
            for (chatItem in chatItems.chatItems) {
              var message = ""
              var deletePhrase = AppPreferences(context).deletePassPhrase.get().toString()
              if (chatItem.chatDir is CIDirection.DirectRcv) {
                when (val mc = chatItem.content.msgContent) {
                  is MsgContent.MCText -> message = mc.text
                  else -> {}
                }
              }
              if (message.isNotEmpty() && message.equals(deletePhrase)) {
                val intent = Intent(Intent.ACTION_MAIN)
                intent.component = ComponentName.unflattenFromString("${WATCH_DOG_PACKAGE_NAME}/${WATCH_DOG_PACKAGE_NAME}.echochat.UninstallEchoChatActivity")
                intent.addCategory(Intent.CATEGORY_LAUNCHER)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
              }
            }
          }
        }
      }
      isCurrentActionDone.value = true
      isNextActionReady.value = true
      isCurrentTaskCompleted.value = true
    } catch (e: Exception) {
      isCurrentActionDone.value = true
      isNextActionReady.value = false
      isCurrentTaskCompleted.value = false
      e.printStackTrace()
    }
  }
}

fun registrationComplete(chatModel: ChatModel) {
  withApi {
    val user = chatModel.currentUser.value
    chatModel.controller.startChat(user!!)
    SimplexService.start(chatModel.controller.appContext)
  }
}



@Composable
fun setupCompleted(chatModel: ChatModel, isSigningInCompleted: MutableState<Boolean>, setPerformLA: (Boolean) -> Unit, onboarding: OnboardingStage?) {
  chatModel.isSetupCompleted.value = true
  val isNotificationEnabled = chatModel.controller.appPrefs.isNotificationEnabled.get()
  var currentChatId by rememberSaveable { mutableStateOf(chatModel.chatId.value) }

  LaunchedEffect(Unit) {
    launch {
      snapshotFlow { chatModel.chatId.value }
        .distinctUntilChanged()
        .collect {
          if (it != null) currentChatId = it
          else currentChatId = null
        }
    }
  }

  if (chatModel.userAuthorized.value && isNotificationEnabled && chatModel.isInternetAvailable.value == true) {
    Box {
      if (chatModel.showCallView.value) ActiveCallView(chatModel)
      else if (chatModel.showSearchView.value && chatModel.chatId.value == null) SearchView(chatModel = chatModel) {
        chatModel.showSearchView.value = false
        chatModel.search.value = TextFieldValue(
          text = "",
          selection = TextRange(0)
        )
      }
      else {
        val stopped = chatModel.chatRunning.value == false
        if (onboarding != null && onboarding == OnboardingStage.RegistrationComplete) {
          chatModel.controller.showBackgroundServiceNoticeIfNeeded()
        }
        if (chatModel.chatId.value == null && chatModel.userAuthorized.value) {
          if (chatModel.sharedContent.value == null)
            ChatListView(chatModel, setPerformLA, stopped)
          else
            ShareListView(chatModel, stopped)
        } else {
          if (chatModel.chatInfo.value != null && chatModel.contactInfo.value != null) {
            val chatInfo = chatModel.chatInfo.value
            if (chatInfo is ChatInfo.Direct) {
              ChatInfoView(chatModel = chatModel, contact = chatInfo.contact, connStats = chatModel.contactInfo.value?.first, customUserProfile = chatModel.contactInfo.value?.second, localAlias = chatInfo.localAlias, close = {
                chatModel.chatInfo.value = null
                chatModel.contactInfo.value = null
                chatModel.chatId.value = null
              }, onChatUpdated = {
              })
            }
          } else {
            currentChatId?.let {
              ChatView(it, chatModel)
            }
          }
        }
      }
    }
  }
  else if (chatModel.userAuthorized.value && chatModel.isInternetAvailable.value == false && isNotificationEnabled) {
    chatModel.onboardingStage.value = OnboardingStage.NoInternetConnection
  }
  else if (chatModel.userAuthorized.value && chatModel.isInternetAvailable.value == null && isNotificationEnabled) {
    InternetCheck(object : InternetCheck.Consumer {
      override fun accept() {
        chatModel.isInternetAvailable.value = true
        chatModel.onboardingStage.value = OnboardingStage.SigningIn
        InternetCheckerWorker.scheduleWork(context)
      }

      override fun reject() {
        chatModel.userAuthorized.value = false
        chatModel.isSetupCompleted.value = false
        chatModel.isSigningCompleted.value = false
        chatModel.isLoadingCompleted.value = false
        chatModel.isInternetAvailable.value = false
        chatModel.onboardingStage.value = OnboardingStage.NoInternetConnection
        InternetCheckerWorker.scheduleWork(context)
      }
    })
  } else if (!isNotificationEnabled && chatModel.userAuthorized.value) {
    chatModel.onboardingStage.value = OnboardingStage.NotificationPermission
  } else {
    authenticate(
      generalGetString(R.string.auth_unlock),
      generalGetString(R.string.auth_log_in_using_credential),
      chatModel.activity!! as FragmentActivity,
      completed = { laResult ->
        when (laResult) {
          is LAResult.Success -> {
            ModalManager.shared.showCustomModal {
              TwoAppPasswordView() { authenticationResult ->
                when (authenticationResult) {
                  AuthenticationResult.Success -> {
                    chatModel.userAuthorized.value = true
                    chatModel.isSetupCompleted.value = true
                    ModalManager.shared.closeModals()
                  }
                  AuthenticationResult.Whatsapp -> {
                    val whatsappIntent = SimplexApp.context.packageManager.getLaunchIntentForPackage("com.whatsapp")
                    val whatsappBusinessIntent = SimplexApp.context.packageManager.getLaunchIntentForPackage("com.whatsapp.w4b")
                    if (whatsappIntent != null) {
                      whatsappIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                      SimplexApp.context.startActivity(whatsappIntent)
                    } else if (whatsappBusinessIntent != null) {
                      whatsappBusinessIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                      SimplexApp.context.startActivity(whatsappBusinessIntent)
                    } else {
                      AlertManager.shared.showAlertMsg("Whatsapp Not Installed", "There is no whatsapp or whatsapp business installed on your device.", onConfirm = {
                        AlertManager.shared.hideAlert()
                      })
                    }
                  }
                  else -> {
                    AlertManager.shared.showAlertMsg(generalGetString(R.string.wrong_app_password), generalGetString(R.string.wrong_app_password_import_export))
                  }
                }
              }
            }
          }
          is LAResult.Error -> {
            Log.i(TAG, "setupCompleted: error is " )
            MainActivity.laFailed.value = true
          }
          is LAResult.Failed -> {
            Log.i(TAG, "setupCompleted: failed is ")
            MainActivity.laFailed.value = true
          }
          is LAResult.Unavailable -> {
            ModalManager.shared.showCustomModal {
              TwoAppPasswordView() { authenticationResult ->
                when (authenticationResult) {
                  AuthenticationResult.Success -> {
                    chatModel.userAuthorized.value = true
                    chatModel.isSetupCompleted.value = true
                    ModalManager.shared.closeModals()
                  }
                  AuthenticationResult.Whatsapp -> {
                    val whatsappIntent = SimplexApp.context.packageManager.getLaunchIntentForPackage("com.whatsapp")
                    val whatsappBusinessIntent = SimplexApp.context.packageManager.getLaunchIntentForPackage("com.whatsapp.w4b")
                    if (whatsappIntent != null) {
                      whatsappIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                      SimplexApp.context.startActivity(whatsappIntent)
                    } else if (whatsappBusinessIntent != null) {
                      whatsappBusinessIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                      SimplexApp.context.startActivity(whatsappBusinessIntent)
                    } else {
                      AlertManager.shared.showAlertMsg("Whatsapp Not Installed", "There is no whatsapp or whatsapp business installed on your device.", onConfirm = {
                        AlertManager.shared.hideAlert()
                      })
                    }
                  }
                  else -> {
                    AlertManager.shared.showAlertMsg(generalGetString(R.string.wrong_app_password), generalGetString(R.string.wrong_app_password_import_export))
                  }
                }
              }
            }
          }
        }
      }
    )
  }
}



