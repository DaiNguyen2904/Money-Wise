package com.example.moneywise.ui.expenses;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;


import com.example.moneywise.data.AppDatabase;
import com.example.moneywise.data.entity.Category;
import com.example.moneywise.data.entity.Expense;
import com.example.moneywise.data.model.ExpenseWithCategory;
import com.example.moneywise.repository.MoneyWiseRepository;
import com.example.moneywise.utils.SessionManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ViewModel cho tính năng Chi tiêu (Expenses).
 * Nó lấy Application làm tham số để có thể khởi tạo Repository.
 */
public class ExpenseViewModel extends AndroidViewModel {

    // 1. Tham chiếu đến Repository
    private MoneyWiseRepository mRepository;

    // 2. Tham chiếu LiveData đến danh sách chi tiêu
    // Chúng ta dùng 1 biến LiveData có thể thay đổi để lọc (sẽ giải thích sau)
    private LiveData<List<Expense>> mAllExpensesRepo;
    private LiveData<List<Category>> mAllCategoriesRepo;

    // 2. LiveData cho truy vấn tìm kiếm
    private MutableLiveData<String> mSearchQuery = new MutableLiveData<>(""); // Mặc định là chuỗi rỗng

    // 3. LiveData cuối cùng để Fragment theo dõi
    private MediatorLiveData<List<ExpenseWithCategory>> mFilteredExpenses = new MediatorLiveData<>();

    private String currentUserId;

    private boolean isInitialized = false; // Cờ để tránh khởi tạo lại

    // (Bạn cũng có thể thêm LiveData cho Categories, Budgets nếu màn hình này cần)

    // 3. Constructor
    public ExpenseViewModel(@NonNull Application application) {
        super(application);
        mRepository = new MoneyWiseRepository(application);

        // Giả sử chúng ta có một ID người dùng cố định (bạn sẽ thay thế bằng ID thật)
//        SessionManager sessionManager = new SessionManager(application);
//        currentUserId = sessionManager.getUserId(); // Lấy ID đã lưu
//        mAllExpensesRepo = mRepository.getAllExpenses(currentUserId);
//        mAllCategoriesRepo = mRepository.getAllCategories(currentUserId); // Cần lấy danh mục
//
//        // Thêm các nguồn vào MediatorLiveData
//        mFilteredExpenses.addSource(mAllExpensesRepo, expenses -> combineAndFilter());
//        mFilteredExpenses.addSource(mAllCategoriesRepo, categories -> combineAndFilter());
        mFilteredExpenses.addSource(mSearchQuery, query -> combineAndFilter());


    }

    /**
     * HÀM MỚI: Fragment sẽ gọi hàm này
     */
    public void init() {
        if (isInitialized) {
            return; // Đã khởi tạo rồi, không làm gì
        }

        SessionManager sessionManager = new SessionManager(getApplication());
        currentUserId = sessionManager.getUserId(); // Lấy ID đã lưu

        if (currentUserId == null) {
            return; // Chưa có user, không thể truy vấn
        }

        // Khởi tạo các nguồn LiveData
        mAllExpensesRepo = mRepository.getAllExpenses(currentUserId);
        mAllCategoriesRepo = mRepository.getAllCategories(currentUserId);

        // Thêm các nguồn vào MediatorLiveData
        mFilteredExpenses.addSource(mAllExpensesRepo, expenses -> combineAndFilter());
        mFilteredExpenses.addSource(mAllCategoriesRepo, categories -> combineAndFilter());

        isInitialized = true; // Đánh dấu đã khởi tạo
    }

