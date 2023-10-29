package chat.echo.app

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.IntentSender.SendIntentException
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.os.SystemClock.elapsedRealtime
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat.startIntentSenderForResult
import androidx.core.view.*
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import chat.echo.app.model.*
import chat.echo.app.ui.theme.SimpleXTheme
import chat.echo.app.views.*
import chat.echo.app.views.call.IncomingCallAlertView
import chat.echo.app.views.chatlist.*
import chat.echo.app.views.database.CHAT_IMPORT_EXPORT_SUCCESSFUL
import chat.echo.app.views.database.DatabaseErrorView
import chat.echo.app.views.helpers.*
import chat.echo.app.views.helpers.localcontact.LocalDatabaseViewModel
import chat.echo.app.views.newchat.connectViaUri
import chat.echo.app.views.newchat.withUriAction
import chat.echo.app.views.notification.NotificationPermissionView
import chat.echo.app.views.onboarding.*
import kotlinx.coroutines.delay
import org.openintents.openpgp.IOpenPgpService2
import org.openintents.openpgp.OpenPgpError
import org.openintents.openpgp.util.OpenPgpApi
import org.openintents.openpgp.util.OpenPgpServiceConnection
import org.openintents.openpgp.util.OpenPgpServiceConnection.OnBound
import java.io.ByteArrayOutputStream
import java.io.UnsupportedEncodingException

class MainActivity: FragmentActivity(){
  companion object {
    /**
     * We don't want these values to be bound to Activity lifecycle since activities are changed often, for example, when a user
     * clicks on new message in notification. In this case savedInstanceState will be null (this prevents restoring the values)
     * See [SimplexService.onTaskRemoved] for another part of the logic which nullifies the values when app closed by the user
     * */
   // val userAuthorized = mutableStateOf<Boolean?>(null)
    val enteredBackground = mutableStateOf<Long?>(null)

    // Remember result and show it after orientation change
    val laFailed = mutableStateOf(false)

    fun clearAuthState() {
     // userAuthorized.value = null
      ModalManager.shared.closeModals()
      enteredBackground.value = null
    }
  }

  private val vm by viewModels<SimplexViewModel>()

  private lateinit var  mServiceConnection: OpenPgpServiceConnection

