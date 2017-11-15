package app.akexorcist.ioiocamerarobot;

import android.app.Application;

import app.akexorcist.ioiocamerarobot.di.AppComponent;
import app.akexorcist.ioiocamerarobot.di.DaggerAppComponent;
import app.akexorcist.ioiocamerarobot.di.modules.AppModule;


/**
 * Created by OldMan on 11.11.2017.
 */

public class App extends Application {
    private static AppComponent appComponent;
    private static App context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
        setAppComponent();
    }


    public static AppComponent getAppComponent() {
        return appComponent;
    }

    private void setAppComponent() {
        if (appComponent == null) {
            buildAppComponent();
        }
    }

    private static void buildAppComponent() {
        appComponent = DaggerAppComponent.builder()
                .appModule(new AppModule(context))
                .build();

    }

}