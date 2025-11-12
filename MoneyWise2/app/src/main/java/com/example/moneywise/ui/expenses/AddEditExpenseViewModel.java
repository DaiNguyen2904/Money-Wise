package com.example.moneywise.ui.expenses;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;


import com.example.moneywise.data.AppDatabase;
import com.example.moneywise.data.entity.Category;
import com.example.moneywise.data.entity.Expense;
import com.example.moneywise.repository.MoneyWiseRepository;
import com.example.moneywise.utils.SessionManager;

import java.util.List;
import java.util.UUID;

public class AddEditExpenseViewModel extends AndroidViewModel {

    private MoneyWiseRepository mRepository;
    private LiveData<List<Category>> mAllCategories;

    private String currentUserId;


    // Dùng MutableLiveData để chứa Giao dịch (Expense) đang được sửa
    private MutableLiveData<Expense> mLoadedExpense = new MutableLiveData<>();

    /**
     * Cung cấp LiveData cho Activity để theo dõi Giao dịch
     */
    public LiveData<Expense> getLoadedExpense() {
        return mLoadedExpense;
    }

    /**
     * Ra lệnh cho Repository tải Giao dịch (Expense) bằng ID
     * (Chạy trên luồng nền)
     */
    public void loadExpense(String expenseId) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            Expense expense = mRepository.getExpenseById_Sync(expenseId);
            // Post giá trị lên LiveData
            mLoadedExpense.postValue(expense);
        });
    }
    public AddEditExpenseViewModel(@NonNull Application application) {
        super(application);
        mRepository = new MoneyWiseRepository(application);

        // Giả sử lấy ID người dùng (sẽ thay thế sau)
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

    /**
     * === HÀM MỚI ===
     * Tạo một danh mục mới (ví dụ: "Du lịch") với icon và màu mặc định.
     * Hàm này sẽ lưu vào CSDL (thông qua Repository) và
     * trả về đối tượng Category vừa tạo để UI sử dụng ngay.
     */
    public Category insertNewCategory(String categoryName) {
        Category newCategory = new Category();
        newCategory.setCategoryId(UUID.randomUUID().toString());
        newCategory.setUserId(currentUserId);
        newCategory.setName(categoryName);
        newCategory.setIcon("ic_other"); // Icon mặc định "ic_other"
        newCategory.setColor("#808080"); // Màu xám mặc định
        newCategory.setIsDefault(0); // 0 = không phải danh mục gốc
        newCategory.setCreatedAt(System.currentTimeMillis());
        // (Repository sẽ tự động set 'synced = 0')

        // Yêu cầu Repository lưu trữ (hành động này chạy nền)
        mRepository.insertCategory(newCategory);

        // Trả về đối tượng Category ngay lập tức cho UI
        return newCategory;
    }

}