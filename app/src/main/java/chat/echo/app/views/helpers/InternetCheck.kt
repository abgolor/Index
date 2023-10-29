package chat.echo.app.views.helpers

import android.os.AsyncTask
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.util.function.Consumer

internal class InternetCheck(private val mConsumer: Consumer): AsyncTask<Void, Void, Boolean>() {
  interface Consumer {
    fun accept ()
    fun reject()
  }

  init {
    execute()
  }

  override fun doInBackground(vararg params: Void?): Boolean {
    try {
      val sock = Socket()
      sock.connect(InetSocketAddress("8.8.8.8", 53), 1500)
      sock.close()
      return true
    } catch (e: IOException){
      return false
    }
  }

  override fun onPostExecute(internet: Boolean?) {
    if(internet == true) {
      mConsumer.accept()
    } else {
      mConsumer.reject()
    }
  }
}