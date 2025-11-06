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

import java.util.List;

public class AddEditExpenseViewModel extends AndroidViewModel {

    private MoneyWiseRepository mRepository;
    private LiveData<List<Category>> mAllCategories;


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
        String currentUserId = "USER_ID_TAM_THOI";
        mAllCategories = mRepository.getAllCategories(currentUserId);
    }

    /**
     * Cung cấp danh sách Danh mục (Categories) cho Spinner
     */
    public LiveData<List<Category>> getAllCategories() {
        return mAllCategories;
    }

    // Chúng ta sẽ không đặt hàm insert/update ở đây,
    // mà sẽ gửi dữ liệu về cho ExpenseViewModel xử lý
    // để giữ logic tập trung ở một nơi.


}