package app.akexorcist.ioiocamerarobot.controller;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AlertDialog;
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

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

import app.akexorcist.ioiocamerarobot.R;
import app.akexorcist.ioiocamerarobot.constant.Command;
import app.akexorcist.ioiocamerarobot.constant.ExtraKey;

public class ControllerActivity extends Activity implements ConnectionManager.IOIOResponseListener,
        ConnectionManager.ConnectionListener, OnClickListener, SeekBar.OnSeekBarChangeListener,
        JoyStickManager.JoyStickEventListener {

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

        connectionManager = new ConnectionManager(this, ipAddress, password);
        connectionManager.start();
        connectionManager.setConnectionListener(this);
        connectionManager.setResponseListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        connectionManager.stop();
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
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.view_simple_textview, ipList);
        ListView lvAvailablePreviewSize = ipListDialog.findViewById(R.id.lv_available_preview_size);
        lvAvailablePreviewSize.setAdapter(adapter);
        lvAvailablePreviewSize.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                connectionManager.sendCommand(ipList.get(position));
                ipListDialog.cancel();
            }
        });
        ipListDialog.show();
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
        connectionManager.sendMovement(Command.STOP);
        connectionManager.sendMovement(Command.STOP);
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
}
