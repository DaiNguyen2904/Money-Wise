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

import java.util.List;

public class SyncWorker extends Worker {

    private static final String TAG = "SyncWorker";
    private MoneyWiseRepository mRepository;

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        // Khởi tạo Repository để truy cập CSDL
        mRepository = new MoneyWiseRepository((Application) getApplicationContext());
    }

    /**
     * Đây là hàm chính sẽ chạy ở luồng nền (background thread)
     */
    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "SyncWorker bắt đầu chạy...");

        try {
            // 1. Lấy tất cả các mục đang chờ (theo thứ tự FIFO)
            List<SyncQueue> pendingItems = mRepository.getPendingSyncItems();

            if (pendingItems.isEmpty()) {
                Log.d(TAG, "Không có gì để đồng bộ.");
                return Result.success();
            }

            Log.d(TAG, "Tìm thấy " + pendingItems.size() + " mục cần đồng bộ.");

            // 2. Lặp qua từng mục và đẩy lên Firebase
            for (SyncQueue item : pendingItems) {
                boolean isSuccess = pushToFirebase(item);

                if (isSuccess) {
                    // 3. Nếu thành công: Xóa mục khỏi hàng đợi
                    mRepository.deleteSyncItem(item);
                    Log.d(TAG, "Đồng bộ thành công: " + item.recordId);
                } else {
                    // 4. Nếu thất bại: Tăng số lần thử lại
                    item.retryCount = item.retryCount + 1;
                    mRepository.updateSyncItem(item);
                    Log.w(TAG, "Đồng bộ thất bại, thử lại sau: " + item.recordId);

                    // (Bạn có thể thêm logic để không thử lại mãi mãi,
                    // ví dụ: nếu retryCount > 5 thì báo lỗi)
                }
            }

            Log.d(TAG, "SyncWorker hoàn thành.");
            return Result.success(); // Hoàn thành (cho dù có lỗi hay không)

        } catch (Exception e) {
            Log.e(TAG, "SyncWorker gặp lỗi nghiêm trọng: ", e);
            return Result.failure(); // Báo lỗi
        }
    }

    /**
     * Hàm GIẢ LẬP đẩy dữ liệu lên Firebase
     * (Bạn sẽ thay thế hàm này bằng logic Firebase thật)
     */
    /**
     * Hàm ĐÃ HOÀN THIỆN (về mặt logic) để đẩy dữ liệu
     */
    private boolean pushToFirebase(SyncQueue item) {
        // 1. Lấy dữ liệu đầy đủ từ CSDL
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

        // 2. Xử lý hành động
        try {
            // TODO: Khởi tạo Firebase Firestore của bạn ở đây
            // FirebaseFirestore db = FirebaseFirestore.getInstance();

            String collectionPath = item.tableName.toLowerCase(); // "expenses", "categories"
            String documentId = item.recordId;

            switch (item.action) {
                case CREATE:
                case UPDATE:
                    // Nếu dữ liệu vẫn tồn tại ở cục bộ (chưa bị xóa)
                    if (data != null) {
                        // TODO: Gọi hàm .set() hoặc .update() của Firebase
                        // db.collection(collectionPath).document(documentId).set(data).await();
                        Log.d(TAG, "Đẩy (CREATE/UPDATE) lên Firebase: " + documentId);
                    } else {
                        // Bản ghi đã bị xóa cục bộ trước khi kịp đồng bộ
                        Log.w(TAG, "Bản ghi không tồn tại để CREATE/UPDATE: " + documentId);
                    }
                    break;

                case DELETE:
                    // TODO: Gọi hàm .delete() của Firebase
                    // db.collection(collectionPath).document(documentId).delete().await();
                    Log.d(TAG, "Đẩy (DELETE) lên Firebase: " + documentId);
                    break;
            }

            // Giả lập độ trễ mạng (bạn có thể xóa dòng này)
            Thread.sleep(500);

            return true; // Thành công

        } catch (Exception e) {
            // Ví dụ: Mất kết nối, Firebase lỗi quyền...
            Log.e(TAG, "Lỗi khi đẩy lên Firebase: " + item.recordId, e);
            return false; // Thất bại
        }
    }
}