  private val notificationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){isGranted ->
    if(isGranted){

    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // testJson()
    val m = vm.chatModel
    //val mProvider = findPreference("openpgp_provider_list") as OpenPgpAppPreference
    mServiceConnection = OpenPgpServiceConnection(SimplexApp.context, "org.sufficientlysecure.keychain.debug",
      object: OnBound{
        override fun onBound(p0: IOpenPgpService2?) {
          vm.chatModel.mServiceConnection = mServiceConnection
          vm.chatModel.activity = this@MainActivity
          Log.i(TAG, "onBound: bounded")
        }

        override fun onError(p0: Exception?) {
          vm.chatModel.mServiceConnection = null
          //Log.i(TAG, "onError: error is " + p0.toString())
        }
      })
    mServiceConnection.bindToService()
    m.controller.appPrefs.laNoticeShown.set(false)
    if(checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED){
      m.controller.appPrefs.isNotificationEnabled.set(true)
    } else {
      if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
        m.controller.appPrefs.isNotificationEnabled.set(false)
      } else {
        m.controller.appPrefs.isNotificationEnabled.set(true)
      }
    }

    // When call ended and orientation changes, it re-process old intent, it's unneeded.
    // Only needed to be processed on first creation of activity
    if (savedInstanceState == null) {
      processNotificationIntent(intent, m)
      processExternalIntent(intent, m)
    }
    setContent {
      SimpleXTheme {
        Surface(
          Modifier
            .background(MaterialTheme.colors.background)
            .fillMaxSize()
        ) {
          val owner = LocalViewModelStoreOwner.current
          owner?.let {
            val viewModel: LocalDatabaseViewModel = viewModel(
              it,
              "LocalDatabaseViewModel",
              MainViewModelFactory(
                LocalContext.current.applicationContext
                    as Application
              )
            )
            m.localContactViewModel = viewModel
          }
          Log.i(TAG, "onCreate: this validated is " + vm.chatModel.userAuthorized.value)
          MainPage(
            m,
            ::setPerformLA,
            showLANotice = {/* m.controller.showLANotice(this)*/ }
          )
        }
      }
    }

    SimplexApp.context.schedulePeriodicServiceRestartWorker()
    SimplexApp.context.schedulePeriodicWakeUp()
    //

    //Scroll bottom layout content to top when keyboard is active
    ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)){ view, insets ->
      val bottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
      view.updatePadding(bottom = bottom)
      insets
    }

  }

  override fun onNewIntent(intent: Intent?) {
    super.onNewIntent(intent)
    processIntent(intent, vm.chatModel)
    processExternalIntent(intent, vm.chatModel)
  }

  override fun onStart() {
    super.onStart()
    vm.chatModel.registerBroadcastReceiver(applicationContext)
    val enteredBackgroundVal = enteredBackground.value
    if (enteredBackgroundVal == null || elapsedRealtime() - enteredBackgroundVal >= 30 * 1e+3) {
     vm.chatModel.userAuthorized.value = false
      vm.chatModel.isSetupCompleted.value = false
      vm.chatModel.isSigningCompleted.value = false
      vm.chatModel.isLoadingCompleted.value = false
    }
  }

  override fun onStop() {
    super.onStop()
    Log.i(TAG, "onStop: stopped")
    enteredBackground.value = elapsedRealtime()
    vm.chatModel.unregisterBroadcastReceiver(applicationContext)
  }

  override fun onBackPressed() {
    super.onBackPressed()
    if (!onBackPressedDispatcher.hasEnabledCallbacks() && vm.chatModel.controller.appPrefs.performLA.get()) {
      // When pressed Back and there is no one wants to process the back event, clear auth state to force re-auth on launch
      clearAuthState()
      laFailed.value = true
    }
    if (!onBackPressedDispatcher.hasEnabledCallbacks()) {
      // Drop shared content
      SimplexApp.context.chatModel.sharedContent.value = null
    }
  }

