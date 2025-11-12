package com.example.moneywise.ui.report;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.moneywise.R;
import com.example.moneywise.data.model.BudgetComparisonData;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.tabs.TabLayout;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ReportFragment extends Fragment {
    private LinearLayout mLayoutPieChartGroup;
    private PieChart mPieChart;
    private TextView mTextTotalExpense;
    private ReportViewModel mReportViewModel;
    private TextView mTextCurrentMonth;
    private BarChart mBarChart;
    private TabLayout mTabLayout;

    private ImageButton mBtnPrevMonth;
    private ImageButton mBtnNextMonth;

    private NumberFormat mCurrencyFormat;
    private SimpleDateFormat mMonthFormat;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate layout đã tạo ở Bước 5
        return inflater.inflate(R.layout.fragment_report, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Khởi tạo định dạng
        mCurrencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        mMonthFormat = new SimpleDateFormat("MMMM yyyy", new Locale("vi", "VN"));

        // Lấy ViewModel
        mReportViewModel = new ViewModelProvider(this).get(ReportViewModel.class);

        // Ánh xạ Views
        mTextCurrentMonth = view.findViewById(R.id.text_view_current_month);
        mBtnPrevMonth = view.findViewById(R.id.button_prev_month);
        mBtnNextMonth = view.findViewById(R.id.button_next_month);

        mTabLayout = view.findViewById(R.id.tab_layout_report_mode);

        mLayoutPieChartGroup = view.findViewById(R.id.layout_pie_chart_group);
        mTextTotalExpense = view.findViewById(R.id.text_view_total_expense);
        mPieChart = view.findViewById(R.id.pie_chart_report);

        mBarChart = view.findViewById(R.id.bar_chart_report);

        // Cài đặt
        setupActionBar();
        setupPieChart();
        setupBarChart(); // <-- HÀM MỚI
        setupTabs(); // <-- HÀM MỚI
        setupClickListeners();
        setupObservers();
    }

    private void setupActionBar() {
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity != null && activity.getSupportActionBar() != null) {
            ActionBar actionBar = activity.getSupportActionBar();
            actionBar.setTitle("Báo cáo");
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     * Cấu hình các thuộc tính cơ bản cho biểu đồ tròn
     */
    private void setupPieChart() {
        mPieChart.setUsePercentValues(true);
        mPieChart.getDescription().setEnabled(false);
        mPieChart.setExtraOffsets(5, 10, 5, 5);
        mPieChart.setDragDecelerationFrictionCoef(0.95f);
        mPieChart.setDrawHoleEnabled(true);
        mPieChart.setHoleColor(Color.WHITE);
        mPieChart.setTransparentCircleRadius(61f);
        mPieChart.setDrawCenterText(true);

        // --- THAY ĐỔI 1: TĂNG CỠ CHỮ ---
        mPieChart.setCenterTextSize(16f); // Cỡ chữ "Tổng chi" ở giữa
        mPieChart.setEntryLabelTextSize(12f); // Cỡ chữ tên danh mục (nếu hiển thị)

        // Cấu hình Chú thích (Legend)
        Legend l = mPieChart.getLegend();
        l.setEnabled(true);
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        l.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        l.setDrawInside(false);
        l.setWordWrapEnabled(true); // Cho phép xuống dòng nếu nhiều
        l.setTextSize(14f); // <-- TĂNG CỠ CHỮ CHÚ THÍCH
        // --- KẾT THÚC THAY ĐỔI 1 ---
    }

    /**
     * --- HÀM MỚI: Cài đặt cho Biểu đồ Cột ---
     */
    private void setupBarChart() {
        mBarChart.getDescription().setEnabled(false);
        mBarChart.setDrawGridBackground(false);

        // Cấu hình trục X (Tên danh mục)
        XAxis xAxis = mBarChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f); // Khoảng cách tối thiểu là 1
        xAxis.setCenterAxisLabels(true); // Căn giữa các nhãn

        // Cấu hình trục Y (Số tiền)
        mBarChart.getAxisRight().setEnabled(false); // Tắt trục bên phải
        mBarChart.getAxisLeft().setAxisMinimum(0f); // Bắt đầu từ 0

        mBarChart.getLegend().setTextSize(12f);
    }

    /**
     * --- HÀM MỚI: Xử lý chuyển Tab ---
     */
    private void setupTabs() {
        mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    // Tab "Tỷ trọng"
                    mLayoutPieChartGroup.setVisibility(View.VISIBLE);
                    mBarChart.setVisibility(View.GONE);
                } else if (tab.getPosition() == 1) {
                    // Tab "So sánh Ngân sách"
                    mLayoutPieChartGroup.setVisibility(View.GONE);
                    mBarChart.setVisibility(View.VISIBLE);
                }
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) { }
            @Override
            public void onTabReselected(TabLayout.Tab tab) { }
        });
    }

    /**
     * Gán sự kiện cho các nút điều khiển tháng
     */
    private void setupClickListeners() {
        mBtnPrevMonth.setOnClickListener(v -> mReportViewModel.previousMonth());
        mBtnNextMonth.setOnClickListener(v -> mReportViewModel.nextMonth());
    }

    /**
     * Lắng nghe tất cả LiveData từ ViewModel
     */
    private void setupObservers() {
        // 1. Lắng nghe Tháng
        mReportViewModel.getCurrentDate().observe(getViewLifecycleOwner(), calendar -> {
            if (calendar != null) {
                String monthText = mMonthFormat.format(calendar.getTime());
                // Viết hoa chữ cái đầu
                monthText = monthText.substring(0, 1).toUpperCase() + monthText.substring(1);
                mTextCurrentMonth.setText(monthText);
            }
        });

        // 2. Lắng nghe Tổng chi tiêu
        mReportViewModel.getTotalExpense().observe(getViewLifecycleOwner(), total -> {
            if (total != null) {
                mTextTotalExpense.setText("- " + mCurrencyFormat.format(total));
                // Cập nhật text giữa biểu đồ
                mPieChart.setCenterText("Tổng chi:\n" + mCurrencyFormat.format(total));
            }
        });

        // 3. Lắng nghe Dữ liệu Biểu đồ Tròn
        mReportViewModel.getPieChartData().observe(getViewLifecycleOwner(), entries -> {
            if (entries != null && !entries.isEmpty()) {
                updatePieChartData(entries); // Đổi tên hàm
                mLayoutPieChartGroup.setVisibility(View.VISIBLE);
            } else {
                mPieChart.clear();
                mLayoutPieChartGroup.setVisibility(View.INVISIBLE);
            }
        });

        // 4. --- HÀM MỚI: Lắng nghe Dữ liệu Biểu đồ Cột ---
        mReportViewModel.getBarChartData().observe(getViewLifecycleOwner(), comparisonData -> {
            if (comparisonData != null && !comparisonData.isEmpty()) {
                updateBarChartData(comparisonData);
                // (Không cần set visibility ở đây, Tab listener sẽ xử lý)
            } else {
                mBarChart.clear();
            }
        });
    }

    /**
     * "Vẽ" dữ liệu mới lên biểu đồ tròn
     */
    private void updatePieChartData(List<PieEntry> entries) {
        PieDataSet dataSet = new PieDataSet(entries, ""); // Bỏ tiêu đề của dataset
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);

        // --- THAY ĐỔI 2: TĂNG CỠ CHỮ ---
        // Cấu hình đường chỉ (value lines)
        dataSet.setValueLinePart1OffsetPercentage(80.f);
        dataSet.setValueLinePart1Length(0.5f);
        dataSet.setValueLinePart2Length(0.5f);
        dataSet.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE); // Hiển thị % bên ngoài

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new PercentFormatter(mPieChart));
        data.setValueTextSize(14f); // <-- TĂNG CỠ CHỮ PHẦN TRĂM
        data.setValueTextColor(Color.BLACK);
        // --- KẾT THÚC THAY ĐỔI 2 ---

        mPieChart.setData(data);
        mPieChart.animateY(1000);
        mPieChart.invalidate();
    }
    /**
     * --- HÀM MỚI: "Vẽ" dữ liệu so sánh lên Biểu đồ Cột ---
     */
    private void updateBarChartData(List<BudgetComparisonData> dataList) {
        ArrayList<BarEntry> spentEntries = new ArrayList<>(); // Cột 1: Đã chi
        ArrayList<BarEntry> budgetEntries = new ArrayList<>(); // Cột 2: Ngân sách
        ArrayList<String> labels = new ArrayList<>(); // Nhãn trục X

        for (int i = 0; i < dataList.size(); i++) {
            BudgetComparisonData data = dataList.get(i);

            // Thêm dữ liệu cho 2 cột
            // (Lưu ý: MPAndroidChart dùng 'float')
            spentEntries.add(new BarEntry(i, (float) data.spentAmount));
            budgetEntries.add(new BarEntry(i, (float) data.budgetAmount));

            // Thêm nhãn
            labels.add(data.categoryName);
        }

        BarDataSet spentSet = new BarDataSet(spentEntries, "Đã chi");
        spentSet.setColor(ColorTemplate.rgb("#D32F2F")); // Màu đỏ (colorNegative)

        BarDataSet budgetSet = new BarDataSet(budgetEntries, "Ngân sách");
        budgetSet.setColor(ColorTemplate.rgb("#388E3C")); // Màu xanh

        BarData barData = new BarData(spentSet, budgetSet);

        // --- Cấu hình nhóm cột ---
        float barWidth = 0.45f; // 45% chiều rộng cho 1 cột
        float barSpace = 0.05f; // 5% khoảng cách giữa 2 cột trong 1 nhóm
        float groupSpace = 0.0f; // 0% khoảng cách giữa các nhóm

        barData.setBarWidth(barWidth);

        // Cấu hình trục X
        XAxis xAxis = mBarChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setCenterAxisLabels(true);
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(-45); // Xoay nhãn nếu quá dài

        mBarChart.setData(barData);
        mBarChart.groupBars(0f, groupSpace, barSpace); // (fromX, groupSpace, barSpace)
        mBarChart.getXAxis().setAxisMinimum(0f);
        mBarChart.getXAxis().setAxisMaximum(labels.size()); // Hiển thị tất cả các nhóm

        mBarChart.animateY(1000);
        mBarChart.invalidate();
    }
}