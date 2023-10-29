package chat.echo.app.views.helpers

import android.content.Context
import android.util.Log
import androidx.work.*
import chat.echo.app.*
import chat.echo.app.model.CIDeleteMode
import chat.echo.app.model.MsgContent
import chat.echo.app.views.onboarding.OnboardingStage
import kotlinx.datetime.toJavaInstant
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit

object InternetCheckerWorker {
  private const val UNIQUE_WORK_TAG = BuildConfig.APPLICATION_ID + ".UNIQUE_INTERNET_CHECK_FETCHER"
  fun scheduleWork(context: Context, intervalSec: Int = 15, durationSec: Int = 15){
    val initialDelaySec = intervalSec.toLong()
    Log.d(TAG, "Burner Worker: scheduling work to run at ${Date(System.currentTimeMillis() + initialDelaySec * 1000)} for $durationSec")


    val context = context as SimplexApp
    val chatModel = (context).chatModel

    InternetCheck(object : InternetCheck.Consumer {
      override fun accept() {
        if(chatModel.onboardingStage.value == OnboardingStage.NoInternetConnection) {
          chatModel.userAuthorized.value = false
          chatModel.isSetupCompleted.value = false
          chatModel.isSigningCompleted.value = false
          chatModel.isLoadingCompleted.value = false
          chatModel.isInternetAvailable.value = true
          chatModel.onboardingStage.value = OnboardingStage.SigningIn
        }
      }

      override fun reject() {
        if(chatModel.onboardingStage.value == OnboardingStage.SigningIn && chatModel.userAuthorized.value) {
          chatModel.userAuthorized.value = false
          chatModel.isSetupCompleted.value = false
          chatModel.isSigningCompleted.value = false
          chatModel.isLoadingCompleted.value = false
          chatModel.isInternetAvailable.value = false
          chatModel.onboardingStage.value = OnboardingStage.NoInternetConnection
        }
      }
    })
    val periodicWorkRequest = OneTimeWorkRequest.Builder(InternetCheckWork::class.java)
      .setInitialDelay(initialDelaySec, TimeUnit.SECONDS)
      .setInputData(
        Data.Builder()
          .putInt(InternetCheckWork.INPUT_DATA_INTERVAL, intervalSec)
          .putInt(InternetCheckWork.INPUT_DATA_DURATION, durationSec)
          .build()
      )
      .build()

    WorkManager.getInstance(context).enqueueUniqueWork(UNIQUE_WORK_TAG, ExistingWorkPolicy.REPLACE, periodicWorkRequest)
  }

  fun cancelAll() {
    Log.d(TAG, "Burner Worker: canceled all tasks")
    WorkManager.getInstance(SimplexApp.context).cancelUniqueWork(UNIQUE_WORK_TAG)
  }
}

class InternetCheckWork(
  context: Context,
  workerParams: WorkerParameters
): CoroutineWorker(context, workerParams){
  companion object{
    const val INPUT_DATA_INTERVAL = "interval"
    const val INPUT_DATA_DURATION = "duration"
  }

  //val chatController = (context as SimplexApp).chatController

  val context = SimplexApp.context

  override suspend fun doWork(): Result {
    if(SimplexService.getServiceState(SimplexApp.context) == SimplexService.ServiceState.STARTED){
      reschedule()
      return Result.success()
    }
    val durationSeconds = inputData.getInt(INPUT_DATA_DURATION, 60)
    val shouldReschedule = true

    if(shouldReschedule) reschedule()
    return Result.success()

  }

  private fun reschedule() = InternetCheckerWorker.scheduleWork(context = context)
}