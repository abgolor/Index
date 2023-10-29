package chat.echo.app;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import chat.echo.app.model.AppPreferences;
import chat.echo.app.model.ChatModel;

public class TwoAppPasswordActivity extends AppCompatActivity {

    private static final String TAG = TwoAppPasswordActivity.class.getSimpleName();
    private TextView authenticationExplanation;
    private EditText twoAppPasswordEdt;
    private Button authentication;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_two_app_password);

        authenticationExplanation = findViewById(R.id.two_app_password_explanation);
        twoAppPasswordEdt = findViewById(R.id.edt_app_password);
        authentication = findViewById(R.id.authenticate);

        authentication.setOnClickListener(v -> {
            if(isAppPassword(twoAppPasswordEdt.getText().toString())){
                Intent resultData = new Intent();
                setResult(RESULT_OK, resultData);
                finish();
            } else {

            }
        });
    }

    public boolean isAppPassword(String password){
        String savedPassword = new AppPreferences(getApplicationContext()).getPasswordOne().getGet().invoke();
        Log.i(TAG, "isAppPassword: saved password is " + savedPassword);
        if(savedPassword.equals(password)) return true;
        return false;
    }
}