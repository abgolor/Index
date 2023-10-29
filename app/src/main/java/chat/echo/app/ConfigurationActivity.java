package chat.echo.app;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;

import org.openintents.openpgp.util.OpenPgpAppPreference;
import org.openintents.openpgp.util.OpenPgpKeyPreference;

import chat.echo.app.views.helpers.Constants;

public class ConfigurationActivity extends PreferenceActivity {
    OpenPgpKeyPreference mKey;
    OpenPgpAppPreference mProvider;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // load preferences from xml
        addPreferencesFromResource(R.xml.base_preference);

        userId = getIntent().getStringExtra(Constants.USER_DISPLAY_NAME);

        // find preferences
        Preference openKeychainIntents = findPreference("intent_demo");
        Preference openPgpApi = findPreference("openpgp_provider_demo");
        OpenPgpAppPreference mProvider = (OpenPgpAppPreference) findPreference("openpgp_provider_list");
        mKey = (OpenPgpKeyPreference) findPreference("openpgp_key");

        openPgpApi.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(ConfigurationActivity.this, MainActivity.class));

                return false;
            }
        });

        mKey.setOpenPgpProvider(mProvider.getValue());
        mProvider.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                mKey.setOpenPgpProvider((String) newValue);
                return true;
            }
        });
        mKey.setDefaultUserId(userId + " <" +  userId + "@echo.chat" + ">");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Intent resultIntent = new Intent();
        if (mKey.handleOnActivityResult(requestCode, resultCode, data)) {
            setResult(RESULT_OK, resultIntent);
            finish();
        } else {
            setResult(RESULT_CANCELED, resultIntent);
            finish();
        }
        // other request codes...
    }
}
