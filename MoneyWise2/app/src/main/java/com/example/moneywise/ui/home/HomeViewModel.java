package com.example.moneywise.ui.home;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.moneywise.data.entity.Category;
import com.example.moneywise.data.entity.Expense;
import com.example.moneywise.data.entity.User;
import com.example.moneywise.data.model.CategoryExpenseSummary;
import com.example.moneywise.data.model.ExpenseWithCategory;
import com.example.moneywise.data.model.HomeCategorySummary;
import com.example.moneywise.repository.MoneyWiseRepository;
import com.example.moneywise.utils.SessionManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeViewModel extends AndroidViewModel {

    private MoneyWiseRepository mRepository;
    private String currentUserId;
    private FirebaseUser mFirebaseUser;

    // --- NGUỒN DỮ LIỆU ĐẦU RA (Cho Fragment) ---
    private LiveData<User> mUser;
    private LiveData<Double> mCurrentMonthTotal;
    private LiveData<Double> mPreviousMonthTotal;
//    private LiveData<List<CategoryExpenseSummary>> mCategorySummary; // (Cho list ngang)
    private MediatorLiveData<List<HomeCategorySummary>> mHomeCategorySummary = new MediatorLiveData<>();
    private MediatorLiveData<List<ExpenseWithCategory>> mRecentTransactions = new MediatorLiveData<>();

    // --- Biến nội bộ để tính toán ---
    private LiveData<List<Category>> mAllCategories;
    private LiveData<List<Expense>> mRecentExpensesRaw;
    private LiveData<List<CategoryExpenseSummary>> mMonthlyCategoryRawSummary;

    private MutableLiveData<Calendar> mCurrentMonthStart = new MutableLiveData<>();
    private MutableLiveData<Calendar> mPreviousMonthStart = new MutableLiveData<>();

    public HomeViewModel(@NonNull Application application) {
        super(application);
        mRepository = new MoneyWiseRepository(application);
        SessionManager sessionManager = new SessionManager(application);
        mFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        currentUserId = sessionManager.getUserId();

        // 1. Tính toán ngày
        Calendar currentMonth = getStartOfMonth(Calendar.getInstance());
        Calendar prevMonth = (Calendar) currentMonth.clone();
        prevMonth.add(Calendar.MONTH, -1);

        mCurrentMonthStart.setValue(currentMonth);
        mPreviousMonthStart.setValue(prevMonth);

        // 2. Lấy dữ liệu người dùng
        mUser = mRepository.getUser(currentUserId);

        // 3. Lấy Tổng chi tháng này (dùng switchMap)
        mCurrentMonthTotal = Transformations.switchMap(mCurrentMonthStart, date -> {
            long startDate = date.getTimeInMillis();
            long endDate = getEndOfMonth(date).getTimeInMillis();
            return mRepository.getTotalSum(currentUserId, startDate, endDate);
        });

        // 4. Lấy Tổng chi tháng trước (dùng switchMap)
        mPreviousMonthTotal = Transformations.switchMap(mPreviousMonthStart, date -> {
            long startDate = date.getTimeInMillis();
            long endDate = getEndOfMonth(date).getTimeInMillis();
            return mRepository.getTotalSum(currentUserId, startDate, endDate);
        });

        // 5. Lấy Tóm tắt Danh mục tháng này (cho list ngang)
        mMonthlyCategoryRawSummary = Transformations.switchMap(mCurrentMonthStart, date -> {
            long startDate = date.getTimeInMillis();
            long endDate = getEndOfMonth(date).getTimeInMillis();
            // (Dùng lại Query của màn hình Báo cáo)
            return mRepository.getExpenseSummaryByCategory(currentUserId, startDate, endDate);
        });

        // 6. Lấy Giao dịch gần đây (dùng Mediator)
        mAllCategories = mRepository.getAllCategories(currentUserId);
        mRecentExpensesRaw = mRepository.getRecentExpenses(currentUserId); // Query mới

        mRecentTransactions.addSource(mAllCategories, categories ->
                combineRecentTransactions(categories, mRecentExpensesRaw.getValue())
        );
        mRecentTransactions.addSource(mRecentExpensesRaw, expenses ->
                combineRecentTransactions(mAllCategories.getValue(), expenses)
        );

        mHomeCategorySummary.addSource(mAllCategories, categories ->
                combineHomeCategorySummary(categories, mMonthlyCategoryRawSummary.getValue(), mCurrentMonthTotal.getValue())
        );
        mHomeCategorySummary.addSource(mMonthlyCategoryRawSummary, summaries ->
                combineHomeCategorySummary(mAllCategories.getValue(), summaries, mCurrentMonthTotal.getValue())
        );
        mHomeCategorySummary.addSource(mCurrentMonthTotal, total ->
                combineHomeCategorySummary(mAllCategories.getValue(), mMonthlyCategoryRawSummary.getValue(), total)
        );
    }

    /**
     * HÀM MỚI: Nơi kết hợp dữ liệu cho thẻ ngang (Fix lỗi NullPointer)
     */
    private void combineHomeCategorySummary(List<Category> categories, List<CategoryExpenseSummary> summaries, Double total) {
        // Chờ cả 3 nguồn sẵn sàng
        if (categories == null || summaries == null || total == null) {
            return; // Nếu 1 trong 3 là null, không làm gì cả
        }

        Map<String, Category> categoryMap = new HashMap<>();
        for (Category cat : categories) {
            categoryMap.put(cat.getCategoryId(), cat);
        }

        List<HomeCategorySummary> result = new ArrayList<>();
        double currentTotal = (total != null) ? total : 0.0;

        for (CategoryExpenseSummary summary : summaries) {
            if (summary.totalAmount <= 0) continue;

            Category cat = categoryMap.get(summary.categoryId);
            String name = (cat != null) ? cat.getName() : "Không rõ";
            String color = (cat != null) ? cat.getColor() : "#808080";

            double percentage = (currentTotal > 0) ? (summary.totalAmount / currentTotal) * 100 : 0.0;

            result.add(new HomeCategorySummary(name, color, summary.totalAmount, percentage));
        }

        // Sắp xếp (tùy chọn): Hiển thị chi nhiều nhất lên đầu
        result.sort((o1, o2) -> Double.compare(o2.totalAmount, o1.totalAmount));

        mHomeCategorySummary.setValue(result);
    }

    /**
     * Hàm nội bộ: Kết hợp Giao dịch gần đây và Tên Danh mục
     */
    private void combineRecentTransactions(List<Category> categories, List<Expense> expenses) {
        if (categories == null || expenses == null) {
            return;
        }

        Map<String, Category> categoryMap = new HashMap<>();
        for (Category cat : categories) {
            categoryMap.put(cat.getCategoryId(), cat);
        }

        List<ExpenseWithCategory> result = new ArrayList<>();
        for (Expense exp : expenses) {
            result.add(new ExpenseWithCategory(exp, categoryMap.get(exp.getCategoryId())));
        }

        mRecentTransactions.setValue(result);
    }

    // --- Các hàm public cho Fragment gọi ---

    public LiveData<User> getUser() { return mUser; }
    public FirebaseUser getFirebaseUser() { return mFirebaseUser; }
    public LiveData<Double> getCurrentMonthTotal() { return mCurrentMonthTotal; }
    public LiveData<Double> getPreviousMonthTotal() { return mPreviousMonthTotal; }
    public LiveData<List<HomeCategorySummary>> getHomeCategorySummary() { return mHomeCategorySummary; }
    public LiveData<List<ExpenseWithCategory>> getRecentTransactions() { return mRecentTransactions; }

    // --- THÊM HÀM MỚI NÀY VÀO ---
    /**
     * Cung cấp danh sách Danh mục (để lấy tên, màu sắc)
     */
    public LiveData<List<Category>> getAllCategories() {
        return mAllCategories;
    }

    // --- Các hàm tiện ích xử lý Ngày/Tháng ---
    private Calendar getStartOfMonth(Calendar cal) {
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return (Calendar) cal.clone();
    }

    private Calendar getEndOfMonth(Calendar cal) {
        Calendar endCal = (Calendar) cal.clone();
        endCal.add(Calendar.MONTH, 1);
        endCal.add(Calendar.MILLISECOND, -1);
        return endCal;
    }
}