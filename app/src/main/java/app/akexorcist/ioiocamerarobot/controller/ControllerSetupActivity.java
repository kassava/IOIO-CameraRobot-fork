package app.akexorcist.ioiocamerarobot.controller;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputFilter;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import app.akexorcist.ioiocamerarobot.R;
import app.akexorcist.ioiocamerarobot.constant.ExtraKey;

public class ControllerSetupActivity extends Activity implements OnClickListener {

    private static final String DEFAULT_IP_ADDRESS = "192.168.1.1";
    private EditText etIpAddress;
    private Button btnConnect;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_controller_connection);
        SharedPreferences settings = getSharedPreferences(ExtraKey.SETUP_PREFERENCE, 0);

        etIpAddress = findViewById(R.id.et_ip_address);
        setInputFilterForEditText(etIpAddress);
        etIpAddress.setText(settings.getString(ExtraKey.IP_ADDRESS, DEFAULT_IP_ADDRESS));

        btnConnect = findViewById(R.id.btn_connect);
        btnConnect.setOnClickListener(this);
    }

    public void onPause() {
        super.onPause();
        saveConnectionConfiguration();
    }

    public void saveConnectionConfiguration() {
        SharedPreferences settings = getSharedPreferences(ExtraKey.SETUP_PREFERENCE, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(ExtraKey.IP_ADDRESS, etIpAddress.getText().toString());
        editor.putString(ExtraKey.TARGET_PASSWORD, "19655");
        editor.apply();
    }

    public void goToController() {
        Intent intent = new Intent(ControllerSetupActivity.this, ControllerActivity.class);
        intent.putExtra(ExtraKey.IP_ADDRESS, etIpAddress.getText().toString());
        intent.putExtra(ExtraKey.TARGET_PASSWORD, "19655");
        startActivity(intent);
    }

    public void onResume() {
        super.onResume();
        delayedEnableButton();
    }

    public void delayedEnableButton() {
        btnConnect.setEnabled(false);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                btnConnect.setEnabled(true);
            }
        }, 1000);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_connect) {
            goToController();
        }
    }

    private void setInputFilterForEditText(EditText editText) {
        InputFilter[] filters = new InputFilter[1];
        filters[0] = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end,
                                       android.text.Spanned dest, int dstart, int dend) {
                if (end > start) {
                    String destTxt = dest.toString();
                    String resultingTxt = destTxt.substring(0, dstart)
                            + source.subSequence(start, end)
                            + destTxt.substring(dend);
                    if (!resultingTxt
                            .matches("^\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3})?)?)?)?)?)?")) {
                        return "";
                    } else {
                        String[] splits = resultingTxt.split("\\.");
                        for (String split : splits) {
                            if (Integer.valueOf(split) > 255) {
                                return "";
                            }
                        }
                    }
                }
                return null;
            }
        };
        editText.setFilters(filters);
    }
}
