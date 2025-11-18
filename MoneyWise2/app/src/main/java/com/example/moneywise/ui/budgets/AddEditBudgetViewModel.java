package com.example.moneywise.ui.budgets;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;


import com.example.moneywise.data.AppDatabase;
import com.example.moneywise.data.entity.Budget;
import com.example.moneywise.data.entity.Category;
import com.example.moneywise.repository.MoneyWiseRepository;
import com.example.moneywise.utils.SessionManager;

import java.util.List;

public class AddEditBudgetViewModel extends AndroidViewModel {

    private MoneyWiseRepository mRepository;
    private LiveData<List<Category>> mAllCategories;

    private String currentUserId;

    // Interface callback để trả kết quả kiểm tra
    public interface OnDuplicateCheckListener {
        void onResult(boolean isDuplicate);
    }

    // Dùng MutableLiveData để chứa Budget đang được sửa

    private MutableLiveData<Budget> mLoadedBudget = new MutableLiveData<>();

    public LiveData<Budget> getLoadedBudget() {
        return mLoadedBudget;
    }

    public void loadBudget(String budgetId) {
        // (Chúng ta sẽ cần hàm _Sync trong Repository)
        AppDatabase.databaseWriteExecutor.execute(() -> {
            Budget budget = mRepository.getBudgetById_Sync(budgetId);
            mLoadedBudget.postValue(budget);
        });
    }

    public AddEditBudgetViewModel(@NonNull Application application) {
        super(application);
        mRepository = new MoneyWiseRepository(application);

        SessionManager sessionManager = new SessionManager(application);
        currentUserId = sessionManager.getUserId(); // Lấy ID đã lưu
        mAllCategories = mRepository.getAllCategories(currentUserId);
    }

    /**
     * Cung cấp danh sách Danh mục (Categories) cho Spinner
     */
    public LiveData<List<Category>> getAllCategories() {
        return mAllCategories;
    }

    // --- HÀM MỚI: Kiểm tra trùng lặp ---
    public void checkDuplicateBudget(String categoryId, String currentBudgetId, OnDuplicateCheckListener listener) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            // Tìm xem có ngân sách nào dùng categoryId này chưa
            Budget existingBudget = mRepository.getBudgetByCategory_Sync(currentUserId, categoryId);

            boolean isDuplicate = false;
            if (existingBudget != null) {
                // Nếu đang ở chế độ THÊM: Cứ tồn tại là trùng
                if (currentBudgetId == null) {
                    isDuplicate = true;
                }
                // Nếu đang ở chế độ SỬA: Trùng nếu ID khác nhau (nghĩa là trùng với ngân sách KHÁC)
                else if (!existingBudget.getBudgetId().equals(currentBudgetId)) {
                    isDuplicate = true;
                }
            }

            // Trả kết quả về (cần post về luồng UI nếu muốn cập nhật UI ngay,
            // nhưng callback này sẽ được xử lý trong Activity nên ta cứ trả về)
            listener.onResult(isDuplicate);
        });
    }
}