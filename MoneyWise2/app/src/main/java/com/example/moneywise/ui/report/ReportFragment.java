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
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.tabs.TabLayout;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ReportFragment extends Fragment {

    private ReportViewModel mReportViewModel;

    // Views
    private LinearLayout mLayoutPieChartGroup;
    private PieChart mPieChart;
    private TextView mTextTotalExpense;
    private BarChart mBarChart;
    private TabLayout mTabLayout;
    private TextView mTextCurrentMonth;
    private ImageButton mBtnPrevMonth;
    private ImageButton mBtnNextMonth;

    private NumberFormat mCurrencyFormat;
    private SimpleDateFormat mMonthFormat;

    // Biến cờ để kiểm tra dữ liệu có rỗng không
    private boolean isPieDataEmpty = true;
    private boolean isBarDataEmpty = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_report, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mCurrencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        mMonthFormat = new SimpleDateFormat("MMMM yyyy", new Locale("vi", "VN"));

        mReportViewModel = new ViewModelProvider(this).get(ReportViewModel.class);

        // Ánh xạ
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
        setupBarChart();
        setupTabs();
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

    private void setupPieChart() {
        mPieChart.setUsePercentValues(true);
        mPieChart.getDescription().setEnabled(false);
        mPieChart.setExtraOffsets(5, 10, 5, 5);
        mPieChart.setDragDecelerationFrictionCoef(0.95f);
        mPieChart.setDrawHoleEnabled(true);
        mPieChart.setHoleColor(Color.WHITE);
        mPieChart.setTransparentCircleRadius(61f);
        mPieChart.setDrawCenterText(true);
        mPieChart.setCenterTextSize(16f);
        mPieChart.setEntryLabelTextSize(12f);

        Legend l = mPieChart.getLegend();
        l.setEnabled(true);
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        l.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        l.setDrawInside(false);
        l.setWordWrapEnabled(true);
        l.setTextSize(14f);
    }

    private void setupBarChart() {
        mBarChart.getDescription().setEnabled(false);
        mBarChart.setDrawGridBackground(false);

        XAxis xAxis = mBarChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setCenterAxisLabels(true);

        mBarChart.getAxisRight().setEnabled(false);
        mBarChart.getAxisLeft().setAxisMinimum(0f);
        mBarChart.getLegend().setTextSize(12f);
    }

    private void setupTabs() {
        mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                // Khi chuyển tab, cập nhật lại hiển thị
                updateVisibilityState();
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) { }
            @Override
            public void onTabReselected(TabLayout.Tab tab) { }
        });
    }

    private void setupClickListeners() {
        mBtnPrevMonth.setOnClickListener(v -> mReportViewModel.previousMonth());
        mBtnNextMonth.setOnClickListener(v -> mReportViewModel.nextMonth());
    }

    private void setupObservers() {
        // 1. Tháng
        mReportViewModel.getCurrentDate().observe(getViewLifecycleOwner(), calendar -> {
            if (calendar != null) {
                String monthText = mMonthFormat.format(calendar.getTime());
                monthText = monthText.substring(0, 1).toUpperCase() + monthText.substring(1);
                mTextCurrentMonth.setText(monthText);
            }
        });

        // 2. Tổng chi tiêu
        mReportViewModel.getTotalExpense().observe(getViewLifecycleOwner(), total -> {
            if (total != null) {
                String formattedTotal = mCurrencyFormat.format(total);
                mTextTotalExpense.setText("- " + formattedTotal);
                mPieChart.setCenterText("Tổng chi\n" + formattedTotal);
            }
        });

        // 3. Dữ liệu Biểu đồ Tròn
        mReportViewModel.getPieChartData().observe(getViewLifecycleOwner(), entries -> {
            if (entries != null && !entries.isEmpty()) {
                updatePieChartData(entries);
                isPieDataEmpty = false;
            } else {
                mPieChart.clear();
                isPieDataEmpty = true;
            }
            // Dữ liệu thay đổi -> Cập nhật hiển thị
            updateVisibilityState();
        });

        // 4. Dữ liệu Biểu đồ Cột
        mReportViewModel.getBarChartData().observe(getViewLifecycleOwner(), comparisonData -> {
            if (comparisonData != null && !comparisonData.isEmpty()) {
                updateBarChartData(comparisonData);
                isBarDataEmpty = false;
            } else {
                mBarChart.clear();
                isBarDataEmpty = true;
            }
            // Dữ liệu thay đổi -> Cập nhật hiển thị
            updateVisibilityState();
        });
    }

    /**
     * HÀM MỚI QUAN TRỌNG: Quản lý việc Ẩn/Hiện dựa trên Tab và Dữ liệu
     * Hàm này đảm bảo CHỈ 1 biểu đồ được hiện tại 1 thời điểm
     */
    private void updateVisibilityState() {
        int selectedTabPosition = mTabLayout.getSelectedTabPosition();

        if (selectedTabPosition == 0) {
            // Tab 0: Tỷ trọng (Pie Chart)
            mBarChart.setVisibility(View.GONE); // Luôn ẩn BarChart

            if (!isPieDataEmpty) {
                mLayoutPieChartGroup.setVisibility(View.VISIBLE);
            } else {
                mLayoutPieChartGroup.setVisibility(View.INVISIBLE); // Hoặc hiển thị thông báo "Không có dữ liệu"
            }
        } else if (selectedTabPosition == 1) {
            // Tab 1: So sánh (Bar Chart)
            mLayoutPieChartGroup.setVisibility(View.GONE); // Luôn ẩn PieChart group

            if (!isBarDataEmpty) {
                mBarChart.setVisibility(View.VISIBLE);
            } else {
                mBarChart.setVisibility(View.INVISIBLE);
            }
        }
    }

    private void updatePieChartData(List<PieEntry> entries) {
        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);

        dataSet.setValueLinePart1OffsetPercentage(80.f);
        dataSet.setValueLinePart1Length(0.5f);
        dataSet.setValueLinePart2Length(0.5f);
        dataSet.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new PercentFormatter(mPieChart));
        data.setValueTextSize(14f);
        data.setValueTextColor(Color.BLACK);

        mPieChart.setData(data);
        mPieChart.animateY(1000);
        mPieChart.invalidate();
    }

    private void updateBarChartData(List<BudgetComparisonData> dataList) {
        ArrayList<BarEntry> spentEntries = new ArrayList<>();
        ArrayList<BarEntry> budgetEntries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();

        for (int i = 0; i < dataList.size(); i++) {
            BudgetComparisonData data = dataList.get(i);
            spentEntries.add(new BarEntry(i, (float) data.spentAmount));
            budgetEntries.add(new BarEntry(i, (float) data.budgetAmount));
            labels.add(data.categoryName);
        }

        BarDataSet spentSet = new BarDataSet(spentEntries, "Đã chi");
        spentSet.setColor(ColorTemplate.rgb("#D32F2F"));
        BarDataSet budgetSet = new BarDataSet(budgetEntries, "Ngân sách");
        budgetSet.setColor(ColorTemplate.rgb("#388E3C"));

        BarData barData = new BarData(spentSet, budgetSet);
        float barWidth = 0.45f;
        float barSpace = 0.05f;
        float groupSpace = 0.0f;

        barData.setBarWidth(barWidth);

        XAxis xAxis = mBarChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setLabelRotationAngle(-45);
        xAxis.setAxisMaximum(labels.size());

        mBarChart.setData(barData);
        mBarChart.groupBars(0f, groupSpace, barSpace);
        mBarChart.getXAxis().setAxisMinimum(0f);
        mBarChart.getXAxis().setAxisMaximum(labels.size());

        mBarChart.animateY(1000);
        mBarChart.invalidate();
    }
}