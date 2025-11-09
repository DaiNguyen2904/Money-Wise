package com.example.moneywise;

import android.app.Application;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.moneywise.sync.SyncWorker;

import java.util.concurrent.TimeUnit;

// Bạn phải kế thừa (extends) từ Application
public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
    }

}