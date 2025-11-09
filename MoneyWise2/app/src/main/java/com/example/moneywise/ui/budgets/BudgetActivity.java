package com.example.moneywise.ui.budgets;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.moneywise.data.entity.Budget;
import com.example.moneywise.data.entity.BudgetPeriod;
import com.example.moneywise.data.model.BudgetStatus;
import com.example.moneywise.ui.budgets.AddEditBudgetActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.example.moneywise.R;

import java.util.ArrayList;
import java.util.UUID;

public class BudgetActivity extends AppCompatActivity implements BudgetAdapter.OnBudgetItemClickListener {

    private BudgetViewModel mBudgetViewModel;
    private BudgetAdapter mAdapter;
    private ListView mListView;
    private FloatingActionButton mFab;

    // Launcher để mở màn hình Thêm/Sửa Ngân sách
    private ActivityResultLauncher<Intent> mAddEditBudgetLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_budget);

        // Cài đặt ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Quản lý Ngân sách");
        }

        // 1. Ánh xạ Views
        mListView = findViewById(R.id.list_view_budgets);
        mFab = findViewById(R.id.fab_add_budget);

        // 2. Khởi tạo Adapter
        mAdapter = new BudgetAdapter(this, new ArrayList<>(), this);
        mListView.setAdapter(mAdapter);

        // 3. Lấy ViewModel
        ViewModelProvider.AndroidViewModelFactory factory =
                ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication());
        mBudgetViewModel = new ViewModelProvider(this, factory).get(BudgetViewModel.class);

        // 4. THEO DÕI LIVE DATA (Quan trọng!)
        // Chúng ta theo dõi 'BudgetStatus' (dữ liệu đã xử lý)
        mBudgetViewModel.getBudgetStatuses().observe(this, budgetStatuses -> {
            // Khi dữ liệu thay đổi (từ Budget, Category, HOẶC Expense)
            // Adapter sẽ được cập nhật
            mAdapter.setData(budgetStatuses);
        });

// 5. ĐĂNG KÝ LAUNCHER (Cập nhật)
        mAddEditBudgetLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            // Lấy dữ liệu trả về từ AddEditBudgetActivity
                            String categoryId = data.getStringExtra(AddEditBudgetActivity.EXTRA_BUDGET_CATEGORY_ID);
                            double amount = data.getDoubleExtra(AddEditBudgetActivity.EXTRA_BUDGET_AMOUNT, 0);

                            // Tạo đối tượng Budget mới
                            Budget newBudget = new Budget();
                            newBudget.setBudgetId( UUID.randomUUID().toString());
                            newBudget.setCategoryId(categoryId);
                            newBudget.setAmount(amount);

                            // Gọi ViewModel để chèn
                            mBudgetViewModel.insert(newBudget);

                            Toast.makeText(this, "Đã lưu ngân sách!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Đã hủy", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        // 6. Xử lý sự kiện nhấn FAB (Cập nhật)
        mFab.setOnClickListener(view -> {
            // Mở màn hình AddEditBudgetActivity
            Intent intent = new Intent(BudgetActivity.this, AddEditBudgetActivity.class);
            mAddEditBudgetLauncher.launch(intent);
        });


    }

    // Xử lý khi nhấn nút Back trên ActionBar
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    /**
     * Được gọi TỪ ADAPTER khi nhấn nút "Sửa"
     */
    @Override
    public void onEditClick(BudgetStatus budgetStatus) {
        // Lấy Ngân sách (Budget) từ đối tượng Status
        Budget budgetToEdit = budgetStatus.budget;

        // Mở màn hình AddEditBudgetActivity
        Intent intent = new Intent(BudgetActivity.this, AddEditBudgetActivity.class);

        // TODO: Gửi ID và dữ liệu sang cho chế độ "Sửa"
        // (Chúng ta chưa làm logic Sửa cho Ngân sách,
        //  nhưng đây là nơi bạn sẽ đặt nó)
        // intent.putExtra(AddEditBudgetActivity.EXTRA_BUDGET_ID, budgetToEdit.budgetId);

        // Tạm thời, chúng ta chỉ mở chế độ "Thêm"
        // mAddEditBudgetLauncher.launch(intent);
        Toast.makeText(this, "Chức năng Sửa Ngân sách (chưa làm)", Toast.LENGTH_SHORT).show();
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
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Xác nhận Xóa");
        builder.setMessage("Bạn có chắc chắn muốn xóa ngân sách này không?");

        builder.setPositiveButton("Xóa", (dialog, which) -> {
            // TODO: Bạn cần thêm hàm delete(Budget) vào BudgetViewModel
            mBudgetViewModel.delete(budgetToDelete);
            Toast.makeText(this, "Đã xóa ngân sách", Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("Hủy", (dialog, which) -> {
            if (dialog != null) dialog.dismiss();
        });

        builder.create().show();
    }
}
