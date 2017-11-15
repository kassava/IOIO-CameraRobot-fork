package app.akexorcist.ioiocamerarobot.menu;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

import app.akexorcist.ioiocamerarobot.R;
import app.akexorcist.ioiocamerarobot.constant.ExtraKey;
import app.akexorcist.ioiocamerarobot.controller.ControllerActivity;
import app.akexorcist.ioiocamerarobot.ioio.IOIOControllerActivity;

public class MenuActivity extends Activity implements OnClickListener, SeekBar.OnSeekBarChangeListener {
    private SharedPreferences settings;
    private static final String DEFAULT_IP_ADDRESS = "192.168.1.1";
    private EditText clientIpAddress;
    private EditText robotIpAddress;
    private SeekBar sbImageQuality;
    private TextView tvImageQuality;
    private Button btnPreviewSizeChooser;
    private ArrayList<String> previewSizeList;
    private int selectedSizePosition;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        setContentView(R.layout.activity_main);
        settings = getSharedPreferences(ExtraKey.SETUP_PREFERENCE, Context.MODE_PRIVATE);
        Button btnController = findViewById(R.id.btn_controller);
        btnController.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                invokeControllerConnection();
            }
        });

        Button btnIoio = findViewById(R.id.btn_ioio);
        btnIoio.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                invokeControllerRobotConnection();
            }
        });
    }

    public SharedPreferences.Editor getPreferenceEditor() {
        return settings.edit();
    }

    public void saveRobotIpAddress(String ipAddress) {
        getPreferenceEditor().putString(ExtraKey.OWN_IP_ADDRESS, ipAddress).apply();
    }

    public void saveImageQuality(int quality) {
        getPreferenceEditor().putInt(ExtraKey.QUALITY, quality).apply();
    }

    public void saveImagePreviewSize(int size) {
        getPreferenceEditor().putInt(ExtraKey.PREVIEW_SIZE, size).apply();
    }

    private void invokeControllerConnection() {

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.dialog_controller_connection, null);
        dialogBuilder.setView(dialogView);

        clientIpAddress = (EditText) dialogView.findViewById(R.id.customName);
        setInputFilterForEditText(clientIpAddress);
        clientIpAddress.setText(settings.getString(ExtraKey.IP_ADDRESS, DEFAULT_IP_ADDRESS));

//        dialogBuilder.setTitle("Введите IP адрес сервера");
        dialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                goToController(clientIpAddress.getText().toString().trim());
            }
        });
        dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                //pass
            }
        });

        AlertDialog b = dialogBuilder.create();
        b.show();
    }

    private void invokeControllerRobotConnection() {

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.dialog_robot_setup, null);
        dialogBuilder.setView(dialogView);

        selectedSizePosition = settings.getInt(ExtraKey.PREVIEW_SIZE, 0);
        int quality = settings.getInt(ExtraKey.QUALITY, 100);
        initCameraPreviewSize();

        robotIpAddress = dialogView.findViewById(R.id.et_ip);
        setInputFilterForEditText(robotIpAddress);
        robotIpAddress.setText(settings.getString(ExtraKey.OWN_IP_ADDRESS, ""));

        btnPreviewSizeChooser = dialogView.findViewById(R.id.btn_preview_size_chooser);
        updateSelectedPreviewSize();
        btnPreviewSizeChooser.setOnClickListener(this);

        tvImageQuality = dialogView.findViewById(R.id.tv_image_quality);
        updateTextViewQuality(quality);

        sbImageQuality = dialogView.findViewById(R.id.sb_image_quality);
        sbImageQuality.setProgress(quality);
        sbImageQuality.setOnSeekBarChangeListener(this);

        dialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                goToRobotController();
            }
        });
        dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                //pass
            }
        });

        AlertDialog b = dialogBuilder.create();
        b.show();
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

    public void onPause() {
        super.onPause();
        saveConnectionConfiguration();
    }

    public void saveConnectionConfiguration() {
        SharedPreferences.Editor editor = getPreferenceEditor();
        if (clientIpAddress != null) {
            editor.putString(ExtraKey.IP_ADDRESS, clientIpAddress.getText().toString());
//        editor.putString(ExtraKey.TARGET_PASSWORD, etIp.getText().toString());
            editor.putString(ExtraKey.TARGET_PASSWORD, "19655");
        } else if (robotIpAddress != null) {
            editor.putString(ExtraKey.OWN_IP_ADDRESS, robotIpAddress.getText().toString());
        }
        editor.apply();
    }

    public void goToController(String ip) {
        Intent intent = new Intent(this, ControllerActivity.class);
        intent.putExtra(ExtraKey.IP_ADDRESS, ip);
//        intent.putExtra(ExtraKey.TARGET_PASSWORD, etIp.getText().toString());
        intent.putExtra(ExtraKey.TARGET_PASSWORD, "19655");
        startActivity(intent);
    }

    public void goToRobotController() {
        String strIpAddress = robotIpAddress.getText().toString();
        if (strIpAddress.length() != 0) {
            saveRobotIpAddress(strIpAddress);
            Intent intent = new Intent(this, IOIOControllerActivity.class);
            intent.putExtra(ExtraKey.OWN_IP_ADDRESS, strIpAddress);
            intent.putExtra(ExtraKey.PREVIEW_SIZE, selectedSizePosition);
            intent.putExtra(ExtraKey.QUALITY, sbImageQuality.getProgress());
            startActivity(intent);
        } else {
            showToast(getString(R.string.ip_address_unavailable));
        }

    }

    @SuppressWarnings("deprecation")
    public void initCameraPreviewSize() {
        Camera mCamera;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD) {
            mCamera = Camera.open();
        } else {
            mCamera = Camera.open(0);
        }
        Camera.Parameters params = mCamera.getParameters();
        initPreviewSizeList(params.getSupportedPreviewSizes());
        mCamera.release();
    }

    @SuppressWarnings("deprecation")
    public void initPreviewSizeList(List<Camera.Size> previewSize) {
        previewSizeList = new ArrayList<>();
        for (int i = 0; i < previewSize.size(); i++) {
            String str = previewSize.get(i).width + " x " + previewSize.get(i).height;
            previewSizeList.add(str);
        }
        JSONArray jsonArray = new JSONArray(previewSizeList);
        getPreferenceEditor().putString(ExtraKey.PREVIEW_SIZE_SET, jsonArray.toString()).apply();
    }

    public void updateSelectedPreviewSize() {
        String strSize = previewSizeList.get(selectedSizePosition);
        btnPreviewSizeChooser.setText(strSize);
    }

    @SuppressLint("StringFormatMatches")
    public void updateTextViewQuality(int quality) {
        tvImageQuality.setText(getString(R.string.image_quality, quality));
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.btn_preview_size_chooser) {
            createPreviewSizeChooserDialog();
        }
    }

    public void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
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
                updateSelectedPreviewSize();
                dialogSize.cancel();
            }
        });
        dialogSize.show();
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
}
