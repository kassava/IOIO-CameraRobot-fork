package app.akexorcist.ioiocamerarobot.ioio;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;
import com.michaelflisar.rxbus.RXBus;
import com.michaelflisar.rxbus.RXBusBuilder;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import app.akexorcist.ioiocamerarobot.App;
import app.akexorcist.ioiocamerarobot.R;
import app.akexorcist.ioiocamerarobot.constant.Command;
import app.akexorcist.ioiocamerarobot.constant.DirectionState;
import app.akexorcist.ioiocamerarobot.constant.ExtraKey;
import app.akexorcist.ioiocamerarobot.model.Location;
import app.akexorcist.ioiocamerarobot.model.OrientationValue;
import app.akexorcist.ioiocamerarobot.service.LocationService;
import app.akexorcist.ioiocamerarobot.service.SensorService;
import app.akexorcist.ioiocamerarobot.utils.Utilities;
import rx.Subscription;
import rx.functions.Action1;

import static android.content.ContentValues.TAG;


public class IOIOControllerActivity extends Activity implements CameraManager.CameraManagerListener, Callback, ConnectionManager.ConnectionListener, ConnectionManager.ControllerCommandListener, ConnectionManager.SendCommandListener {
    @Inject
    Gson gson;

    private static final int TAKE_PICTURE_COOLDOWN = 1000;
    private final String LOG_TAG = IOIOControllerActivity.class.getSimpleName();
    private RelativeLayout layoutParent;
    private TextView tvMovementSpeed;
    private TextView tvIpAddress;
    private Button btnMoveForward;
    private Button btnMoveForwardLeft;
    private Button btnMoveForwardRight;
    private Button btnMoveDown;
    private Button btnMoveDownLeft;
    private Button btnMoveDownRight;
    private Button btnMoveRight;
    private Button btnMoveLeft;
    private SurfaceView surfacePreview;
    private Subscription subscriptionOrientation;
    private Subscription subscriptionLocation;
    private int movementSpeed = 0;
    private int lastPictureTakenTime = 0;
    private int directionState = DirectionState.STOP;

    private ConnectionManager connectionManager;
    private CameraManager cameraManager;
    private OrientationManager orientationManager;

    private SharedPreferences settings;
    /**
     * The device currently in use, or {@code null}.
     */
    private UsbSerialDriver mSerialDevice;

    /**
     * The system's USB service.
     */
    private UsbManager mUsbManager;

    private int imageQuality;
    private boolean connected = false;

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    private SerialInputOutputManager mSerialIoManager;

    private final SerialInputOutputManager.Listener mListener =
            new SerialInputOutputManager.Listener() {

                @Override
                public void onRunError(Exception e) {
                    Log.d(TAG, "Runner stopped.");
                }

                @Override
                public void onNewData(final byte[] data) {
                    Log.d(TAG, "data " + data);

                }
            };

    @SuppressWarnings("deprecation")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App.getAppComponent().inject(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_ioio);
        startServices();
        settings = getSharedPreferences(ExtraKey.SETUP_PREFERENCE, Context.MODE_PRIVATE);
        String ipAddress = settings.getString(ExtraKey.IP_ADDRESS, "192.168.1.10");
        int selectedPreviewSize = settings.getInt(ExtraKey.PREVIEW_SIZE, 0);
        imageQuality = settings.getInt(ExtraKey.QUALITY, 100);

        btnMoveForward = findViewById(R.id.btn_move_forward);
        btnMoveForwardLeft = findViewById(R.id.btn_move_forward_left);
        btnMoveForwardRight = findViewById(R.id.btn_move_forward_right);
        btnMoveDown = findViewById(R.id.btn_move_backward);
        btnMoveDownLeft = findViewById(R.id.btn_move_backward_left);
        btnMoveDownRight = findViewById(R.id.btn_move_backward_right);
        btnMoveRight = findViewById(R.id.btn_move_right);
        btnMoveLeft = findViewById(R.id.btn_move_left);

        tvMovementSpeed = findViewById(R.id.tv_movement_speed);

        tvIpAddress = findViewById(R.id.tv_ip_address);
        tvIpAddress.setText(Utilities.getCurrentIP(this));

