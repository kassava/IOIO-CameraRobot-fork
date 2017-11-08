package app.akexorcist.ioiocamerarobot.menu;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import app.akexorcist.ioiocamerarobot.controller.ControllerSetupActivity;
import app.akexorcist.ioiocamerarobot.ioio.IOIOSetupActivity;
import app.akexorcist.ioiocamerarobot.R;

public class MenuActivity extends Activity {

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        setContentView(R.layout.activity_main);

        Button btnController = findViewById(R.id.btn_controller);
        btnController.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(MenuActivity.this, ControllerSetupActivity.class);
                startActivity(intent);
            }
        });

        Button btnIoio = findViewById(R.id.btn_ioio);
        btnIoio.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(MenuActivity.this, IOIOSetupActivity.class);
                startActivity(intent);
            }
        });
    }
}
