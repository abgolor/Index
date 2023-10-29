package chat.echo.app

import android.app.Application
import android.net.LocalServerSocket
import android.util.Log
import androidx.lifecycle.*
import androidx.work.*
import chat.echo.app.model.*
import chat.echo.app.views.helpers.*
import chat.echo.app.views.onboarding.OnboardingStage
import chat.echo.app.views.usersettings.NotificationsMode
import kotlinx.coroutines.*
import kotlinx.serialization.decodeFromString
import java.io.*
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

const val TAG = "SIMPLEX"

// ghc's rts
external fun initHS()

// android-support
external fun pipeStdOutToSocket(socketName: String): Int

// SimpleX API
typealias ChatCtrl = Long

external fun chatMigrateInit(dbPath: String, dbKey: String, confirm: String): Array<Any>
external fun chatSendCmd(ctrl: ChatCtrl, msg: String): String
external fun chatRecvMsg(ctrl: ChatCtrl): String
external fun chatRecvMsgWait(ctrl: ChatCtrl, timeout: Int): String
external fun chatParseMarkdown(str: String): String
external fun chatParseServer(str: String): String
external fun chatPasswordHash(pwd: String, salt: String): String

class SimplexApp: Application(), LifecycleEventObserver {
  lateinit var chatController: ChatController
  var isAppOnForeground: Boolean = false
  val defaultLocale: Locale = Locale.getDefault()

  fun initChatController(useKey: String? = null, confirmMigrations: MigrationConfirmation? = null, startChat: Boolean = true) {
    val dbKey = useKey ?: DatabaseUtils.useDatabaseKey()
    val dbAbsolutePathPrefix = getFilesDirectory(SimplexApp.context)
    val confirm = confirmMigrations ?: if (appPreferences.confirmDBUpgrades.get()) MigrationConfirmation.Error else MigrationConfirmation.YesUp
    val migrated: Array<Any> = chatMigrateInit(dbAbsolutePathPrefix, dbKey, confirm.value)
    val res: DBMigrationResult = kotlin.runCatching {
      json.decodeFromString<DBMigrationResult>(migrated[0] as String)
    }.getOrElse { DBMigrationResult.Unknown(migrated[0] as String) }
    val ctrl = if (res is DBMigrationResult.OK) {
      migrated[1] as Long
    } else null
    if (::chatController.isInitialized) {
      chatController.ctrl = ctrl
    } else {
      chatController = ChatController(ctrl, ntfManager, applicationContext, appPreferences)
    }
    chatModel.chatDbEncrypted.value = dbKey != ""
    chatModel.chatDbStatus.value = res
    if (res != DBMigrationResult.OK) {
      Log.d(TAG, "Unable to migrate successfully: $res")
    } else if (startChat) {
      // If we migrated successfully means previous re-encryption process on database level finished successfully too
      if (appPreferences.encryptionStartedAt.get() != null) appPreferences.encryptionStartedAt.set(null)
      withApi {
        val user = chatController.apiGetActiveUser()
        val publicKey = chatModel.controller.appPrefs.publicKey.get()!!
        val openKeyChainID = chatModel.controller.appPrefs.openKeyChainID.get()!!
        if (user == null) {
          chatModel.onboardingStage.value = OnboardingStage.NewUser
        } else {
          chatModel.onboardingStage.value = OnboardingStage.SigningIn
          chatController.startChat(user)
          chatController.showBackgroundServiceNoticeIfNeeded()
          if (appPreferences.notificationsMode.get() == NotificationsMode.SERVICE.name)
            SimplexService.start(applicationContext)
        }
      }
    }
  }

  val chatModel: ChatModel
    get() = chatController.chatModel
  private val ntfManager: NtfManager by lazy {
    NtfManager(applicationContext, appPreferences)
  }
  private val appPreferences: AppPreferences by lazy {
    AppPreferences(applicationContext)
  }

  override fun onCreate() {
    super.onCreate()
    context = this
      initChatController()
   // initChatController()
    ProcessLifecycleOwner.get().lifecycle.addObserver(this)

    context.getDir("temp", MODE_PRIVATE).deleteRecursively()
  }

