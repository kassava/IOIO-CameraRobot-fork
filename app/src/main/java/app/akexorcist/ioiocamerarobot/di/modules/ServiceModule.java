package app.akexorcist.ioiocamerarobot.di.modules;

import android.content.Context;

import com.getwandup.rxsensor.RxSensor;
import com.github.ivbaranov.rxbluetooth.RxBluetooth;

import dagger.Module;
import dagger.Provides;

/**
 * Created by OldMan on 12.11.2017.
 */

@Module
public class ServiceModule {
    private RxSensor rxSensor;
    private RxBluetooth rxBluetooth;

    @Provides
    public RxSensor provideRxSensor(Context context) {
        return new RxSensor(context);
    }
    @Provides
    public RxBluetooth provideRxBluetooth(Context context) {
        return new RxBluetooth(context);
    }
}
