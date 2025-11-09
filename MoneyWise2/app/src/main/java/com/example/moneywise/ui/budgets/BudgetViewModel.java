package com.example.moneywise.ui.budgets;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;


import com.example.moneywise.data.entity.Budget;
import com.example.moneywise.data.entity.Category;
import com.example.moneywise.data.entity.Expense;
import com.example.moneywise.data.model.BudgetStatus;
import com.example.moneywise.repository.MoneyWiseRepository;
import com.example.moneywise.utils.SessionManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BudgetViewModel extends AndroidViewModel {

    private MoneyWiseRepository mRepository;
    private String currentUserId;

    // 1 nguồn dữ liệu
    private LiveData<List<Category>> mAllCategories;
    private LiveData<List<Expense>> mAllExpenses;
    private LiveData<List<Budget>> mAllBudgets;

    // 2. Nguồn dữ liệu kết hợp (Output)
    private MediatorLiveData<List<BudgetStatus>> mBudgetStatuses;

    private MutableLiveData<BudgetStatus> mTotalBudgetStatus = new MutableLiveData<>();
    private double totalSpen;

    public BudgetViewModel(@NonNull Application application) {
        super(application);
        mRepository = new MoneyWiseRepository(application);
        mBudgetStatuses = new MediatorLiveData<>();

        SessionManager sessionManager = new SessionManager(application);
        currentUserId = sessionManager.getUserId(); // Lấy ID đã lưu

        // Lấy 3 nguồn (SỬA: Dùng getAllBudgets)
        mAllBudgets = mRepository.getAllBudgets(currentUserId);
        mAllCategories = mRepository.getAllCategories(currentUserId);
        mAllExpenses = mRepository.getAllExpenses(currentUserId); // Lấy tất cả, sẽ lọc sau

        // 3. Kết hợp chúng lại (SỬA: Dùng mAllBudgets)
        mBudgetStatuses.addSource(mAllBudgets, budgets -> calculateStatuses());
        mBudgetStatuses.addSource(mAllCategories, categories -> calculateStatuses());
        mBudgetStatuses.addSource(mAllExpenses, expenses -> calculateStatuses());
    }

    public LiveData<BudgetStatus> getTotalBudgetStatus() {
        return mTotalBudgetStatus;
    }
    /**
     * Cung cấp LiveData<List<BudgetStatus>> đã xử lý cho View (Activity)
     */
    public LiveData<List<BudgetStatus>> getBudgetStatuses() {
        return mBudgetStatuses;
    }

    /**
     * Đây là hàm tính toán chính.
     * Nó chạy mỗi khi 1 trong 3 nguồn (Budget, Category, Expense) thay đổi.
     */
    private void calculateStatuses() {
        List<Budget> budgets = mAllBudgets.getValue();
        List<Category> categories = mAllCategories.getValue();
        List<Expense> expenses = mAllExpenses.getValue();

        if (budgets == null || categories == null || expenses == null) {
            return;
        }

        // --- LOGIC MỚI: TÍNH TOÁN THÁNG HIỆN TẠI ---
        Calendar cal = Calendar.getInstance();

        // 1. Đặt lịch về Ngày đầu tiên của tháng
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0);
        long startOfMonth = cal.getTimeInMillis();

        // 2. Lấy Ngày cuối cùng của tháng
        int lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        cal.set(Calendar.DAY_OF_MONTH, lastDay);
        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59);
        long endOfMonth = cal.getTimeInMillis();
        // --- KẾT THÚC LOGIC MỚI ---

        // (Tạo categoryMap như cũ)
        Map<String, String> categoryMap = new HashMap<>();
        for (Category category : categories) {
            categoryMap.put(category.getCategoryId(), category.getName());
        }

        List<BudgetStatus> newStatusList = new ArrayList<>();

        // Lặp qua từng Ngân sách
        // --- BƯỚC 3: TÍNH TOÁN "TỔNG" KHI LẶP QUA CÁC BUDGETS CON ---
        double totalBudgeted = 0; // Tổng ngân sách đã đặt
        double totalSpentThisMonth = 0; // Tổng chi tiêu (cho các mục có ngân sách)

        // (Chúng ta sẽ tính tổng chi tiêu CỦA TOÀN BỘ CÁC MỤC)
        // (Logic này chính xác hơn)
        for(Expense expense : expenses) {
            if (expense.getDate() >= startOfMonth && expense.getDate() <= endOfMonth) {
                totalSpentThisMonth += expense.getAmount();
            }
        }

        for (Budget budget : budgets) {
            double categorySpent = 0; // Chi tiêu cho danh mục này
            for (Expense expense : expenses) {
                if (expense.getDate() >= startOfMonth && expense.getDate() <= endOfMonth) {
                    if (budget.getCategoryId() != null && budget.getCategoryId().equals(expense.getCategoryId())) {
                        categorySpent += expense.getAmount();
                    }
                }
            }

            // Cộng vào tổng ngân sách
            totalBudgeted += budget.getAmount();

            // ... (Logic tính % và add vào newStatusList... như cũ)
            String budgetName = categoryMap.getOrDefault(budget.getCategoryId(), "Danh mục đã xóa");
            int progress = 0;
            if (budget.getAmount() > 0) {
                progress = (int) ((categorySpent / budget.getAmount()) * 100);
            }
            newStatusList.add(new BudgetStatus(budget, budgetName, categorySpent, progress));
        }

        // --- BƯỚC 4: TẠO VÀ GỬI ĐI TÌNH TRẠNG "TỔNG" ---

        // Tạo một đối tượng Budget "ảo" để chứa tổng
        Budget totalBudgetInfo = new Budget();
        totalBudgetInfo.setAmount(totalBudgeted);

        int totalProgress = 0;
        if (totalBudgeted > 0) {
            // Thanh "Tổng" sẽ so sánh TỔNG CHI TIÊU với TỔNG NGÂN SÁCH
            totalProgress = (int) ((totalSpentThisMonth / totalBudgeted) * 100);
        }

        // "Tổng chi tiêu" là tên của ngân sách tổng
        BudgetStatus totalStatus = new BudgetStatus(totalBudgetInfo, "Tổng chi tiêu", totalSpentThisMonth, totalProgress);
        mTotalBudgetStatus.postValue(totalStatus); // Gửi đi

        // 5. Gửi đi danh sách các budgets con (như cũ)
        mBudgetStatuses.setValue(newStatusList);
    }

    // (Thêm các hàm insert, update, delete Budget... sau)
    /**
     * Ủy quyền hành động 'insert' cho Repository
     */
    public void insert(Budget budget) {
        // Gán userId và các giá trị mặc định
        budget.setUserId(currentUserId);
        budget.setCreatedAt(System.currentTimeMillis());
        // (synced, updatedAt sẽ do Repository xử lý)

        mRepository.insertBudget(budget); // (Chúng ta sẽ cần tạo hàm này trong Repository)
    }

    public void update(Budget budget) {
        // Gán userId và thời gian
        budget.setUserId(currentUserId);
        budget.setUpdatedAt(System.currentTimeMillis());
        mRepository.updateBudget(budget); // (Sẽ tạo ở bước sau)
    }

    public void delete(Budget budget) {
        mRepository.deleteBudget(budget); // (Sẽ tạo ở bước sau)
    }
}