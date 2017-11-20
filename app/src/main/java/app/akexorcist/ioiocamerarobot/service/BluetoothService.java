package app.akexorcist.ioiocamerarobot.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.util.List;

import app.akexorcist.ioiocamerarobot.constant.AppConstants;
import io.palaima.smoothbluetooth.Device;
import io.palaima.smoothbluetooth.SmoothBluetooth;
import io.reactivex.disposables.CompositeDisposable;
import rx.Subscription;

public class BluetoothService extends Service implements SmoothBluetooth.Listener {
    private SmoothBluetooth mSmoothBluetooth;
    public static final int ENABLE_BT__REQUEST = 1;

    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private String TAG = BluetoothService.class.getSimpleName();
    private Subscription subscriptionCommandToArduino;

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "BluetoothService started!");
        mSmoothBluetooth = new SmoothBluetooth(this, SmoothBluetooth.ConnectionTo.OTHER_DEVICE, SmoothBluetooth.Connection.SECURE, this);
//        mSmoothBluetooth.doDiscovery();
        mSmoothBluetooth.tryConnection();
        return START_STICKY;
    }

    private void onEventRxBus() {
//        subscriptionCommandToArduino = RXBusBuilder.create(String.class)
//                .subscribe(new Action1<String>() {
//                    @Override
//                    public void call(String s) {
//                        Log.d(TAG, "bluetoothConnection: " + mSmoothBluetooth.isConnected());
//                        if (mSmoothBluetooth.isConnected()) {
//                            Log.d(TAG, "bluetoothConnection send: " + s);
//                            mSmoothBluetooth.send(s);
//                        }
//                    }
//                });
    }


    @Override
    public void onDestroy() {
        if (mSmoothBluetooth != null)
            mSmoothBluetooth.stop();
        if (subscriptionCommandToArduino != null)
            subscriptionCommandToArduino.unsubscribe();
        compositeDisposable.clear();
        super.onDestroy();
    }

    @Override
    public void onBluetoothNotSupported() {
        Log.d(TAG, "onBluetoothNotSupported ");
    }

    @Override
    public void onBluetoothNotEnabled() {
//        Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//        startActivityForResult(enableBluetooth, ENABLE_BT__REQUEST);
        Log.d(TAG, "onBluetoothNotEnabled ");
    }

    @Override
    public void onConnecting(Device device) {
        Toast.makeText(this, "onConnecting" + device.getAddress(), Toast.LENGTH_SHORT).show();
        Log.d(TAG, "onConnecting " + device);

    }

    @Override
    public void onConnected(Device device) {
        Toast.makeText(this, "onConnected" + device.getAddress(), Toast.LENGTH_SHORT).show();
        Log.d(TAG, "onConnected " + device);
        onEventRxBus();

    }

    @Override
    public void onDisconnected() {
        Log.d(TAG, "onDisconnected ");

    }

    @Override
    public void onConnectionFailed(Device device) {
        Toast.makeText(this, "onConnectionFailed" + device.getAddress(), Toast.LENGTH_SHORT).show();
        Log.d(TAG, "onConnectionFailed " + device);
        if (device.isPaired()) {
            mSmoothBluetooth.doDiscovery();
        }

    }

    @Override
    public void onDiscoveryStarted() {
        Log.d(TAG, "onDiscoveryStarted ");

    }

    @Override
    public void onDiscoveryFinished() {
        Log.d(TAG, "onDiscoveryFinished ");

    }

    @Override
    public void onNoDevicesFound() {
        Log.d(TAG, "onNoDevicesFound ");

    }

    @Override
    public void onDevicesFound(final List<Device> deviceList, final SmoothBluetooth.ConnectionCallback connectionCallback) {
        Log.d(TAG, "onDevicesFound  " + deviceList.get(0).getAddress() + deviceList.get(0).isPaired());
        Device device = new Device(AppConstants.NAME, AppConstants.ADDRESS_BT, true);
        connectionCallback.connectTo(device);
    }

    @Override
    public void onDataReceived(int data) {
        Log.d(TAG, "onDataReceived " + data);
    }
}
