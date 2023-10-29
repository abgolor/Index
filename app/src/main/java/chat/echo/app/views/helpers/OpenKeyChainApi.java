package chat.echo.app.views.helpers;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import org.openintents.openpgp.IOpenPgpService2;
import org.openintents.openpgp.OpenPgpError;
import org.openintents.openpgp.util.OpenPgpApi;
import org.openintents.openpgp.util.OpenPgpAppPreference;
import org.openintents.openpgp.util.OpenPgpKeyPreference;
import org.openintents.openpgp.util.OpenPgpServiceConnection;
import org.openintents.openpgp.util.OpenPgpUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import chat.echo.app.OpenKeyChainCallBack;
import chat.echo.app.R;

public class OpenKeyChainApi {
    private static final String TAG = OpenKeyChainApi.class.getSimpleName();

    private Context context;
    private OpenPgpServiceConnection mServiceConnection;
    private Activity activity;

    public OpenKeyChainApi(Context context, OpenPgpServiceConnection mServiceConnection, Activity activity) {
        this.context = context;
        this.mServiceConnection = mServiceConnection;
        this.activity = activity;
    }

    private InputStream getInputStream(String message) {
        InputStream is = null;
        try {
            is = new ByteArrayInputStream(message.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "UnsupportedEncodingException", e);
        }

        return is;
    }


    public void signAndEncrypt(String message, String[] userIds) throws IOException {
        Intent data = new Intent();
        data.setAction(OpenPgpApi.ACTION_ENCRYPT);
        data.putExtra(OpenPgpApi.EXTRA_USER_IDS, userIds);
        data.putExtra(OpenPgpApi.EXTRA_REQUEST_ASCII_ARMOR, true);
        InputStream inputStream = new ByteArrayInputStream(message.getBytes("UTF-8"));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        OpenPgpApi api = new OpenPgpApi(activity.getApplicationContext(), mServiceConnection.getService());
        OpenKeyChainCallBack openKeyChainCallBack = new OpenKeyChainCallBack(context, outputStream, 1002, activity);
        api.executeApiAsync(data, inputStream, outputStream, openKeyChainCallBack);
    }


}
