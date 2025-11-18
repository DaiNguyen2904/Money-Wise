package com.example.moneywise.ui.budgets;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.moneywise.R;
import com.example.moneywise.data.entity.Budget;
import com.example.moneywise.data.model.BudgetStatus;
import com.example.moneywise.utils.SessionManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class BudgetFragment extends Fragment implements BudgetAdapter.OnBudgetItemClickListener {

    private BudgetViewModel mBudgetViewModel;
    private BudgetAdapter mAdapter;
    private ListView mListView;
    private FloatingActionButton mFab;

    // Các View của thanh Tổng
    private View mTotalBudgetBar;
    private TextView mTotalBudgetName;
    private TextView mTotalBudgetPeriod;
    private ProgressBar mTotalProgressBar;
    private TextView mTotalSpent;
    private TextView mTotalAmount;
    private TextView mTotalWarningText; // Thêm view này

    private NumberFormat mCurrencyFormat;
    private String currentUserId;
    private ActivityResultLauncher<Intent> mAddEditBudgetLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_budget, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // --- Setup ActionBar ---
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity != null && activity.getSupportActionBar() != null) {
            ActionBar actionBar = activity.getSupportActionBar();
            actionBar.setTitle("Ngân sách");
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mCurrencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

        // --- Ánh xạ Views ---
        mListView = view.findViewById(R.id.list_view_budgets);
        mFab = view.findViewById(R.id.fab_add_budget);

        // --- Ánh xạ Thanh Tổng ---
        mTotalBudgetBar = view.findViewById(R.id.total_budget_bar);
        mTotalBudgetName = mTotalBudgetBar.findViewById(R.id.text_view_budget_name);
        mTotalBudgetPeriod = mTotalBudgetBar.findViewById(R.id.text_view_budget_period);
        mTotalProgressBar = mTotalBudgetBar.findViewById(R.id.progress_bar_budget);
        mTotalSpent = mTotalBudgetBar.findViewById(R.id.text_view_spent);
        mTotalAmount = mTotalBudgetBar.findViewById(R.id.text_view_total);
        mTotalWarningText = mTotalBudgetBar.findViewById(R.id.text_view_budget_warning); // Ánh xạ text cảnh báo

        // Ẩn nút Sửa/Xóa ở thanh tổng
        mTotalBudgetBar.findViewById(R.id.layout_buttons_budget).setVisibility(View.GONE);

        // --- Setup Adapter & ViewModel ---
        mAdapter = new BudgetAdapter(requireContext(), new ArrayList<>(), this);
        mListView.setAdapter(mAdapter);

        mBudgetViewModel = new ViewModelProvider(this).get(BudgetViewModel.class);

        // --- OBSERVER 1: Danh sách ngân sách ---
        mBudgetViewModel.getBudgetStatuses().observe(getViewLifecycleOwner(), budgetStatuses -> {
            mAdapter.setData(budgetStatuses);
            // Kiểm tra và hiện Snackbar cảnh báo chung
            checkAndShowSnackbar(budgetStatuses);
        });

        // --- OBSERVER 2: Tổng ngân sách ---
        mBudgetViewModel.getTotalBudgetStatus().observe(getViewLifecycleOwner(), totalStatus -> {
            if (totalStatus != null) {
                mTotalBudgetName.setText(totalStatus.categoryName); // "Tổng chi tiêu"
                mTotalBudgetPeriod.setText("Tháng này");
                mTotalSpent.setText(mCurrencyFormat.format(totalStatus.spentAmount));
                mTotalAmount.setText(mCurrencyFormat.format(totalStatus.budget.getAmount()));

                // Cập nhật màu sắc và cảnh báo cho thanh Tổng
                updateBudgetVisuals(totalStatus, mTotalProgressBar, mTotalSpent, mTotalWarningText);
            }
        });

        // --- Setup Launcher (Thêm/Sửa) ---
        setupActivityResultLauncher();

        mFab.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), AddEditBudgetActivity.class);
            mAddEditBudgetLauncher.launch(intent);
        });
    }

    /**
     * Hàm cập nhật màu sắc và cảnh báo cho 1 View (Dùng cho thanh Tổng)
     */
    private void updateBudgetVisuals(BudgetStatus status, ProgressBar progressBar, TextView textSpent, TextView textWarning) {
        int progress = status.progressPercent;
        int colorResId;
        String warningMsg = "";
        boolean showWarning = false;

        if (progress >= 100) {
            colorResId = R.color.budget_exceeded; // Đỏ
            warningMsg = "Tổng chi tiêu đã vỡ kế hoạch!";
            showWarning = true;
        } else if (progress >= 80) {
            colorResId = R.color.budget_warning; // Cam
            warningMsg = "Tổng chi tiêu sắp hết hạn mức.";
            showWarning = true;
        } else {
            colorResId = R.color.budget_safe; // Xanh
            showWarning = false;
        }

        int color = ContextCompat.getColor(requireContext(), colorResId);

        // Đổi màu
        progressBar.setProgress(progress);
        progressBar.setProgressTintList(ColorStateList.valueOf(color));
        textSpent.setTextColor(color);

        // Hiện Text cảnh báo
        if (showWarning && textWarning != null) {
            textWarning.setVisibility(View.VISIBLE);
            textWarning.setText(warningMsg);
            textWarning.setTextColor(color);
        } else if (textWarning != null) {
            textWarning.setVisibility(View.GONE);
        }
    }

    /**
     * Kiểm tra danh sách để hiện Snackbar nếu có mục nguy hiểm
     */
    private void checkAndShowSnackbar(List<BudgetStatus> statuses) {
        int exceededCount = 0;
        for (BudgetStatus status : statuses) {
            if (status.progressPercent >= 100) exceededCount++;
        }

        if (exceededCount > 0) {
            Snackbar snackbar = Snackbar.make(mListView, "⚠️ Có " + exceededCount + " mục chi tiêu vượt quá ngân sách!", Snackbar.LENGTH_LONG);
            snackbar.setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.budget_exceeded));
            snackbar.setAction("Xem", v -> {}); // Nút hành động (nếu cần)
            snackbar.show();
        }
    }

    private void setupActivityResultLauncher() {
        mAddEditBudgetLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        String categoryId = data.getStringExtra(AddEditBudgetActivity.EXTRA_BUDGET_CATEGORY_ID);
                        double amount = data.getDoubleExtra(AddEditBudgetActivity.EXTRA_BUDGET_AMOUNT, 0);

                        SessionManager sessionManager = new SessionManager(requireActivity().getApplication());
                        currentUserId = sessionManager.getUserId();

                        if (data.hasExtra(AddEditBudgetActivity.EXTRA_BUDGET_ID)) {
                            // Sửa
                            String budgetId = data.getStringExtra(AddEditBudgetActivity.EXTRA_BUDGET_ID);
                            Budget updatedBudget = new Budget(budgetId, currentUserId, categoryId, amount, System.currentTimeMillis());
                            mBudgetViewModel.update(updatedBudget);
                            Toast.makeText(requireContext(), "Đã cập nhật!", Toast.LENGTH_SHORT).show();
                        } else {
                            // Thêm mới
                            Budget newBudget = new Budget(UUID.randomUUID().toString(), currentUserId, categoryId, amount, System.currentTimeMillis());
                            mBudgetViewModel.insert(newBudget);
                            Toast.makeText(requireContext(), "Đã thêm mới!", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    @Override
    public void onEditClick(BudgetStatus budgetStatus) {
        Intent intent = new Intent(requireContext(), AddEditBudgetActivity.class);
        intent.putExtra(AddEditBudgetActivity.EXTRA_BUDGET_ID, budgetStatus.budget.getBudgetId());
        intent.putExtra(AddEditBudgetActivity.EXTRA_BUDGET_CATEGORY_ID, budgetStatus.budget.getCategoryId());
        intent.putExtra(AddEditBudgetActivity.EXTRA_BUDGET_AMOUNT, budgetStatus.budget.getAmount());
        mAddEditBudgetLauncher.launch(intent);
    }

    @Override
    public void onDeleteClick(BudgetStatus budgetStatus) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Xác nhận Xóa")
                .setMessage("Bạn có chắc chắn muốn xóa ngân sách mục " + budgetStatus.categoryName + " không?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    mBudgetViewModel.delete(budgetStatus.budget);
                    Toast.makeText(requireContext(), "Đã xóa ngân sách", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}