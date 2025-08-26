package com.idsiber.eye;

import android.app.Application;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;

public class IdSiberEyeApplication extends Application implements LifecycleOwner {
    private LifecycleRegistry lifecycleRegistry;

    @Override
    public void onCreate() {
        super.onCreate();
        lifecycleRegistry = new LifecycleRegistry(this);
        // Set the lifecycle to started state for CameraX
        lifecycleRegistry.setCurrentState(Lifecycle.State.STARTED);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        lifecycleRegistry.setCurrentState(Lifecycle.State.DESTROYED);
    }

    @Override
    public Lifecycle getLifecycle() {
        return lifecycleRegistry;
    }
}