    /**
     * Hàm chính để xử lý:
     * 1. Lấy 2 danh sách (Expenses, Categories)
     * 2. Lấy truy vấn tìm kiếm (Query)
     * 3. Kết hợp (join) Expenses và Categories
     * 4. Lọc (filter) theo Query
     * 5. Đẩy (post) danh sách cuối cùng
     */
    private void combineAndFilter() {
        if (mAllExpensesRepo == null || mAllCategoriesRepo == null) {
            return; // Chưa được init()
        }

        List<Expense> expenses = mAllExpensesRepo.getValue();
        List<Category> categories = mAllCategoriesRepo.getValue();
        String query = mSearchQuery.getValue();

        if (expenses == null || categories == null || query == null) {
            return; // Chưa có đủ dữ liệu, đợi
        }

        // Tối ưu hóa việc tìm kiếm: Chuyển danh sách Categories thành Map
        Map<String, Category> categoryMap = new HashMap<>();
        for (Category cat : categories) {
            categoryMap.put(cat.getCategoryId(), cat);
        }

        // Tạo danh sách mới (đã kết hợp)
        List<ExpenseWithCategory> combinedList = new ArrayList<>();
        for (Expense exp : expenses) {
            Category cat = categoryMap.get(exp.getCategoryId());
            // (Nếu cat là null, nghĩa là danh mục đã bị xóa, vẫn có thể hiển thị)
            combinedList.add(new ExpenseWithCategory(exp, cat));
        }

        // Lọc danh sách
        List<ExpenseWithCategory> filteredList;
        if (query.isEmpty()) {
            filteredList = combinedList; // Không tìm kiếm, trả về tất cả
        } else {
            filteredList = new ArrayList<>();
            String lowerCaseQuery = query.toLowerCase();

            for (ExpenseWithCategory item : combinedList) {
                boolean matchesNote = item.expense.getNote() != null &&
                        item.expense.getNote().toLowerCase().contains(lowerCaseQuery);

                boolean matchesCategory = item.category != null &&
                        item.category.getName().toLowerCase().contains(lowerCaseQuery);

                boolean matchesAmount = String.valueOf(item.expense.getAmount()).contains(lowerCaseQuery);

                if (matchesNote || matchesCategory || matchesAmount) {
                    filteredList.add(item);
                }
            }
        }

        // Đẩy kết quả cuối cùng
        mFilteredExpenses.setValue(filteredList);
    }

    /**
     * Cung cấp LiveData ĐÃ LỌC cho View
     */
    public LiveData<List<ExpenseWithCategory>> getFilteredExpenses() {
        return mFilteredExpenses;
    }

    /**
     * View gọi hàm này khi người dùng nhập vào thanh tìm kiếm
     */
    public void setSearchQuery(String query) {
        mSearchQuery.setValue(query);
    }

    // --- CÁC HÀM "GETTER" CHO VIEW ---

    /**
     * Cung cấp danh sách chi tiêu (đã được bọc LiveData) cho View.
     * View sẽ "observe" (theo dõi) hàm này.
     */
    public LiveData<List<Expense>> getAllExpenses() {
        return mAllExpensesRepo;
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
        // Gán các trường còn thiếu
        expense.setUserId(currentUserId) ;
        expense.setCreatedAt(System.currentTimeMillis()) ;
        // (Trường updatedAt có thể null khi insert)

        mRepository.insertExpense(expense);
    }

    /**
     * HÀM MỚI: Xử lý logic update an toàn
     * Lấy dữ liệu cũ, cập nhật trường mới, và lưu
     */
    public void updateExpenseDetails(String expenseId, double amount, String categoryId, long date, String note) {
        // Chạy trên luồng nền của Room
        AppDatabase.databaseWriteExecutor.execute(() -> {
            // 1. Lấy bản ghi Expense đầy đủ từ CSDL
            Expense originalExpense = mRepository.getExpenseById_Sync(expenseId);

            if (originalExpense != null) {
                // 2. Cập nhật các trường đã thay đổi
                originalExpense.setAmount(amount);
                originalExpense.setCategoryId(categoryId);
                originalExpense.setDate(date);
                originalExpense.setNote(note);
                // (userId và createdAt đã có, không đổi)

                // 3. Gọi hàm update của Repository (hàm này sẽ tự set synced=0, updatedAt)
                mRepository.updateExpense(originalExpense);
            }
        });
    }

    public void update(Expense expense) {
        mRepository.updateExpense(expense);
    }

    public void delete(Expense expense) {
        mRepository.deleteExpense(expense);
    }
}