  override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
    Log.d(TAG, "onStateChanged: $event")
    withApi {
      when (event) {
        Lifecycle.Event.ON_START -> {
          isAppOnForeground = true
          InternetCheckerWorker.scheduleWork(applicationContext)
          if (chatModel.chatRunning.value == true) {
            kotlin.runCatching {
              val currentUserId = chatModel.currentUser.value?.userId
              val chats = ArrayList(chatController.apiGetChats())
              /** Active user can be changed in background while [ChatController.apiGetChats] is executing */
              if (chatModel.currentUser.value?.userId == currentUserId) {
                val currentChatId = chatModel.chatId.value
                val oldStats = if (currentChatId != null) chatModel.getChat(currentChatId)?.chatStats else null
                if (oldStats != null) {
                  val indexOfCurrentChat = chats.indexOfFirst { it.id == currentChatId }
                  /** Pass old chatStats because unreadCounter can be changed already while [ChatController.apiGetChats] is executing */
                  if (indexOfCurrentChat >= 0) chats[indexOfCurrentChat] = chats[indexOfCurrentChat].copy(chatStats = oldStats)
                }
                chatModel.updateChats(chats)
              }
            }.onFailure { Log.e(TAG, it.stackTraceToString()) }
          }
        }
        Lifecycle.Event.ON_RESUME -> {
          isAppOnForeground = true
          if (chatModel.onboardingStage.value == OnboardingStage.SigningIn) {
            chatController.showBackgroundServiceNoticeIfNeeded()
          }
          /**
           * We're starting service here instead of in [Lifecycle.Event.ON_START] because
           * after calling [ChatController.showBackgroundServiceNoticeIfNeeded] notification mode in prefs can be changed.
           * It can happen when app was started and a user enables battery optimization while app in background
           * */
          if (chatModel.chatRunning.value != false &&
            chatModel.onboardingStage.value == OnboardingStage.SigningIn &&
            appPreferences.notificationsMode.get() == NotificationsMode.SERVICE.name
          ) {
            SimplexService.start(applicationContext)
          }
        }
        else -> {
          InternetCheckerWorker.cancelAll()
          isAppOnForeground = false
        }
      }
    }
  }

  fun allowToStartServiceAfterAppExit() = with(chatModel.controller) {
    appPrefs.notificationsMode.get() == NotificationsMode.SERVICE.name &&
        (!NotificationsMode.SERVICE.requiresIgnoringBattery || isIgnoringBatteryOptimizations(chatModel.controller.appContext))
  }

  private fun allowToStartPeriodically() = with(chatModel.controller) {
    appPrefs.notificationsMode.get() == NotificationsMode.PERIODIC.name &&
        (!NotificationsMode.PERIODIC.requiresIgnoringBattery || isIgnoringBatteryOptimizations(chatModel.controller.appContext))
  }

  /*
  * It takes 1-10 milliseconds to process this function. Better to do it in a background thread
  * */
  fun schedulePeriodicServiceRestartWorker() = CoroutineScope(Dispatchers.Default).launch {
    if (!allowToStartServiceAfterAppExit()) {
      return@launch
    }
    val workerVersion = chatController.appPrefs.autoRestartWorkerVersion.get()
    val workPolicy = if (workerVersion == SimplexService.SERVICE_START_WORKER_VERSION) {
      Log.d(TAG, "ServiceStartWorker version matches: choosing KEEP as existing work policy")
      ExistingPeriodicWorkPolicy.KEEP
    } else {
      Log.d(TAG, "ServiceStartWorker version DOES NOT MATCH: choosing REPLACE as existing work policy")
      chatController.appPrefs.autoRestartWorkerVersion.set(SimplexService.SERVICE_START_WORKER_VERSION)
      ExistingPeriodicWorkPolicy.REPLACE
    }
    val work = PeriodicWorkRequestBuilder<SimplexService.ServiceStartWorker>(SimplexService.SERVICE_START_WORKER_INTERVAL_MINUTES, TimeUnit.MINUTES)
      .addTag(SimplexService.TAG)
      .addTag(SimplexService.SERVICE_START_WORKER_WORK_NAME_PERIODIC)
      .build()
    Log.d(TAG, "ServiceStartWorker: Scheduling period work every ${SimplexService.SERVICE_START_WORKER_INTERVAL_MINUTES} minutes")
    WorkManager.getInstance(context)?.enqueueUniquePeriodicWork(SimplexService.SERVICE_START_WORKER_WORK_NAME_PERIODIC, workPolicy, work)
  }

  fun schedulePeriodicWakeUp() = CoroutineScope(Dispatchers.Default).launch {
    if (!allowToStartPeriodically()) {
      return@launch
    }
    MessagesFetcherWorker.scheduleWork()
  }

  companion object {
    lateinit var context: SimplexApp private set

    init {
      val socketName = BuildConfig.APPLICATION_ID + ".local.socket.address.listen.native.cmd2"
      val s = Semaphore(0)
      thread(name = "stdout/stderr pipe") {
        Log.d(TAG, "starting server")
        var server: LocalServerSocket? = null
        for (i in 0..100) {
          try {
            server = LocalServerSocket(socketName + i)
            break
          } catch (e: IOException) {
            Log.e(TAG, e.stackTraceToString())
          }
        }
        if (server == null) {
          throw Error("Unable to setup local server socket. Contact developers")
        }
        Log.d(TAG, "started server")
        s.release()
        val receiver = server.accept()
        Log.d(TAG, "started receiver")
        val logbuffer = FifoQueue<String>(500)
        if (receiver != null) {
          val inStream = receiver.inputStream
          val inStreamReader = InputStreamReader(inStream)
          val input = BufferedReader(inStreamReader)
          Log.d(TAG, "starting receiver loop")
          while (true) {
            val line = input.readLine() ?: break
            Log.w("$TAG (stdout/stderr)", line)
            logbuffer.add(line)
          }
          Log.w(TAG, "exited receiver loop")
        }
      }

      System.loadLibrary("app-lib")

      s.acquire()
      pipeStdOutToSocket(socketName)

      initHS()
    }
  }
}

class FifoQueue<E>(private var capacity: Int): LinkedList<E>() {
  override fun add(element: E): Boolean {
    if (size > capacity) removeFirst()
    return super.add(element)
  }
}
