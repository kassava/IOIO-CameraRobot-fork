package app.akexorcist.ioiocamerarobot.ioio;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.os.Build;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

import app.akexorcist.ioiocamerarobot.R;
import app.akexorcist.ioiocamerarobot.constant.ExtraKey;

public class IOIOSetupActivity extends Activity implements OnClickListener, OnSeekBarChangeListener {

    private TextView tvImageQuality;
    private EditText etIpAddress;
    private SeekBar sbImageQuality;
    private Button btnOk;
    private Button btnPreviewSizeChooser;
    private ArrayList<String> previewSizeList;

    private int selectedSizePosition;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        setContentView(R.layout.activity_ioio_setup);

        SharedPreferences settings = getSharedPreferences(ExtraKey.SETUP_PREFERENCE, Context.MODE_PRIVATE);
        selectedSizePosition = settings.getInt(ExtraKey.PREVIEW_SIZE, 0);
        String ipAddress = settings.getString(ExtraKey.OWN_IP_ADDRESS, "");
        int quality = settings.getInt(ExtraKey.QUALITY, 100);

        initCameraPreviewSize();

        etIpAddress = findViewById(R.id.et_ip);
        setInputFilterForEditText(etIpAddress);
        etIpAddress.setText(ipAddress);

        btnPreviewSizeChooser = findViewById(R.id.btn_preview_size_chooser);
        updateSeletedPreviewSize();
        btnPreviewSizeChooser.setOnClickListener(this);

        tvImageQuality = findViewById(R.id.tv_image_quality);
        updateTextViewQuality(quality);

        sbImageQuality = findViewById(R.id.sb_image_quality);
        sbImageQuality.setProgress(quality);
        sbImageQuality.setOnSeekBarChangeListener(this);

        btnOk = findViewById(R.id.btn_ok);
        btnOk.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_preview_size_chooser) {
            createPreviewSizeChooserDialog();
        } else if (id == R.id.btn_ok) {
            confirmSetup();
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        saveImageQuality(progress);
        updateTextViewQuality(progress);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    public void updateSeletedPreviewSize() {
        String strSize = previewSizeList.get(selectedSizePosition);
        btnPreviewSizeChooser.setText(strSize);
    }

    @SuppressLint("StringFormatMatches")
    public void updateTextViewQuality(int quality) {
        tvImageQuality.setText(getString(R.string.image_quality, quality));
    }

    public void saveIpAddress(String ipAddress) {
        getPreferenceEditor().putString(ExtraKey.OWN_IP_ADDRESS, ipAddress).apply();
    }

    public void saveImageQuality(int quality) {
        getPreferenceEditor().putInt(ExtraKey.QUALITY, quality).apply();
    }

    public void saveImagePreviewSize(int size) {
        getPreferenceEditor().putInt(ExtraKey.PREVIEW_SIZE, size).apply();
    }

    public SharedPreferences.Editor getPreferenceEditor() {
        SharedPreferences settings = getSharedPreferences(ExtraKey.SETUP_PREFERENCE, Context.MODE_PRIVATE);
        return settings.edit();
    }

    public void goToIOIOController() {
        Intent intent = new Intent(this, IOIOControllerActivity.class);
        intent.putExtra(ExtraKey.OWN_IP_ADDRESS, etIpAddress.getText().toString());
        intent.putExtra(ExtraKey.PREVIEW_SIZE, selectedSizePosition);
        intent.putExtra(ExtraKey.QUALITY, sbImageQuality.getProgress());
        startActivity(intent);
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
    public void initPreviewSizeList(List<Size> previewSize) {
        previewSizeList = new ArrayList<>();
        for (int i = 0; i < previewSize.size(); i++) {
            String str = previewSize.get(i).width + " x " + previewSize.get(i).height;
            previewSizeList.add(str);
        }
        JSONArray jsonArray = new JSONArray(previewSizeList);
        getPreferenceEditor().putString(ExtraKey.PREVIEW_SIZE_SET, jsonArray.toString()).apply();
    }

    public void createPreviewSizeChooserDialog() {
        final Dialog dialogSize = new Dialog(this);
        dialogSize.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialogSize.setContentView(R.layout.dialog_camera_size);
        dialogSize.setCancelable(true);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.view_simple_textview, previewSizeList);
        ListView lvAvailablePreviewSize = dialogSize.findViewById(R.id.lv_available_preview_size);
        lvAvailablePreviewSize.setAdapter(adapter);
        lvAvailablePreviewSize.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                selectedSizePosition = position;
                saveImagePreviewSize(position);
                updateSeletedPreviewSize();
                dialogSize.cancel();
            }
        });
        dialogSize.show();
    }

    public void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    public void confirmSetup() {
        String strIpAddress = etIpAddress.getText().toString();
        if (strIpAddress.length() != 0) {
            saveIpAddress(strIpAddress);
            goToIOIOController();
        } else {
            showToast(getString(R.string.ip_address_unavailable));
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
