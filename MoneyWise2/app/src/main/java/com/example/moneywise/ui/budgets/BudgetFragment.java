package com.example.moneywise.ui.budgets;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.moneywise.data.entity.Budget;
import com.example.moneywise.data.entity.BudgetPeriod;
import com.example.moneywise.data.model.BudgetStatus;
import com.example.moneywise.ui.budgets.AddEditBudgetActivity;
import com.example.moneywise.utils.SessionManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.example.moneywise.R;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;

public class BudgetFragment extends Fragment implements BudgetAdapter.OnBudgetItemClickListener {

    private BudgetViewModel mBudgetViewModel;
    private BudgetAdapter mAdapter;
    private ListView mListView;
    private FloatingActionButton mFab;

    private View mTotalBudgetBar; // Thẻ CardView
    private TextView mTotalBudgetName;
    private TextView mTotalBudgetPeriod;
    private ProgressBar mTotalProgressBar;
    private TextView mTotalSpent;
    private TextView mTotalAmount;
    private NumberFormat mCurrencyFormat; // Để định dạng tiền

    private String currentUserId;

    // Launcher để mở màn hình Thêm/Sửa Ngân sách
    private ActivityResultLauncher<Intent> mAddEditBudgetLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_budget, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

// Khởi tạo định dạng tiền
        mCurrencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

        // 1. Ánh xạ Views (của Fragment)
        mListView = view.findViewById(R.id.list_view_budgets);
        mFab = view.findViewById(R.id.fab_add_budget);

        // --- BƯỚC 2: ÁNH XẠ CÁC VIEW CỦA THANH "TỔNG" ---
        mTotalBudgetBar = view.findViewById(R.id.total_budget_bar);
        mTotalBudgetName = mTotalBudgetBar.findViewById(R.id.text_view_budget_name);
        mTotalBudgetPeriod = mTotalBudgetBar.findViewById(R.id.text_view_budget_period);
        mTotalProgressBar = mTotalBudgetBar.findViewById(R.id.progress_bar_budget);
        mTotalSpent = mTotalBudgetBar.findViewById(R.id.text_view_spent);
        mTotalAmount = mTotalBudgetBar.findViewById(R.id.text_view_total);

        // (Ẩn các nút Sửa/Xóa trên thanh Tổng)
        mTotalBudgetBar.findViewById(R.id.layout_buttons_budget).setVisibility(View.GONE);

        // 2. Khởi tạo Adapter
        mAdapter = new BudgetAdapter(requireContext(), new ArrayList<>(), this);
        mListView.setAdapter(mAdapter);

        // 3. Lấy ViewModel
        mBudgetViewModel = new ViewModelProvider(this).get(BudgetViewModel.class);

        // 4. Theo dõi LiveData (cho ListView)
        mBudgetViewModel.getBudgetStatuses().observe(getViewLifecycleOwner(), budgetStatuses -> {
            mAdapter.setData(budgetStatuses);
        });

        // --- BƯỚC 3: THEO DÕI LIVE DATA "TỔNG" MỚI ---
        mBudgetViewModel.getTotalBudgetStatus().observe(getViewLifecycleOwner(), totalStatus -> {
            if (totalStatus != null) {
                // Cập nhật giao diện thanh "Tổng"
                mTotalBudgetName.setText(totalStatus.categoryName); // "Tổng chi tiêu"
                mTotalBudgetPeriod.setText("Tháng này");
                mTotalProgressBar.setProgress(totalStatus.progressPercent);
                mTotalSpent.setText(mCurrencyFormat.format(totalStatus.spentAmount));
                mTotalAmount.setText(mCurrencyFormat.format(totalStatus.budget.amount));
            }
        });




        mAddEditBudgetLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        String categoryId = data.getStringExtra(AddEditBudgetActivity.EXTRA_BUDGET_CATEGORY_ID);
                        double amount = data.getDoubleExtra(AddEditBudgetActivity.EXTRA_BUDGET_AMOUNT, 0);

