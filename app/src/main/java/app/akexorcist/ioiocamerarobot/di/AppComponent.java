package app.akexorcist.ioiocamerarobot.di;

import android.content.Context;
import android.content.res.Resources;

import com.getwandup.rxsensor.RxSensor;
import com.github.ivbaranov.rxbluetooth.RxBluetooth;
import com.google.gson.Gson;

import java.util.Calendar;

import javax.inject.Singleton;

import app.akexorcist.ioiocamerarobot.controller.ConnectionManager;
import app.akexorcist.ioiocamerarobot.di.modules.AppModule;
import app.akexorcist.ioiocamerarobot.di.modules.ServiceModule;
import app.akexorcist.ioiocamerarobot.ioio.IOIOControllerActivity;
import app.akexorcist.ioiocamerarobot.ioio.IOIOSetupActivity;
import app.akexorcist.ioiocamerarobot.service.BluetoothService;
import app.akexorcist.ioiocamerarobot.service.SensorService;
import dagger.Component;

/**
 * Created by OldMan on 07.11.2016.
 */
@Singleton
@Component(modules = {AppModule.class, ServiceModule.class})
public interface AppComponent {
    Context getContext();
    Resources getResources();
    Calendar getCalendar();
    RxSensor provideRxSensor();
    RxBluetooth provideRxBluetooth();
    Gson provideGson();


    void inject(IOIOControllerActivity activity);
    void inject(IOIOSetupActivity activity);
    void inject(SensorService sensorService);
    void inject(BluetoothService bluetoothService);
    void inject(ConnectionManager connectionManager);

//    void inject(MapsActivity activity);
//    void inject(SensorService sensorService);

}
