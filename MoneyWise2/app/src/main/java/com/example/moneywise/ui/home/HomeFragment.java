package com.example.moneywise.ui.home;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.moneywise.R;
import com.example.moneywise.sync.SyncWorker;

public class HomeFragment extends Fragment {

    private WorkManager mWorkManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate layout cho fragment này
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity != null && activity.getSupportActionBar() != null) {
            ActionBar actionBar = activity.getSupportActionBar();
            actionBar.setTitle("Trang chủ"); // Đặt tiêu đề
            actionBar.setDisplayHomeAsUpEnabled(false); // HIỂN THỊ nút quay lại
        }

        // Khởi tạo WorkManager
        mWorkManager = WorkManager.getInstance(requireActivity().getApplicationContext());

        // Tìm nút Button
        Button manualSyncButton = view.findViewById(R.id.btn_manual_sync);

        // Gán sự kiện click
        manualSyncButton.setOnClickListener(v -> {
            Log.d("HomeFragment", "Nút đồng bộ thủ công được nhấn.");
            Toast.makeText(getContext(), "Đang kích hoạt đồng bộ...", Toast.LENGTH_SHORT).show();

            // Gọi hàm trigger sync (logic tương tự LoginActivity)
            triggerImmediateSync();
        });
    }

    /**
     * HÀM MỚI: Kích hoạt SyncWorker chạy 1 lần ngay lập tức
     * (Đây là logic bạn đã dùng trong LoginActivity)
     */
    private void triggerImmediateSync() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest immediateSyncRequest =
                new OneTimeWorkRequest.Builder(SyncWorker.class)
                        .setConstraints(constraints)
                        .build();

        // Dùng enqueueUniqueWork để đảm bảo không bị chồng chéo
        mWorkManager.enqueueUniqueWork(
                "MoneyWiseSyncJob_Immediate", // Tên duy nhất cho công việc 1 lần
                ExistingWorkPolicy.REPLACE, // Thay thế nếu có yêu cầu cũ
                immediateSyncRequest
        );
    }
}