/*  fun runAuthenticate() {
    if(intent.getBooleanExtra(CHAT_IMPORT_EXPORT_SUCCESSFUL, false) ){
      vm.chatModel.userAuthorized.value = true
    } else {
      val m = vm.chatModel
      if (!m.controller.appPrefs.performLA.get()) {
        vm.chatModel.userAuthorized.value = true
      } else {
        vm.chatModel.userAuthorized.value = false
        authenticate(
          generalGetString(R.string.auth_unlock),
          generalGetString(R.string.auth_log_in_using_credential),
          this@MainActivity,
          completed = { laResult ->
            when (laResult) {
              LAResult.Success -> {
                ModalManager.shared.showCustomModal {
                  TwoAppPasswordView() { authenticationResult ->
                    when (authenticationResult) {
                      AuthenticationResult.Success -> {
                        vm.chatModel.userAuthorized.value = true
                        ModalManager.shared.closeModals()
                      }
                      AuthenticationResult.Whatsapp -> {
                        val whatsappIntent = SimplexApp.context.packageManager.getLaunchIntentForPackage("com.whatsapp")
                        val whatsappBusinessIntent = SimplexApp.context.packageManager.getLaunchIntentForPackage("com.whatsapp.w4b")
                        if (whatsappIntent != null) {
                          whatsappIntent.addFlags(FLAG_ACTIVITY_NEW_TASK)
                          SimplexApp.context.startActivity(whatsappIntent)
                        } else if (whatsappBusinessIntent != null) {
                          whatsappBusinessIntent.addFlags(FLAG_ACTIVITY_NEW_TASK)
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
                laFailed.value = true
                laErrorToast(applicationContext, laResult.errString)
              }
              LAResult.Failed -> {
                laFailed.value = true
                laFailedToast(applicationContext)
              }
              LAResult.Unavailable -> {
                */
  /* userAuthorized.value = true
              m.performLA.value = false
              m.controller.appPrefs.performLA.set(false)
              laUnavailableTurningOffAlert()*/
  /*
                ModalManager.shared.showCustomModal {
                  TwoAppPasswordView() { authenticationResult ->
                    when (authenticationResult) {
                      AuthenticationResult.Success -> {
                        vm.chatModel.userAuthorized.value = true
                        ModalManager.shared.closeModals()
                      }
                      AuthenticationResult.Whatsapp -> {
                        val whatsappIntent = SimplexApp.context.packageManager.getLaunchIntentForPackage("com.whatsapp")
                        val whatsappBusinessIntent = SimplexApp.context.packageManager.getLaunchIntentForPackage("com.whatsapp.w4b")
                        if (whatsappIntent != null) {
                          whatsappIntent.addFlags(FLAG_ACTIVITY_NEW_TASK)
                          SimplexApp.context.startActivity(whatsappIntent)
                        } else if (whatsappBusinessIntent != null) {
                          whatsappBusinessIntent.addFlags(FLAG_ACTIVITY_NEW_TASK)
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
  }*/

 fun setPerformLA(on: Boolean) {
    vm.chatModel.controller.appPrefs.laNoticeShown.set(true)
    /*if (on) {
      enableLA()
    } else {
      disableLA()
    }*/

    AlertManager.shared.showAlertMsg("Operation Blocked!",
      "So sorry! But we can't allow you turn off authentication because your privacy is our concern."){
      AlertManager.shared.hideAlert()
    }
  }

  private fun enableLA() {
    val m = vm.chatModel
    authenticate(
      generalGetString(R.string.auth_enable_simplex_lock),
      generalGetString(R.string.auth_confirm_credential),
      this@MainActivity,
      completed = { laResult ->
        val prefPerformLA = m.controller.appPrefs.performLA
        when (laResult) {
          LAResult.Success -> {
            if(AppPreferences(SimplexApp.context).passwordOne.get().equals("") &&
                AppPreferences(SimplexApp.context).passwordTwo.get().equals("")){
              Log.i(TAG, "enableLA: password one i s " + AppPreferences(SimplexApp.context).passwordOne.get())
              ModalManager.shared.showCustomModal {
                CreateTwoAppPasswordView(m, isFirstTime = false)
              }
            } else {
              m.performLA.value = true
              prefPerformLA.set(true)
              laTurnedOnAlert()
            }
          }
          is LAResult.Error -> {
            m.performLA.value = false
            prefPerformLA.set(false)
            laErrorToast(applicationContext, laResult.errString)
          }
          LAResult.Failed -> {
            m.performLA.value = false
            prefPerformLA.set(false)
            laFailedToast(applicationContext)
          }
          LAResult.Unavailable -> {
            m.performLA.value = false
            prefPerformLA.set(false)
            laUnavailableInstructionAlert()
          }
        }
      }
    )
  }

  private fun disableLA() {
    val m = vm.chatModel
    authenticate(
      generalGetString(R.string.auth_disable_simplex_lock),
      generalGetString(R.string.auth_confirm_credential),
      this@MainActivity,
      completed = { laResult ->
        val prefPerformLA = m.controller.appPrefs.performLA
        when (laResult) {
          LAResult.Success -> {
            m.performLA.value = false
            prefPerformLA.set(false)
          }
          is LAResult.Error -> {
            m.performLA.value = true
            prefPerformLA.set(true)
            laErrorToast(applicationContext, laResult.errString)
          }
          LAResult.Failed -> {
            m.performLA.value = true
            prefPerformLA.set(true)
            laFailedToast(applicationContext)
          }
          LAResult.Unavailable -> {
            m.performLA.value = false
            prefPerformLA.set(false)
            laUnavailableTurningOffAlert()
          }
        }
      }
    )
  }
}