        surfacePreview = findViewById(R.id.surface_preview);
        surfacePreview.getHolder().addCallback(this);
        surfacePreview.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);


        layoutParent = findViewById(R.id.layout_parent);
        layoutParent.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                cameraManager.requestAutoFocus();
            }
        });
        connectionManager = new ConnectionManager(ipAddress);
        connectionManager.start();
        connectionManager.setConnectionListener(this);
        connectionManager.setCommandListener(this);
        connectionManager.setSendCommandListener(this);

        orientationManager = new OrientationManager(this);
        cameraManager = new CameraManager(selectedPreviewSize);
        cameraManager.setCameraManagerListener(this);

        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

    }

    public void onStop() {
        super.onStop();
        if (subscriptionLocation != null)
            subscriptionLocation.unsubscribe();
        if (subscriptionOrientation != null)
            subscriptionOrientation.unsubscribe();
        if (connectionManager != null)
            connectionManager.stop();
        stopIoManager();
        if (mSerialDevice != null) {
            try {
                mSerialDevice.close();
            } catch (IOException e) {
                // Ignore.
            }
            mSerialDevice = null;
        }
        finish();
    }

    public void onUsbResume() {
        // Find all available drivers from attached devices.
        mSerialDevice = UsbSerialProber.acquire(mUsbManager);

        Log.d(TAG, "Resumed, mSerialDevice=" + mSerialDevice);
        if (mSerialDevice == null) {
            Log.d(TAG, "No serial device.");
        } else {
            try {
                mSerialDevice.open();
                mSerialDevice.setBaudRate(115200);
            } catch (IOException e) {
                Log.e(TAG, "Error setting up device: " + e.getMessage(), e);
                try {
                    mSerialDevice.close();
                } catch (IOException e2) {
                    // Ignore.
                }
                mSerialDevice = null;
                return;
            }
            Log.d(TAG, "Serial device: " + mSerialDevice);
        }
        onDeviceStateChange();

    }

    @Override
    public void onResume() {
        // unsubscribe - we used the RXSubscriptionManager for every subscription and bound all subscriptions to this class,
        // so following will safely unsubscribe every subscription
//        subscriptionManual.unsubscribe();
        super.onResume();
        onUsbResume();
    }

    @Override
    public void onDestroy() {
        // unsubscribe - we used the RXSubscriptionManager for every subscription and bound all subscriptions to this class,
        // so following will safely unsubscribe every subscription
//        subscriptionManual.unsubscribe();
        stopService(new Intent(this, SensorService.class));
        stopService(new Intent(this, LocationService.class));
        super.onDestroy();
    }

    public void clearCheckBox() {
        btnMoveForward.setPressed(false);
        btnMoveForwardLeft.setPressed(false);
        btnMoveForwardRight.setPressed(false);
        btnMoveDown.setPressed(false);
        btnMoveDownLeft.setPressed(false);
        btnMoveDownRight.setPressed(false);
        btnMoveRight.setPressed(false);
        btnMoveLeft.setPressed(false);
    }

    @SuppressLint("StringFormatMatches")
    public void updateMovementSpeed(int speed) {
        movementSpeed = speed;
        tvMovementSpeed.setText(getString(R.string.movement_speed, speed));
    }

    @Override
    public void onDataIncoming() {
        clearCheckBox();
    }

    @Override
    public void onChangeQuality(String string) {
        String subStr = string.substring(Command.QUALITY.length() + 1);
        try {
            JSONArray jsonArray = new JSONArray(subStr);
            imageQuality = jsonArray.getInt(1);
            restartPreview(jsonArray.getInt(0));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMoveCommandIncoming(String command) {
        Log.d(LOG_TAG, "Incoming command: " + command);
        byte[] dataToSend = command.getBytes();
        if (mSerialIoManager != null) {
            for (int i = 0; i < dataToSend.length - 1; i++) {
                if (dataToSend[i] == 0x0A) {
                    dataToSend[i] = 0x0B;
                    Log.d(TAG, "Send data: " + dataToSend[i]);

                }
            }
            // send the color to the serial device
            if (mSerialDevice != null) {
                try {
                    Log.d(TAG, "device.write");
                    mSerialDevice.write(dataToSend, 500);

                } catch (IOException e) {
                    Log.d(TAG, "couldn't write bytes to serial device");
                }
            } else {
                Log.d(TAG, "device = null");
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopIoManager();
        if (mSerialDevice != null) {
            try {
                mSerialDevice.close();
            } catch (IOException e) {
                // Ignore.
            }
            mSerialDevice = null;
        }
    }


    private void stopIoManager() {
        if (mSerialIoManager != null) {
            Log.i(TAG, "Stopping io manager ..");
            mSerialIoManager.stop();
            mSerialIoManager = null;
        }
    }

    private void startIoManager() {
        if (mSerialDevice != null) {
            Log.i(TAG, "Starting io manager ..");
            mSerialIoManager = new SerialInputOutputManager(mSerialDevice, mListener);
            mExecutor.submit(mSerialIoManager);
            Toast.makeText(this, "startIoManager", Toast.LENGTH_SHORT).show();

        }
    }

    private void onDeviceStateChange() {
        stopIoManager();
        startIoManager();
    }

    public void onQualityRequest() {
        String previewSizesList = settings.getString(ExtraKey.PREVIEW_SIZE_SET, "");
        previewSizesList = Command.QUALITY_LIST + previewSizesList;
        Log.d(LOG_TAG, "onQualityRequest: " + previewSizesList);
        connectionManager.sendPreviewSizes(previewSizesList);
    }


    @Override
    public void onControllerConnected() {
        connectionManager.sendCommand(Command.ACCEPT_CONNECTION);
        onQualityRequest();
        subscriptionOrientation = RXBusBuilder.create(OrientationValue.class)
                .subscribe(new Action1<OrientationValue>() {
                    @Override
                    public void call(OrientationValue s) {
//                        Log.d(TAG, "orientation" + s.getValue().toString() + " " + gson.toJson(s));
                        connectionManager.sendCommand(Command.ORIENTATION + gson.toJson(s));
                    }
                });
        subscriptionLocation = RXBusBuilder.create(Location.class)
                .subscribe(new Action1<Location>() {
                    @Override
                    public void call(Location s) {
                        Log.d(TAG, "location" + gson.toJson(s));
                        connectionManager.sendCommand(Command.LOCATION + gson.toJson(s));
                    }
                });
        subscriptionOrientation = RXBusBuilder.create(String.class)
                .subscribe(new Action1<String>() {
                    @Override
                    public void call(String s) {
//                        Log.d(TAG, "orientation" + s.getValue().toString() + " " + gson.toJson(s));
                        onMoveCommandIncoming(s);
                    }
                });
    }

    private void startServices() {
        startService(new Intent(this, SensorService.class));
        startService(new Intent(this, LocationService.class));
    }
    @Override
    public void onWrongPassword() {
        connectionManager.sendCommand(Command.WRONG_PASSWORD);
        connectionManager.restart();
    }

    @Override
    public void onControllerDisconnected() {
        showToast(getString(R.string.connection_down));
    }

    @Override
    public void onControllerClosed() {
        setConnected(false);
    }

    @Override
    public void onStopPreview() {
        setConnected(false);
    }

    private void setConnected(boolean value) {
        connected = value;
    }

    @Override
    public void onFlashCommand(String command) {
        if (cameraManager.isFlashAvailable()) {
            if (command.equals(Command.LED_ON)) {
                cameraManager.requestFlashOn();
            } else if (command.equals(Command.LED_OFF)) {
                cameraManager.requestFlashOff();
            }
        } else {
            connectionManager.sendCommand(Command.FLASH_UNAVAILABLE);
        }
    }

    @Override
    public void onRequestTakePicture() {
        double currentTimeSeconds = System.currentTimeMillis();
        if (currentTimeSeconds - lastPictureTakenTime > TAKE_PICTURE_COOLDOWN) {
            lastPictureTakenTime = (int) currentTimeSeconds;
            cameraManager.requestTakePicture();
        }
    }

    @Override
    public void onRequestAutoFocus() {
        cameraManager.requestAutoFocus();
    }

    @Override
    public void onMoveForwardCommand(int movementSpeed) {
        btnMoveForward.setPressed(true);
        directionState = DirectionState.UP;
        updateMovementSpeed(movementSpeed);
    }

    @Override
    public void onMoveForwardRightCommand(int movementSpeed) {
        btnMoveForwardRight.setPressed(true);
        directionState = DirectionState.UPRIGHT;
        updateMovementSpeed(movementSpeed);
    }

    @Override
    public void onMoveForwardLeftCommand(int movementSpeed) {
        btnMoveForwardLeft.setPressed(true);
        directionState = DirectionState.UPLEFT;
        updateMovementSpeed(movementSpeed);
    }

    @Override
    public void onMoveBackwardCommand(int movementSpeed) {
        btnMoveDown.setPressed(true);
        directionState = DirectionState.DOWN;
        updateMovementSpeed(movementSpeed);
    }

    @Override
    public void onMoveBackwardRightCommand(int movementSpeed) {
        btnMoveDownRight.setPressed(true);
        directionState = DirectionState.DOWNRIGHT;
        updateMovementSpeed(movementSpeed);
    }

    @Override
    public void onMoveBackwardLeftCommand(int movementSpeed) {
        btnMoveDownLeft.setPressed(true);
        directionState = DirectionState.DOWNLEFT;
        updateMovementSpeed(movementSpeed);
    }

    @Override
    public void onMoveLeftCommand(int movementSpeed) {
        btnMoveLeft.setPressed(true);
        directionState = DirectionState.LEFT;
        updateMovementSpeed(movementSpeed);
    }

    @Override
    public void onMoveRightCommand(int movementSpeed) {
        btnMoveRight.setPressed(true);
        directionState = DirectionState.RIGHT;
        updateMovementSpeed(movementSpeed);
    }

    @Override
    public void onMoveStopCommand() {
        directionState = DirectionState.STOP;
        updateMovementSpeed(0);
    }

    @Override
    public void onSendCommandSuccess() {
    }

    @Override
    public void onSendCommandFailure() {
        connected = false;
    }

    @Override
    public void onSendPreviewSizesSuccess() {
        connected = true;
    }


    @Override
    public void onSendTelemetry() {

    }

    @Override
    public void onSendTelemetryFailure() {

    }

    @SuppressWarnings("deprecation")
    public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
        if (surfacePreview == null)
            return;

        cameraManager.stopCameraPreview();
        cameraManager.initCameraParameter();

        setupPreviewLayout();

        cameraManager.setCameraOrientation(orientationManager.getOrientation());
        cameraManager.startCameraPreview(surfacePreview);
    }

    @SuppressWarnings("deprecation")
    public void setupPreviewLayout() {
        Display display = getWindowManager().getDefaultDisplay();
        LayoutParams lp = layoutParent.getLayoutParams();

        float previewWidth = cameraManager.getPreviewSize().width;
        float previewHeight = cameraManager.getPreviewSize().height;

        int orientation = orientationManager.getOrientation();
        float ratio = 0;
        if (orientation == OrientationManager.LANDSCAPE_NORMAL
                || orientation == OrientationManager.LANDSCAPE_REVERSE) {
            ratio = previewWidth / previewHeight;
        } else if (orientation == OrientationManager.PORTRAIT_NORMAL
                || orientation == OrientationManager.PORTRAIT_REVERSE) {
            ratio = previewHeight / previewWidth;
        }
        if ((int) ((float) surfacePreview.getWidth() / ratio) >= display.getHeight()) {
            lp.height = (int) ((float) surfacePreview.getWidth() / ratio);
            lp.width = surfacePreview.getWidth();
        } else {
            lp.height = surfacePreview.getHeight();
            lp.width = (int) ((float) surfacePreview.getHeight() * ratio);
        }

        layoutParent.setLayoutParams(lp);
        int locationX = (int) (lp.width / 2.0 - surfacePreview.getWidth() / 2.0);
        layoutParent.animate().translationX(-locationX);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        cameraManager.createCameraInstance(holder);
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        cameraManager.destroyCameraInstance();
    }

    public void restartPreview(int newPreviewSize) {
        if (surfacePreview == null)
            return;

        cameraManager.stopCameraPreview();
        surfaceDestroyed(surfacePreview.getHolder());
        cameraManager = new CameraManager(newPreviewSize);
        surfaceCreated(surfacePreview.getHolder());
        cameraManager.setCameraManagerListener(this);
        cameraManager.initCameraParameter();

        setupPreviewLayout();

        cameraManager.setCameraOrientation(orientationManager.getOrientation());
        cameraManager.startCameraPreview(surfacePreview);
    }

    @Override
    public void onPictureTaken(String filename, String path) {
        connectionManager.sendCommand(Command.SNAP);
    }

    @Override
    public void onPreviewTaken(Bitmap bitmap) {
        if (connected) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, imageQuality, bos);
            connectionManager.sendImageData(bos.toByteArray());
        }
    }

    @Override
    public void onPreviewOutOfMemory(OutOfMemoryError e) {
        e.printStackTrace();
        showToast(getString(R.string.out_of_memory));
        finish();
    }

    @Override
    public void onBackPressed() {
        connectionManager.stop();
        connectionManager.onControllerClosed();
        cameraManager.stopCameraPreview();
        super.onBackPressed();
    }

    public void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
}
