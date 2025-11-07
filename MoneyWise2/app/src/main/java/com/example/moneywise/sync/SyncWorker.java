package com.example.moneywise.sync;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

// (Import các thư viện Firebase của bạn ở đây)

import com.example.moneywise.data.entity.SyncQueue;
import com.example.moneywise.repository.MoneyWiseRepository;
import com.example.moneywise.utils.SessionManager;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class SyncWorker extends Worker {

    private static final String TAG = "SyncWorker";
    private MoneyWiseRepository mRepository;
    private SessionManager mSessionManager; // Cần để lấy User ID
    private FirebaseFirestore mFirestore; // Cần để truy cập Firebase

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        // Khởi tạo Repository để truy cập CSDL
        mRepository = new MoneyWiseRepository((Application) getApplicationContext());
        mSessionManager = new SessionManager(getApplicationContext());
        mFirestore = FirebaseFirestore.getInstance();
    }

    /**
     * Đây là hàm chính sẽ chạy ở luồng nền (background thread)
     */
    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "SyncWorker bắt đầu chạy...");

        // Lấy User ID thật
        String userId = mSessionManager.getUserId();
        if (userId == null) {
            Log.w(TAG, "SyncWorker: Không có user ID, không thể đồng bộ.");
            return Result.failure(); // Thất bại nếu không có user
        }

        try {
            List<SyncQueue> pendingItems = mRepository.getPendingSyncItems();
            if (pendingItems.isEmpty()) {
                Log.d(TAG, "Không có gì để đồng bộ.");
                return Result.success();
            }

            Log.d(TAG, "Tìm thấy " + pendingItems.size() + " mục cần đồng bộ cho user: " + userId);

            for (SyncQueue item : pendingItems) {
                // Gọi hàm đồng bộ thật
                boolean isSuccess = pushToFirebase(item, userId);

                if (isSuccess) {
                    // 3. Nếu thành công: Xóa mục khỏi hàng đợi
                    mRepository.deleteSyncItem(item);
                    Log.d(TAG, "Đồng bộ thành công: " + item.recordId);
                } else {
                    // 4. Nếu thất bại: Tăng số lần thử lại
                    item.retryCount = item.retryCount + 1;
                    mRepository.updateSyncItem(item);
                    Log.w(TAG, "Đồng bộ thất bại, thử lại sau: " + item.recordId);
                }
            }

            Log.d(TAG, "SyncWorker hoàn thành.");
            return Result.success();

        } catch (Exception e) {
            Log.e(TAG, "SyncWorker gặp lỗi nghiêm trọng: ", e);
            return Result.failure(); // Báo lỗi
        }
    }

    /**
     * Hàm ĐÃ HOÀN THIỆN: Đẩy dữ liệu lên Firebase (chạy đồng bộ)
     */
    private boolean pushToFirebase(SyncQueue item, String userId) {

        // 1. Xây dựng đường dẫn (Path)
        // Ví dụ: users/{userId}/expenses/{expenseId}
        DocumentReference docRef = mFirestore
                .collection("users")
                .document(userId)
                .collection(item.tableName.toLowerCase())
                .document(item.recordId);

        try {
            switch (item.action) {
                case CREATE:
                case UPDATE:
                    // 2. Lấy dữ liệu đầy đủ từ CSDL Room
                    Object data = null;
                    switch (item.tableName) {
                        case "EXPENSES":
                            data = mRepository.getExpenseById_Sync(item.recordId);
                            break;
                        case "CATEGORIES":
                            data = mRepository.getCategoryById_Sync(item.recordId);
                            break;
                        case "BUDGETS":
                            data = mRepository.getBudgetById_Sync(item.recordId);
                            break;
                    }

                    if (data != null) {
                        // 3. Đẩy dữ liệu lên và CHỜ (await)
                        Log.d(TAG, "Đang đẩy (Set): " + docRef.getPath());
                        Tasks.await(docRef.set(data)); // Ghi đè (Set)
                    }
                    break;

                case DELETE:
                    // 3. Gửi lệnh xóa và CHỜ (await)
                    Log.d(TAG, "Đang đẩy (Delete): " + docRef.getPath());
                    Tasks.await(docRef.delete());
                    break;
            }

            return true; // Thành công

        } catch (Exception e) {
            // Ví dụ: Mất kết nối, Firebase lỗi quyền...
            Log.e(TAG, "Lỗi khi đẩy lên Firebase: " + item.recordId, e);
            return false; // Thất bại
        }
    }
}