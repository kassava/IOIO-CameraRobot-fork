package app.akexorcist.ioiocamerarobot.service;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.github.ivbaranov.rxbluetooth.RxBluetooth;

import java.io.IOException;

import javax.inject.Inject;

import app.akexorcist.ioiocamerarobot.constant.AppConstants;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class BluetoothService extends Service {
    @Inject
    RxBluetooth rxBluetooth;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private String TAG = BluetoothService.class.getSimpleName();
    private BluetoothSocket btSocket = null;


    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "BluetoothService started!");
        rxBluetooth = new RxBluetooth(this);

        if (!rxBluetooth.isBluetoothAvailable()) {
            // handle the lack of bluetooth support
            Log.d(TAG, "Bluetooth is not supported!");
            Toast.makeText(this, "Bluetooth is not supported!", Toast.LENGTH_LONG).show();
        } else {
            // check if bluetooth is currently enabled and ready for use
            if (!rxBluetooth.isBluetoothEnabled()) {
                Log.d(TAG, "Bluetooth should be enabled first!");
                Toast.makeText(this, "Bluetooth should be enabled first!", Toast.LENGTH_LONG).show();
            } else {
                compositeDisposable.add(rxBluetooth.observeDevices()
                        .observeOn(Schedulers.computation())
                        .subscribeOn(Schedulers.computation())
                        .subscribe(new Consumer<BluetoothDevice>() {
                            @Override
                            public void accept(BluetoothDevice bluetoothDevice) throws IOException {
                                Log.d(TAG, "Device found: " + bluetoothDevice.getAddress()
                                        + " - " + bluetoothDevice.getName());
                                if (AppConstants.ADDRESS_BT.equals(bluetoothDevice.getAddress())) {
                                    rxBluetooth.observeConnectDevice(bluetoothDevice, AppConstants.MY_UUID)
                                            .subscribe(new Consumer<BluetoothSocket>() {
                                                @Override
                                                public void accept(BluetoothSocket socket) throws Exception {
                                                    // Connected to the device, do anything with the socket
                                                    Log.d(TAG, "Device connected ");

                                                }
                                            }, new Consumer<Throwable>() {
                                                @Override
                                                public void accept(Throwable throwable) throws Exception {
                                                    // Error occured
                                                    Log.d(TAG, "Bluetooth throwable!" + throwable);

                                                }
                                            });
//                                    btSocket = bluetoothDevice.createRfcommSocketToServiceRecord(Constants.MY_UUID);
//                                    rxBluetooth.cancelDiscovery();
//                                    btSocket.connect();
//                                    Log.d(TAG, "Device connected ");
                                }
                            }
                        }));
                rxBluetooth.startDiscovery();
            }
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (rxBluetooth != null) {
            rxBluetooth.cancelDiscovery();
        }
        compositeDisposable.clear();
        super.onDestroy();
    }
}