class OpenKeyChainCallBack(context: Context, os: ByteArrayOutputStream, requestCode: Int,  activity: Activity): OpenPgpApi.IOpenPgpCallback {
 val context: Context
  val os: ByteArrayOutputStream
  private val requestCode: Int
  val activity: Activity

  init {
    this.context = context
    this.os = os
    this.requestCode = requestCode
    this.activity = activity
  }

  override fun onReturn(result: Intent?) {
    if (result != null) {
      when (result.getIntExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR)) {
        OpenPgpApi.RESULT_CODE_SUCCESS -> {
          if(os != null){
            try {
              Log.d(TAG, "onReturn: result is "  + os.toString("UTF-8"))
            } catch (e: UnsupportedEncodingException){
              Log.e(TAG, "onReturn: Unsupported Encoding Exception", e)
            }
          }
        }
        OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED -> {
          val pi: PendingIntent? = result.getParcelableExtra(OpenPgpApi.RESULT_INTENT)
          try {
            startIntentSenderForResult(activity, pi!!.intentSender,
              requestCode, null, 0, 0, 0, null
            )
          } catch (e: SendIntentException) {
            Log.e(TAG, "SendIntentException", e)
          }
        }
        OpenPgpApi.RESULT_CODE_ERROR -> {
          val error: OpenPgpError? = result.getParcelableExtra(OpenPgpApi.RESULT_ERROR)
          if (error != null){
            Log.d(TAG, "onReturn: error is " + error.message)
          }
        }
      }
    }
  }
}

class SimplexViewModel(application: Application): AndroidViewModel(application) {
  val app = getApplication<SimplexApp>()
  val chatModel = app.chatModel
}

@Composable
fun MainPage(
  chatModel: ChatModel,
  setPerformLA: (Boolean) -> Unit,
  showLANotice: () -> Unit
) {
  // this with LaunchedEffect(userAuthorized.value) fixes bottom sheet visibly collapsing after authentication
  var chatsAccessAuthorized by rememberSaveable { mutableStateOf(false) }
  LaunchedEffect(chatModel.userAuthorized.value) {
    if (chatModel.controller.appPrefs.performLA.get()) {
      delay(500L)
    }
    chatsAccessAuthorized = /*chatModel.isSetupCompleted.value == false || */chatModel.userAuthorized.value == true
  }
  var showChatDatabaseError by rememberSaveable {
    mutableStateOf(chatModel.chatDbStatus.value != DBMigrationResult.OK && chatModel.chatDbStatus.value != null)
  }
  LaunchedEffect(chatModel.chatDbStatus.value) {
    showChatDatabaseError = chatModel.chatDbStatus.value != DBMigrationResult.OK && chatModel.chatDbStatus.value != null
  }
  var showAdvertiseLAAlert by remember { mutableStateOf(false) }
  LaunchedEffect(showAdvertiseLAAlert) {
    if (
      !chatModel.controller.appPrefs.laNoticeShown.get()
      && showAdvertiseLAAlert
      && chatModel.onboardingStage.value == OnboardingStage.SigningIn
      && chatModel.chats.isNotEmpty()
      && chatModel.activeCallInvitation.value == null
    ) {
      showLANotice()
    }
  }
  LaunchedEffect(chatModel.showAdvertiseLAUnavailableAlert.value) {
    if (chatModel.showAdvertiseLAUnavailableAlert.value) {
      laUnavailableInstructionAlert()
    }
  }
  LaunchedEffect(chatModel.clearOverlays.value) {
    if (chatModel.clearOverlays.value) {
      ModalManager.shared.closeModals()
      chatModel.clearOverlays.value = false
    }
  }

  @Composable
  fun retryAuthView() {
    val enabled = false
    val alpha = if (enabled) 1f else ContentAlpha.disabled

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
      Column(Modifier.align(Alignment.CenterHorizontally)) {
        Row(Modifier.align(Alignment.CenterHorizontally)) {
          Text(
            text = generalGetString(R.string.signing_in)
          )
          Spacer(modifier = Modifier.width(10.dp))
          CircularProgressIndicator(
            Modifier.height(14.dp)
              .width(14.dp)
              .align(Alignment.CenterVertically),
            color = MaterialTheme.colors.onSurface
          )
        }
        Spacer(modifier = Modifier.height(30.dp))
      }
    }
  }

