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

        // Gọi hàm lên lịch đồng bộ khi ứng dụng khởi chạy
        setupPeriodicSync();
    }

    private void setupPeriodicSync() {
        // 1. Tạo ràng buộc (Chỉ chạy khi có mạng)
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        // 2. Tạo Yêu cầu chạy Định kỳ (ví dụ: 1 tiếng một lần)
        PeriodicWorkRequest syncRequest =
                new PeriodicWorkRequest.Builder(
                        SyncWorker.class,
                        1, TimeUnit.HOURS) // Lặp lại mỗi 1 giờ
                        .setConstraints(constraints)
                        .build();

        // 3. Đưa yêu cầu vào hàng đợi
        WorkManager.getInstance(getApplicationContext()).enqueueUniquePeriodicWork(
                "MoneyWiseSyncJob", // Tên duy nhất cho công việc
                ExistingPeriodicWorkPolicy.KEEP, // Nếu đã có, cứ giữ
                syncRequest
        );
    }
}