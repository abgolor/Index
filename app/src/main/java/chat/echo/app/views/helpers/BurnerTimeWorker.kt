package chat.echo.app.views.helpers

import android.content.Context
import android.util.Log
import androidx.work.*
import chat.echo.app.*
import chat.echo.app.model.CIDeleteMode
import chat.echo.app.model.MsgContent
import kotlinx.datetime.toJavaInstant
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit

/*
object BurnerTimeWorker {
  private const val UNIQUE_WORK_TAG = BuildConfig.APPLICATION_ID + ".UNIQUE_BURNER_TIME_FETCHER"
  fun scheduleWork(context: Context, intervalSec: Int = 10, durationSec: Int = 10){
    val initialDelaySec = intervalSec.toLong()
    Log.d(TAG, "Burner Worker: scheduling work to run at ${Date(System.currentTimeMillis() + initialDelaySec * 1000)} for $durationSec")

    withApi {

      try{
        val context = context as SimplexApp
        val chatController = (context).chatController
        val chatModel = (context).chatModel
        val chatList = chatController.apiGetChats()
        for (chat in chatList){
         val chatType =  chat.chatInfo.chatType
          val apiId = chat.chatInfo.apiId
          val chatInfo = chat.chatInfo
          val chatItems = chatController.apiGetChat(chatType, apiId)
          if (chatItems != null){
            for (chatItem in chatItems.chatItems){
              val messageCreatedAt = chatItem.meta.createdAt.toJavaInstant()
              var burntTime = 0L

              if (!chatItem.isDeletedContent) {
                when(val mc = chatItem.content.msgContent){
                  is MsgContent.MCText -> burntTime = mc.text
                  is MsgContent.MCImage -> burntTime = mc.text
                  is MsgContent.MCFile -> burntTime = mc.text
                  is MsgContent.MCLink -> burntTime = mc.text
                  is MsgContent.MCUnknown -> burntTime = mc.text
                }
              }
              val now = Instant.now()
              val expired = now.minusSeconds(burntTime)
              if(messageCreatedAt.isBefore(expired) && burntTime != 0L){
                val toItem = chatController.apiDeleteChatItem(chatType, apiId, chatItem.id, CIDeleteMode.cidmInternal)
                if (toItem != null) chatModel.removeChatItem(chatInfo, toItem.chatItem)
              }
            }
          }
        }
      } catch (e: Exception){
        e.printStackTrace()
        println("No chats found ")
      }
    }
    val periodicWorkRequest = OneTimeWorkRequest.Builder(BurnerTimeWork::class.java)
      .setInitialDelay(initialDelaySec, TimeUnit.SECONDS)
      .setInputData(
        Data.Builder()
          .putInt(BurnerTimeWork.INPUT_DATA_INTERVAL, intervalSec)
          .putInt(BurnerTimeWork.INPUT_DATA_DURATION, durationSec)
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

 class BurnerTimeWork(
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

   private fun reschedule() = BurnerTimeWorker.scheduleWork(context = context)
 }*/
