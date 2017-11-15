package app.akexorcist.ioiocamerarobot.menu;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.github.ivbaranov.rxbluetooth.RxBluetooth;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.util.List;

import javax.inject.Inject;

import app.akexorcist.ioiocamerarobot.controller.ControllerSetupActivity;
import app.akexorcist.ioiocamerarobot.ioio.IOIOSetupActivity;
import app.akexorcist.ioiocamerarobot.R;
import app.akexorcist.ioiocamerarobot.service.BluetoothService;
import app.akexorcist.ioiocamerarobot.service.LocationService;
import app.akexorcist.ioiocamerarobot.service.SensorService;

public class MenuActivity extends Activity {

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        setContentView(R.layout.activity_main);
        preparationApp();
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
    private void preparationApp() {
        Dexter.withActivity(this)
                .withPermissions(
                        Manifest.permission.CAMERA,
                        Manifest.permission.BLUETOOTH,
                        Manifest.permission.BLUETOOTH_ADMIN,
                        Manifest.permission.ACCESS_WIFI_STATE,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.INTERNET,
                        Manifest.permission.ACCESS_NETWORK_STATE,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ).withListener(new MultiplePermissionsListener() {
            @Override
            public void onPermissionsChecked(MultiplePermissionsReport report) {/* ... */}

            @Override
            public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {/* ... */}
        }).check();

    }
}
