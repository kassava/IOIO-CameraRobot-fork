package app.akexorcist.ioiocamerarobot.service;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.michaelflisar.rxbus.RXBus;

import fr.quentinklein.slt.LocationTracker;
import fr.quentinklein.slt.TrackerSettings;


public class LocationService extends Service {

    private String TAG = LocationService.class.getSimpleName();
    private LocationTracker tracker;


    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startTracker();
        return START_STICKY;
    }

    private void startTracker(){
        tracker = new LocationTracker(
                this,
                new TrackerSettings()
                        .setUseGPS(false)
                        .setUseNetwork(true)
                        .setUsePassive(true)
                        .setTimeBetweenUpdates(30 * 60 * 1000)
                        .setMetersBetweenUpdates(0)
        )
        {

            @Override
            public void onLocationFound(@NonNull Location location) {
                // Do some stuff when a new GPS Location has been found
                Log.d(TAG, location.getProvider()+ location.getLongitude());
                RXBus.get().sendEvent(location);
            }

            @Override
            public void onTimeout() {
            }
        };
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        tracker.startListening();

    }
    @Override
    public void onDestroy() {
     if(tracker != null){
         tracker.stopListening();
     }
        super.onDestroy();
    }
}

