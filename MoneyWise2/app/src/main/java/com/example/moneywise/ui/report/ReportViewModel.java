package com.example.moneywise.ui.report;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.moneywise.data.entity.Budget;
import com.example.moneywise.data.entity.Category;
import com.example.moneywise.data.model.BudgetComparisonData;
import com.example.moneywise.data.model.CategoryExpenseSummary;
import com.example.moneywise.repository.MoneyWiseRepository;
import com.example.moneywise.utils.SessionManager;
import com.github.mikephil.charting.data.PieEntry; // <-- Import thư viện biểu đồ

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportViewModel extends AndroidViewModel {

    private MoneyWiseRepository mRepository;
    private String currentUserId;

    // 1. NGUỒN ĐẦU VÀO: Tháng hiện tại người dùng đang xem
    // (Chúng ta dùng 'Calendar' để giữ ngày đầu tiên của tháng)
    private MutableLiveData<Calendar> mCurrentDate = new MutableLiveData<>();

    // 2. NGUỒN DỮ LIỆU TỪ CSDL:
    private LiveData<List<Category>> mAllCategories;
    private LiveData<List<CategoryExpenseSummary>> mMonthlySummary;
    private LiveData<List<Budget>> mAllBudgets;

    // 3. KẾT QUẢ ĐẦU RA: Dữ liệu cuối cùng cho biểu đồ
    private MediatorLiveData<List<PieEntry>> mChartData = new MediatorLiveData<>();
    private MediatorLiveData<Double> mTotalExpense = new MediatorLiveData<>();
    private MediatorLiveData<List<BudgetComparisonData>> mBarChartData = new MediatorLiveData<>();

    public ReportViewModel(@NonNull Application application) {
        super(application);
        mRepository = new MoneyWiseRepository(application);
        SessionManager sessionManager = new SessionManager(application);
        currentUserId = sessionManager.getUserId();

        // Khởi tạo tháng hiện tại
        mCurrentDate.setValue(getStartOfMonth(Calendar.getInstance()));

        // Lấy danh sách TẤT CẢ danh mục (chỉ cần lấy 1 lần)
        mAllCategories = mRepository.getAllCategories(currentUserId);
        mAllBudgets = mRepository.getAllBudgets(currentUserId);

        // --- Logic quan trọng (SwitchMap) ---
        // Khi 'mCurrentDate' thay đổi, 'mMonthlySummary' sẽ tự động
        // kích hoạt 1 truy vấn (Query) mới đến CSDL.
        mMonthlySummary = Transformations.switchMap(mCurrentDate, (date) -> {
            long startDate = date.getTimeInMillis();
            long endDate = getEndOfMonth(date).getTimeInMillis();
            // Gọi hàm DAO chúng ta đã tạo ở Bước 3
            return mRepository.getExpenseSummaryByCategory(currentUserId, startDate, endDate);
        });

        // --- Logic kết hợp (Mediator) ---
        // 'mChartData' sẽ lắng nghe cả 2 nguồn:
        // 1. mAllCategories (Để lấy TÊN)
        // 2. mMonthlySummary (Để lấy SỐ TIỀN)
        mChartData.addSource(mAllCategories, categories -> combineDataForPieChart());
        mChartData.addSource(mMonthlySummary, summaries -> combineDataForPieChart());

        // Mediator cho Tổng chi tiêu (chỉ cần lắng nghe mMonthlySummary)
        mTotalExpense.addSource(mMonthlySummary, summaries -> {
            double total = 0;
            if (summaries != null) {
                for (CategoryExpenseSummary summary : summaries) {
                    total += summary.totalAmount;
                }
            }
            mTotalExpense.setValue(total);
        });

        // --- THÊM MỚI: Mediator cho Biểu đồ Cột ---
        // (Lắng nghe cả 3 nguồn)
        mBarChartData.addSource(mAllCategories, categories -> combineDataForBarChart());
        mBarChartData.addSource(mMonthlySummary, summaries -> combineDataForBarChart());
        mBarChartData.addSource(mAllBudgets, budgets -> combineDataForBarChart());
    }

    /**
     * Hàm nội bộ: Chạy khi 1 trong 2 nguồn dữ liệu thay đổi
     * để tạo ra danh sách PieEntry cho biểu đồ.
     */
    private void combineDataForPieChart() {
        List<Category> categories = mAllCategories.getValue();
        List<CategoryExpenseSummary> summaries = mMonthlySummary.getValue();

        // Chờ cho cả 2 nguồn sẵn sàng
        if (categories == null || summaries == null) {
            return;
        }

        // Tối ưu hóa: Chuyển danh sách Category thành Map
        Map<String, String> categoryMap = new HashMap<>();
        for (Category category : categories) {
            categoryMap.put(category.getCategoryId(), category.getName());
        }

        // Tạo danh sách PieEntry
        List<PieEntry> entries = new ArrayList<>();
        for (CategoryExpenseSummary summary : summaries) {
            // Chỉ thêm vào biểu đồ nếu có chi tiêu
            if (summary.totalAmount > 0) {
                String categoryName = categoryMap.getOrDefault(summary.categoryId, "Không rõ");
                entries.add(new PieEntry((float) summary.totalAmount, categoryName));
            }
        }

        mChartData.setValue(entries);
    }

    /**
     * --- HÀM MỚI: Xử lý dữ liệu cho Biểu đồ Cột ---
     */
    private void combineDataForBarChart() {
        List<Category> categories = mAllCategories.getValue();
        List<CategoryExpenseSummary> summaries = mMonthlySummary.getValue();
        List<Budget> budgets = mAllBudgets.getValue();

        // Chờ cả 3 nguồn sẵn sàng
        if (categories == null || summaries == null || budgets == null) {
            return;
        }

        // Tối ưu hóa: Tạo 2 map
        Map<String, String> categoryNameMap = new HashMap<>();
        for (Category category : categories) {
            categoryNameMap.put(category.getCategoryId(), category.getName());
        }

        Map<String, Double> expenseMap = new HashMap<>();
        for (CategoryExpenseSummary summary : summaries) {
            expenseMap.put(summary.categoryId, summary.totalAmount);
        }

        // Tạo danh sách kết quả (chỉ hiển thị các mục có ngân sách)
        List<BudgetComparisonData> resultList = new ArrayList<>();

        for (Budget budget : budgets) {
            String catId = budget.getCategoryId();
            String catName = categoryNameMap.getOrDefault(catId, "Không rõ");
            double budgetAmount = budget.getAmount();
            double spentAmount = expenseMap.getOrDefault(catId, 0.0); // Lấy chi tiêu, 0 nếu ko có

            // Thêm vào danh sách
            resultList.add(new BudgetComparisonData(catName, spentAmount, budgetAmount));
        }

        mBarChartData.setValue(resultList);
    }

    // --- Các hàm Public cho Fragment gọi (Cập nhật) ---

    public LiveData<List<PieEntry>> getPieChartData() { // <-- ĐỔI TÊN
        return mChartData;
    }

    public LiveData<List<BudgetComparisonData>> getBarChartData() { // <-- HÀM MỚI
        return mBarChartData;
    }

    public LiveData<Double> getTotalExpense() {
        return mTotalExpense;
    }

    public LiveData<Calendar> getCurrentDate() {
        return mCurrentDate;
    }

    public void nextMonth() {
        Calendar cal = mCurrentDate.getValue();
        if (cal == null) return;
        cal.add(Calendar.MONTH, 1);
        mCurrentDate.setValue(cal);
    }

    public void previousMonth() {
        Calendar cal = mCurrentDate.getValue();
        if (cal == null) return;
        cal.add(Calendar.MONTH, -1);
        mCurrentDate.setValue(cal);
    }


    // --- Các hàm tiện ích xử lý Ngày/Tháng ---

    private Calendar getStartOfMonth(Calendar cal) {
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal;
    }

    private Calendar getEndOfMonth(Calendar cal) {
        Calendar endCal = (Calendar) cal.clone();
        endCal.add(Calendar.MONTH, 1);
        endCal.add(Calendar.MILLISECOND, -1);
        return endCal;
    }
}