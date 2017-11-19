package app.akexorcist.ioiocamerarobot.service;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.github.ivbaranov.rxbluetooth.BluetoothConnection;
import com.github.ivbaranov.rxbluetooth.RxBluetooth;
import com.github.ivbaranov.rxbluetooth.events.ConnectionStateEvent;
import com.michaelflisar.rxbus.RXBusBuilder;

import java.io.IOException;

import javax.inject.Inject;

import app.akexorcist.ioiocamerarobot.constant.AppConstants;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import rx.Subscription;
import rx.functions.Action1;

public class BluetoothService extends Service {
    @Inject
    RxBluetooth rxBluetooth;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private String TAG = BluetoothService.class.getSimpleName();
    private BluetoothSocket btSocket = null;
    private BluetoothConnection bluetoothConnection = null;
    private Subscription subscriptionCommandToArduino;

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "BluetoothService started!");
        rxBluetooth = new RxBluetooth(this);
        onEventRxBus();
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
                onConnectDevice();
            }
        }
        return START_STICKY;
    }

    private void onConnectDevice(){
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
//                                            Toast.makeText(BluetoothService.this, "Device connected!", Toast.LENGTH_LONG).show();
                                            bluetoothConnection = new BluetoothConnection(socket);
//                                            onConnectionStateSubscriber();
                                        }
                                    }, new Consumer<Throwable>() {
                                        @Override
                                        public void accept(Throwable throwable) throws Exception {
                                            // Error occured
                                            Log.d(TAG, "Bluetooth throwable: " + throwable);
                                        }
                                    });
                        }
                    }
                }));
        rxBluetooth.startDiscovery();
    }

    private void onConnectionStateSubscriber(){
        rxBluetooth.observeConnectionState()
                .observeOn(Schedulers.computation())
                .subscribeOn(Schedulers.computation())
                .subscribe(new Consumer<ConnectionStateEvent>() {
                    @Override public void accept(ConnectionStateEvent event) throws Exception {
                        switch (event.getState()) {
                            case BluetoothAdapter.STATE_DISCONNECTED:
                                // device disconnected
                                Log.d(TAG, "Bluetooth tSTATE_DISCONNECTED!");
//                                onConnectDevice();
                                break;
                            case BluetoothAdapter.STATE_CONNECTING:
                                // device connecting
                                Log.d(TAG, "Bluetooth STATE_CONNECTING!");
                                break;
                            case BluetoothAdapter.STATE_CONNECTED:
                                // device connected
                                Log.d(TAG, "Bluetooth STATE_CONNECTED!");
//                                onReadMessageFromArduino();
                                break;
                            case BluetoothAdapter.STATE_DISCONNECTING:
                                // device disconnecting
                                Log.d(TAG, "Bluetooth STATE_DISCONNECTING!");
//                                onConnectDevice();
                                break;
                        }
                    }
                });
    }

    private void onEventRxBus() {
        subscriptionCommandToArduino = RXBusBuilder.create(String.class)
                .subscribe(new Action1<String>() {
                    @Override
                    public void call(String s) {
                        Log.d(TAG, "bluetoothConnection: " + bluetoothConnection);
                        if (bluetoothConnection != null) {
                            Log.d(TAG, "bluetoothConnection send: " + s);
                            bluetoothConnection.send(s);
                        }
                    }
                });
    }

    private void onReadMessageFromArduino() {
        // Or just observe string
        if (bluetoothConnection != null)
            bluetoothConnection.observeStringStream()
                    .observeOn(Schedulers.computation())
                    .subscribeOn(Schedulers.io())
                    .subscribe(new Consumer<String>() {
                        @Override
                        public void accept(String s) throws Exception {
                            Log.d(TAG, "onReadMessageFromArduino: " + s);
                        }

                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable throwable) throws Exception {
                            Log.d(TAG, "onReadMessageFromArduino + throwable: " + throwable);

                        }
                    });
    }

    @Override
    public void onDestroy() {
        if (rxBluetooth != null) {
            rxBluetooth.cancelDiscovery();
        }
        if (subscriptionCommandToArduino != null)
            subscriptionCommandToArduino.unsubscribe();
        compositeDisposable.clear();
        super.onDestroy();
    }
}