package com.example.moneywise.ui.expenses;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;


import com.example.moneywise.data.entity.Expense;
import com.example.moneywise.repository.MoneyWiseRepository;
import com.example.moneywise.utils.SessionManager;

import java.util.List;

/**
 * ViewModel cho tính năng Chi tiêu (Expenses).
 * Nó lấy Application làm tham số để có thể khởi tạo Repository.
 */
public class ExpenseViewModel extends AndroidViewModel {

    // 1. Tham chiếu đến Repository
    private MoneyWiseRepository mRepository;

    // 2. Tham chiếu LiveData đến danh sách chi tiêu
    // Chúng ta dùng 1 biến LiveData có thể thay đổi để lọc (sẽ giải thích sau)
    private LiveData<List<Expense>> mAllExpenses;

    private String currentUserId;

    // (Bạn cũng có thể thêm LiveData cho Categories, Budgets nếu màn hình này cần)

    // 3. Constructor
    public ExpenseViewModel(@NonNull Application application) {
        super(application);
        mRepository = new MoneyWiseRepository(application);

        // Giả sử chúng ta có một ID người dùng cố định (bạn sẽ thay thế bằng ID thật)
        SessionManager sessionManager = new SessionManager(application);
        currentUserId = sessionManager.getUserId(); // Lấy ID đã lưu
        mAllExpenses = mRepository.getAllExpenses(currentUserId);
    }

    // --- CÁC HÀM "GETTER" CHO VIEW ---

    /**
     * Cung cấp danh sách chi tiêu (đã được bọc LiveData) cho View.
     * View sẽ "observe" (theo dõi) hàm này.
     */
    public LiveData<List<Expense>> getAllExpenses() {
        return mAllExpenses;
    }

    // (Nếu bạn cần lọc theo ngày):
    // public LiveData<List<Expense>> getExpensesByDateRange(String userId, long start, long end) {
    //     return mRepository.getExpensesByDateRange(userId, start, end);
    // }

    // --- CÁC HÀM "ACTION" TỪ VIEW ---

    /**
     * View sẽ gọi hàm này khi người dùng muốn thêm một chi tiêu mới.
     * ViewModel sẽ gọi Repository (Repository sẽ tự động chạy trên luồng nền).
     */
    public void insert(Expense expense) {
        // (Trong thực tế, bạn sẽ lấy userId và gán vào expense trước khi gọi insert)
        mRepository.insertExpense(expense);
    }

    public void update(Expense expense) {
        mRepository.updateExpense(expense);
    }

    public void delete(Expense expense) {
        mRepository.deleteExpense(expense);
    }
}