/*  @Composable
  fun retryAuthView() {
    Box(
      Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center
    ) {
      SimpleButton(
        stringResource(R.string.auth_retry),
        icon = Icons.Outlined.Replay,
        click = {
          laFailed.value = false
          runAuthenticate()
        }
      )
    }
  }*/

  Box {
    val onboarding = chatModel.onboardingStage.value
    val userCreated = chatModel.userCreated.value
    val isNotificationAllowed = remember{
      mutableStateOf(chatModel.controller.appPrefs.isNotificationEnabled.get())
    }



    when {
      showChatDatabaseError -> {
        chatModel.chatDbStatus.value?.let {
          DatabaseErrorView(chatModel.chatDbStatus, chatModel.controller.appPrefs)
        }
      }

      onboarding == null || userCreated == null -> SplashView(chatModel, null, null, setPerformLA)
     /* !chatsAccessAuthorized -> {
        Log.i(TAG, "MainPage: here is called ")
        if (chatModel.controller.appPrefs.performLA.get() && laFailed.value) {
         //retryAuthView()
          if(onboarding == OnboardingStage.SigningIn) {
            SplashView(chatModel, true, OnboardingStage.SigningIn, setPerformLA)
          }
        } else {
          if (chatModel.currentUser.value != null) {
            SplashView(chatModel, true, OnboardingStage.SigningIn, setPerformLA)
          } else {
            CreateProfile(chatModel = chatModel)
          }
        }
      }*/

      onboarding == OnboardingStage.NotificationPermission -> NotificationPermissionView(chatModel)
      onboarding == OnboardingStage.NoInternetConnection ->
        NoInternetConnectionView()
      onboarding == OnboardingStage.SigningIn && userCreated -> {
        SplashView(chatModel, userCreated = userCreated, onboarding = onboarding, setPerformLA)
      }
      onboarding == OnboardingStage.RegistrationComplete -> {
        /*if (!isNotificationAllowed.value){
          NotificationPermissionView(isNotificationPermissionAccepted = isNotificationAllowed)
        } else {
        }*/
        SplashView(chatModel, userCreated = userCreated, onboarding = onboarding, setPerformLA)
      }
      onboarding == OnboardingStage.NewUser -> {
        Log.i(TAG, "MainPage: here is called for new user ")
       // chatModel.userAuthorized.value = true
        CreateProfile(chatModel)
      }
    }

    Log.i("TAG", "onboarding is " + (onboarding?.name ?: "none"))
    ModalManager.shared.showInView()
    val invitation = chatModel.activeCallInvitation.value
    if (invitation != null) IncomingCallAlertView(invitation, chatModel)
    AlertManager.shared.showInView()
  }
}

fun processNotificationIntent(intent: Intent?, chatModel: ChatModel) {
  when (intent?.action) {
    NtfManager.OpenChatAction -> {
      val chatId = intent.getStringExtra("chatId")
      Log.d(TAG, "processNotificationIntent: OpenChatAction $chatId")
      if (chatId != null) {
        val cInfo = chatModel.getChat(chatId)?.chatInfo
        chatModel.clearOverlays.value = true
        if (cInfo != null) withApi { openChat(cInfo, chatModel) }
      }
    }
    NtfManager.ShowChatsAction -> {
      Log.d(TAG, "processNotificationIntent: ShowChatsAction")
      chatModel.chatId.value = null
      chatModel.clearOverlays.value = true
    }
    NtfManager.AcceptCallAction -> {
      val chatId = intent.getStringExtra("chatId")
      if (chatId == null || chatId == "") return
      Log.d(TAG, "processNotificationIntent: AcceptCallAction $chatId")
      chatModel.clearOverlays.value = true
      val invitation = chatModel.callInvitations[chatId]
      if (invitation == null) {
        AlertManager.shared.showAlertMsg(generalGetString(R.string.call_already_ended))
      } else {
        chatModel.callManager.acceptIncomingCall(invitation = invitation)
      }
    }
  }
}

