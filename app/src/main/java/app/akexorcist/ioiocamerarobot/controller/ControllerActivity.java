package app.akexorcist.ioiocamerarobot.controller;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.clans.fab.FloatingActionButton;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

import app.akexorcist.ioiocamerarobot.R;
import app.akexorcist.ioiocamerarobot.constant.Command;
import app.akexorcist.ioiocamerarobot.constant.ExtraKey;
import app.akexorcist.ioiocamerarobot.utils.CompassView;

public class ControllerActivity extends Activity implements ConnectionManager.IOIOResponseListener,
        ConnectionManager.ConnectionListener, OnClickListener, SeekBar.OnSeekBarChangeListener,
        JoyStickManager.JoyStickEventListener, OnMapReadyCallback {

    private final String LOG_TAG = ControllerActivity.class.getSimpleName();

    private ImageView ivCameraImage;

    private ConnectionManager connectionManager;
    private ArrayList<String> previewSizeList;
    private int selectedSizePosition;
    private JoyStickManager joyStickManager;

    private FloatingActionButton fabTakePhoto;
    private FloatingActionButton fabAutoFocus;
    private FloatingActionButton fabQuality;
    private FloatingActionButton fabFlash;
    private RelativeLayout layoutJoyStick;

    private CompassView compassView;
    private MapView mapView;
    private static final String MAPVIEW_BUNDLE_KEY = "MapViewBundleKey";

    private TextView tvBitrate;

    private Button btnPreviewSizeChooser;
    private TextView textView;
    private SeekBar sbImageQuality;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_controller);

        int screenHeight = getWindowManager().getDefaultDisplay().getHeight();

        String ipAddress = getIntent().getExtras().getString(ExtraKey.IP_ADDRESS);
        String password = "19655";

        ivCameraImage = findViewById(R.id.iv_camera_image);

        layoutJoyStick = findViewById(R.id.layout_joystick);
        joyStickManager = new JoyStickManager(this, layoutJoyStick, screenHeight);
        joyStickManager.setJoyStickEventListener(this);

        fabTakePhoto = findViewById(R.id.fab_take_photo);
        fabTakePhoto.setOnClickListener(this);

        fabAutoFocus = findViewById(R.id.fab_auto_focus);
        fabAutoFocus.setOnClickListener(this);

        fabQuality = findViewById(R.id.fab_quality);
        fabQuality.setOnClickListener(this);

        fabFlash = findViewById(R.id.fab_flash);
        fabFlash.setOnClickListener(this);

        compassView = findViewById(R.id.compassView);

        // *** IMPORTANT ***
        // MapView requires that the Bundle you pass contain _ONLY_ MapView SDK
        // objects or sub-Bundles.
        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY);
        }
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(mapViewBundle);
        mapView.getMapAsync(this);

        tvBitrate = findViewById(R.id.tvBitrate);

        connectionManager = new ConnectionManager(this, ipAddress, password);
        connectionManager.start();
        connectionManager.setConnectionListener(this);
        connectionManager.setResponseListener(this);


    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        Bundle mapViewBundle = outState.getBundle(MAPVIEW_BUNDLE_KEY);
        if (mapViewBundle == null) {
            mapViewBundle = new Bundle();
            outState.putBundle(MAPVIEW_BUNDLE_KEY, mapViewBundle);
        }

        mapView.onSaveInstanceState(mapViewBundle);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
        handler.post(updateBitrate);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    public void onMapReady(GoogleMap map) {
        map.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));
    }

    @Override
    protected void onPause() {
        mapView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mapView.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void onStop() {
        super.onStop();
        connectionManager.stop();
        mapView.onStop();
//        finish();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fab_auto_focus:
                requestAutoFocus();
                break;
            case R.id.fab_take_photo:
                requestTakePhoto();
                break;
            case R.id.fab_quality:
                changeQuality();
                break;
            case R.id.btn_preview_size:
                createPreviewSizeChooserDialog();
                break;
            case R.id.fab_flash:
                changeFlash();
                break;
        }
    }

    public void createPreviewSizeChooserDialog() {
        final Dialog dialogSize = new Dialog(this);
        dialogSize.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialogSize.setContentView(R.layout.dialog_camera_size);
        dialogSize.setCancelable(true);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.view_simple_textview, previewSizeList);
        ListView lvAvailablePreviewSize = dialogSize.findViewById(R.id.lv_available_preview_size);
        lvAvailablePreviewSize.setAdapter(adapter);
        lvAvailablePreviewSize.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                selectedSizePosition = position;
                saveImagePreviewSize(position);
                updateSeletedPreviewSize();
                dialogSize.cancel();
            }
        });
        dialogSize.show();
    }

    public void saveImagePreviewSize(int size) {
        getPreferenceEditor().putInt(ExtraKey.PREVIEW_SIZE, size).apply();
    }

    public SharedPreferences.Editor getPreferenceEditor() {
        SharedPreferences settings = getSharedPreferences(ExtraKey.SETUP_PREFERENCE, Context.MODE_PRIVATE);
        return settings.edit();
    }

    public void updateSeletedPreviewSize() {
        String strSize = previewSizeList.get(selectedSizePosition);
        btnPreviewSizeChooser.setText(strSize);
    }

    public void requestAutoFocus() {
        connectionManager.sendCommand(Command.FOCUS);
    }

    public void requestTakePhoto() {
        connectionManager.sendCommand(Command.SNAP);
    }

    public void changeQuality() {
        if (previewSizeList == null) {
            return;
        }
        ConstraintLayout view = (ConstraintLayout) getLayoutInflater().inflate(R.layout.dialog_image_quality, null);
        btnPreviewSizeChooser = view.findViewById(R.id.btn_preview_size);
        btnPreviewSizeChooser.setOnClickListener(this);
        textView = view.findViewById(R.id.tv_im_quality);
        SharedPreferences settings = getSharedPreferences(ExtraKey.SETUP_PREFERENCE, Context.MODE_PRIVATE);
        int quality = settings.getInt(ExtraKey.QUALITY, 100);
        textView.setText(getString(R.string.image_quality, quality));
        sbImageQuality = view.findViewById(R.id.sb_im_quality);
        sbImageQuality.setProgress(quality);
        sbImageQuality.setOnSeekBarChangeListener(this);
        String strSize = previewSizeList.get(selectedSizePosition);
        btnPreviewSizeChooser.setText(strSize);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(view)
                .setTitle(R.string.quality_selection)
                .setPositiveButton("Добро",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                JSONArray jsonArray = new JSONArray();
                                jsonArray.put(selectedSizePosition);
                                jsonArray.put(sbImageQuality.getProgress());
                                String command = Command.QUALITY + ":"
                                        + jsonArray.toString();
                                connectionManager.sendCommand(command);
                                dialog.dismiss();
                            }
                        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void changeFlash() {
        if (fabFlash.getLabelText().equals(getString(R.string.flash_off))) {
            fabFlash.setLabelText(getString(R.string.flash_on));
            fabFlash.setImageResource(R.drawable.ic_flash_on);
            onCheckedChanged(true);
        } else {
            fabFlash.setLabelText(getString(R.string.flash_off));
            fabFlash.setImageResource(R.drawable.ic_flash_on);
            onCheckedChanged(false);
        }
    }

    public void onCheckedChanged(boolean isChecked) {
        if (isChecked) {
            connectionManager.sendCommand(Command.LED_ON);
        } else {
            connectionManager.sendCommand(Command.LED_OFF);
        }
    }

    @Override
    public void onPictureTaken() {
        showToast(getString(R.string.photo_taken));
    }

    @Override
    public void onFlashUnavailable() {
        showToast(getString(R.string.unsupport_flash));
    }

    @Override
    public void onCameraImageIncoming(Bitmap bitmap) {
        ivCameraImage.setImageBitmap(bitmap);
    }

    @Override
    public void onPreviewSizesResponse(String previewSizesStr) {
        try {
            Log.d(LOG_TAG, "onPreviewSizesResponse: " + previewSizesStr);
            JSONArray jsonArray = new JSONArray(previewSizesStr);
            previewSizeList = new ArrayList<>();
            int len = jsonArray.length();
            for (int i = 0; i < len; i++){
                previewSizeList.add(jsonArray.get(i).toString());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onConnectionDown() {
        showToast(getString(R.string.connection_down));
        finish();
    }

    @Override
    public void onConnectionFailed() {
        showToast(getString(R.string.connection_failed));
//        finish();
    }

    @Override
    public void onWrongPassword() {
        showToast(getString(R.string.wrong_password));
        finish();
    }

    @Override
    public void onIOIOConnected() {
        showToast(getString(R.string.connection_accepted));
    }

    @Override
    public void onSourcesIpList(String ipListStr) {
        final Dialog ipListDialog = new Dialog(this);
        ipListDialog.setTitle("Выберите ip источника");
        ipListDialog.setContentView(R.layout.dialog_camera_size);
        ipListDialog.setCancelable(true);

        final ArrayList<String> ipList = new ArrayList<>();
        ipList.addAll(getStrings(ipListStr));
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.view_simple_textview, ipList);
        ListView lvAvailablePreviewSize = ipListDialog.findViewById(R.id.lv_available_preview_size);
        lvAvailablePreviewSize.setAdapter(adapter);
        lvAvailablePreviewSize.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                connectionManager.sendCommand(Command.SELECTED_IP + ipList.get(position));
                ipListDialog.cancel();
            }
        });
        ipListDialog.show();
    }

    private List<String> getStrings(String response) {
        JSONArray arr;
        List<String> list = new ArrayList<>();
        try {
            arr = new JSONArray(response);
            for(int i = 0; i < arr.length(); i++){
                list.add(arr.getString(i));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return list;
    }

    public void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onJoyStickUp(int speed) {
        connectionManager.sendMovement(Command.FORWARD + speed);
    }

    @Override
    public void onJoyStickUpRight(int speed) {
        connectionManager.sendMovement(Command.FORWARD_RIGHT + speed);
    }

    @Override
    public void onJoyStickUpLeft(int speed) {
        connectionManager.sendMovement(Command.FORWARD_LEFT + speed);
    }

    @Override
    public void onJoyStickDown(int speed) {
        connectionManager.sendMovement(Command.BACKWARD + speed);
    }

    @Override
    public void onJoyStickDownRight(int speed) {
        connectionManager.sendMovement(Command.BACKWARD_RIGHT + speed);
    }

    @Override
    public void onJoyStickDownLeft(int speed) {
        connectionManager.sendMovement(Command.BACKWARD_LEFT + speed);
    }

    @Override
    public void onJoyStickRight(int speed) {
        connectionManager.sendMovement(Command.RIGHT + speed);
    }

    @Override
    public void onJoyStickLeft(int speed) {
        connectionManager.sendMovement(Command.LEFT + speed);
    }

    @Override
    public void onJoyStickNone() {
        connectionManager.sendMovement(Command.STOP + "0");
        connectionManager.sendMovement(Command.STOP + "0");
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            saveImageQuality(i);
            updateTextViewQuality(i);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    public void saveImageQuality(int quality) {
        getPreferenceEditor().putInt(ExtraKey.QUALITY, quality).apply();
    }

    public void updateTextViewQuality(int quality) {
        textView.setText(getString(R.string.image_quality, quality));
    }

    private final Handler handler = new Handler();

    private Runnable updateBitrate = new Runnable() {
        @Override
        public void run() {
            if (connectionManager != null) {
                long bitrate = connectionManager.getBitrate();
                String bitrateStr = "" + bitrate / 1000 +" kbps";
                if (tvBitrate != null) {
                    tvBitrate.setText(bitrateStr);
                }
                handler.postDelayed(updateBitrate, 1000);
            } else {
                tvBitrate.setText("0 kbps");
            }
        }
    };
}