                        // --- CẬP NHẬT LOGIC ---
                        if (data.hasExtra(AddEditBudgetActivity.EXTRA_BUDGET_ID)) {
                            // --- CHẾ ĐỘ SỬA ---
                            String budgetId = data.getStringExtra(AddEditBudgetActivity.EXTRA_BUDGET_ID);

                            // Tạo đối tượng Budget để cập nhật
                            // (Chúng ta không cần tạo mới, chỉ cần ID, categoryId, amount)
                            // (Chúng ta sẽ cần hàm getBudgetById_Sync nếu muốn giữ lại createdAt)
                            // Tạm thời, tạo mới với ID cũ
                            SessionManager sessionManager = new SessionManager(requireActivity().getApplication());
                            currentUserId = sessionManager.getUserId(); // Lấy ID đã lưu

                            Budget updatedBudget = new Budget(
                                    budgetId, // ID Cũ
                                    currentUserId,
                                    categoryId,
                                    amount,
                                    System.currentTimeMillis() // Coi như là createdAt mới
                            );
                            // TODO: Nâng cấp: Tải budget cũ, chỉ cập nhật amount/categoryId

                            mBudgetViewModel.update(updatedBudget);
                            Toast.makeText(requireContext(), "Đã cập nhật Ngân sách!", Toast.LENGTH_SHORT).show();

                        } else {
                            // --- CHẾ ĐỘ THÊM (Như cũ) ---
                            Budget newBudget = new Budget(
                                    UUID.randomUUID().toString(),
                                    currentUserId, // TODO: Thay ID thật
                                    categoryId,
                                    amount,
                                    System.currentTimeMillis()
                            );
                            mBudgetViewModel.insert(newBudget);
                            Toast.makeText(requireContext(), "Đã lưu Ngân sách!", Toast.LENGTH_SHORT).show();
                        }
                    }
                });



        // 6. Xử lý FAB Click (dùng requireContext())
        mFab.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), AddEditBudgetActivity.class);
            mAddEditBudgetLauncher.launch(intent);
        });
    }


    /**
     * Được gọi TỪ ADAPTER khi nhấn nút "Sửa"
     */
    @Override
    public void onEditClick(BudgetStatus budgetStatus) {
        Budget budgetToEdit = budgetStatus.budget;

        Intent intent = new Intent(requireContext(), AddEditBudgetActivity.class);

        // Gửi dữ liệu Sửa sang
        intent.putExtra(AddEditBudgetActivity.EXTRA_BUDGET_ID, budgetToEdit.budgetId);
        // (Gửi 2 cái này để AddEditBudgetActivity có thể điền vào
        //  ngay cả trước khi ViewModel tải xong)
        intent.putExtra(AddEditBudgetActivity.EXTRA_BUDGET_CATEGORY_ID, budgetToEdit.categoryId);
        intent.putExtra(AddEditBudgetActivity.EXTRA_BUDGET_AMOUNT, budgetToEdit.amount);

        mAddEditBudgetLauncher.launch(intent);
    }

    /**
     * Được gọi TỪ ADAPTER khi nhấn nút "Xóa"
     */
    @Override
    public void onDeleteClick(BudgetStatus budgetStatus) {
        // Lấy Ngân sách (Budget) từ đối tượng Status
        Budget budgetToDelete = budgetStatus.budget;
        showDeleteConfirmationDialog(budgetToDelete);
    }

    // --- BƯỚC 5: THÊM HÀM DIALOG XÁC NHẬN ---
    private void showDeleteConfirmationDialog(Budget budgetToDelete) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Xác nhận Xóa");
        builder.setMessage("Bạn có chắc chắn muốn xóa ngân sách này không?");

        builder.setPositiveButton("Xóa", (dialog, which) -> {
            // TODO: Bạn cần thêm hàm delete(Budget) vào BudgetViewModel
            mBudgetViewModel.delete(budgetToDelete);
            Toast.makeText(requireContext(), "Đã xóa ngân sách", Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("Hủy", (dialog, which) -> {
            if (dialog != null) dialog.dismiss();
        });

        builder.create().show();
    }
}