fun processIntent(intent: Intent?, chatModel: ChatModel) {
  when (intent?.action) {
    "android.intent.action.VIEW" -> {
      val uri = intent.data
      if (uri != null) connectIfOpenedViaUri(uri, chatModel)
    }
  }
}

fun processExternalIntent(intent: Intent?, chatModel: ChatModel) {
  when (intent?.action) {
    Intent.ACTION_SEND -> {
      // Close active chat and show a list of chats
      chatModel.chatId.value = null
      chatModel.clearOverlays.value = true
      when {
        intent.type == "text/plain" -> {
          val text = intent.getStringExtra(Intent.EXTRA_TEXT)
          if (text != null) {
            chatModel.sharedContent.value = SharedContent.Text(text)
          }
        }
        isMediaIntent(intent) -> {
          val uri = intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri
          if (uri != null) {
            chatModel.sharedContent.value = SharedContent.Media(intent.getStringExtra(Intent.EXTRA_TEXT) ?: "", listOf(uri))
          } // All other mime types
        }
        else -> {
          val uri = intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri
          if (uri != null) {
            chatModel.sharedContent.value = SharedContent.File(intent.getStringExtra(Intent.EXTRA_TEXT) ?: "", uri)
          }
        }
      }
    }
    Intent.ACTION_SEND_MULTIPLE -> {
      // Close active chat and show a list of chats
      chatModel.chatId.value = null
      chatModel.clearOverlays.value = true
      Log.e(TAG, "ACTION_SEND_MULTIPLE ${intent.type}")
      when {
        isMediaIntent(intent) -> {
          val uris = intent.getParcelableArrayListExtra<Parcelable>(Intent.EXTRA_STREAM) as? List<Uri>
          if (uris != null) {
            chatModel.sharedContent.value = SharedContent.Media(intent.getStringExtra(Intent.EXTRA_TEXT) ?: "", uris)
          } // All other mime types
        }
        else -> {}
      }
    }
  }
}

fun isMediaIntent(intent: Intent): Boolean =
  intent.type?.startsWith("image/") == true || intent.type?.startsWith("video/") == true

fun connectIfOpenedViaUri(uri: Uri, chatModel: ChatModel) {
  Log.d(TAG, "connectIfOpenedViaUri: opened via link")
  if (chatModel.currentUser.value == null) {
    // TODO open from chat list view
    chatModel.appOpenUrl.value = uri
  } else {
    withUriAction(uri) { action ->
      val title = when (action) {
        "contact" -> generalGetString(R.string.connect_via_contact_link)
        "invitation" -> generalGetString(R.string.connect_via_invitation_link)
        else -> {
          Log.e(TAG, "URI has unexpected action. Alert shown.")
          action
        }
      }
      AlertManager.shared.showAlertMsg(
        title = title,
        text = generalGetString(R.string.profile_will_be_sent_to_contact_sending_link),
        confirmText = generalGetString(R.string.connect_via_link_verb),
        onConfirm = {
          withApi {
            Log.d(TAG, "connectIfOpenedViaUri: connecting")
            connectViaUri(chatModel, action, uri)
          }
        }
      )
    }
  }
}

class MainViewModelFactory(val application: Application) :
  ViewModelProvider.Factory {
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    return LocalDatabaseViewModel(application) as T
  }
}

/*fun testJson() {
//  val str: String = """
//  """.trimIndent()
//
//  println(json.decodeFromString<APIResponse>(str))
//}*/
