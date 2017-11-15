package app.akexorcist.ioiocamerarobot.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import com.getwandup.rxsensor.RxSensor;
import com.getwandup.rxsensor.domain.RxSensorEvent;
import com.michaelflisar.rxbus.RXBus;

import javax.inject.Inject;

import app.akexorcist.ioiocamerarobot.model.OrientationValue;
import rx.Subscriber;

/**
 * Created by OldMan on 07.11.2016.
 */

public class SensorService extends Service {
    @Inject
    RxSensor rxAccelerometerSensor;
    @Inject
    RxSensor rxMagneticSensor;
    private volatile float[] accelerometerValues = new float[3];
    private volatile float[] magneticValues = new float[3];
    private volatile float[] orientationValues = new float[3];

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public float[] getAccelerometerValues() {
        return accelerometerValues;
    }

    public void setAccelerometerValues(float[] accelerometerValues) {
        this.accelerometerValues = accelerometerValues;
    }

    public float[] getMagneticValues() {
        return magneticValues;
    }

    public void setMagneticValues(float[] magneticValues) {
        this.magneticValues = magneticValues;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        rxAccelerometerSensor = new RxSensor(this);
        rxAccelerometerSensor.observe(Sensor.TYPE_ACCELEROMETER, SensorManager.SENSOR_DELAY_NORMAL)
                .subscribe(new Subscriber<RxSensorEvent>() {
                    @Override
                    public void onCompleted() { }

                    @Override
                    public void onError(Throwable e) { }

                    @Override
                    public void onNext(RxSensorEvent sensorEvent) {
                        setAccelerometerValues(sensorEvent.values);
                        RXBus.get().sendEvent(new OrientationValue(calculateOrientation()));
//                        eventBus.getDefault().post(new OrientationValue(calculateOrientation()));
                    }
                });
        rxMagneticSensor = new RxSensor(this);
        rxMagneticSensor.observe(Sensor.TYPE_MAGNETIC_FIELD, SensorManager.SENSOR_DELAY_NORMAL)
                .subscribe(new Subscriber<RxSensorEvent>() {
                    @Override
                    public void onCompleted() { }

                    @Override
                    public void onError(Throwable e) { }

                    @Override
                    public void onNext(RxSensorEvent sensorEvent) {
                        setMagneticValues(sensorEvent.values);
                        RXBus.get().sendEvent(new OrientationValue(calculateOrientation()));
//                        eventBus.getDefault().post(new OrientationValue(calculateOrientation()));
                    }
                });
        return START_STICKY;
    }

    private synchronized float[] calculateOrientation() {
        float[] values = new float[3];
        float[] R = new float[9];
        float[] outR = new float[9];
        int axisX = 0, axisY = 0;
        WindowManager window = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Display display = window.getDefaultDisplay();
        int mScreenRotation = display.getRotation();
        SensorManager.getRotationMatrix(R, null, getAccelerometerValues(), getMagneticValues());
        switch (mScreenRotation) {
            case Surface.ROTATION_0:
                axisX = SensorManager.AXIS_X;
                axisY = SensorManager.AXIS_Z;
                break;

            case Surface.ROTATION_90:
                axisX = SensorManager.AXIS_Z;
                axisY = SensorManager.AXIS_MINUS_X;
                break;

            case Surface.ROTATION_180:
                axisX = SensorManager.AXIS_MINUS_X;
                axisY = SensorManager.AXIS_MINUS_Y;
                break;

            case Surface.ROTATION_270:
                axisX = SensorManager.AXIS_MINUS_Z;
                axisY = SensorManager.AXIS_X;
                break;

            default:
                break;
        }
        SensorManager.remapCoordinateSystem(R, axisX, axisY, outR);
        SensorManager.getOrientation(outR, values);

        // Convert from Radians to Degrees.
        values[0] = (float) Math.toDegrees(values[0]);
        values[1] = (float) Math.toDegrees(values[1]);
        values[2] = (float) Math.toDegrees(values[2]);
        return values;
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}