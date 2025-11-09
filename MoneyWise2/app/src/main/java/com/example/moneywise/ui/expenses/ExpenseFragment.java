package com.example.moneywise.ui.expenses;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.moneywise.R;
import com.example.moneywise.data.entity.Expense;
import com.example.moneywise.ui.budgets.BudgetActivity;
import com.example.moneywise.ui.categories.CategoryActivity;
import com.example.moneywise.utils.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;


import java.util.ArrayList;
import java.util.UUID;

public class ExpenseFragment extends Fragment implements ExpenseAdapter.OnExpenseItemClickListener {

    private static final String TAG = "ExpenseActivity";

    // 1. Khai báo ViewModel, Adapter, và Views
    private ExpenseViewModel mExpenseViewModel;
    private ExpenseAdapter mAdapter;
    private ListView mListView;
    private FloatingActionButton mFab;

    private ActivityResultLauncher<Intent> mAddExpenseLauncher;

    private SearchView mSearchView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Trả về View mà Fragment này sẽ quản lý
        return inflater.inflate(R.layout.fragment_expense, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // --- CÀI ĐẶT ACTIONBAR ---
        // Lấy ActionBar từ Activity cha
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity != null && activity.getSupportActionBar() != null) {
            ActionBar actionBar = activity.getSupportActionBar();
            actionBar.setTitle("Chi tiêu"); // Đặt tiêu đề
            actionBar.setDisplayHomeAsUpEnabled(true); // HIỂN THỊ nút quay lại
        }

        // 1. Ánh xạ Views (thêm "view.")
        mListView = view.findViewById(R.id.list_view_expenses);
        mFab = view.findViewById(R.id.fab_add_expense);
        mSearchView = view.findViewById(R.id.search_view_expense);

        // 2. Khởi tạo Adapter (thay 'this' bằng 'requireContext()')
        mAdapter = new ExpenseAdapter(requireContext(), new ArrayList<>(), this);
        mListView.setAdapter(mAdapter);

        // 3. Lấy ViewModel (thay 'this' bằng 'this' hoặc 'requireActivity()')
        ViewModelProvider.AndroidViewModelFactory factory =
                ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().getApplication());
        mExpenseViewModel = new ViewModelProvider(this, factory).get(ExpenseViewModel.class);

        mExpenseViewModel.init();

        // 4. Theo dõi LiveData (thay 'this' bằng 'getViewLifecycleOwner()')
        mExpenseViewModel.getFilteredExpenses().observe(getViewLifecycleOwner(), items -> {
            // 'items' bây giờ là List<ExpenseWithCategory>
            mAdapter.setData(items);
        });

// 6. ĐĂNG KÝ LAUNCHER (MỚI)
        // Chúng ta đăng ký launcher để nhận kết quả trả về
        mAddExpenseLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // Hàm này được gọi khi AddEditExpenseActivity đóng lại
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // Nhận dữ liệu trả về
                        Intent data = result.getData();
                        if (data != null) {
                            // --- SỬA LỖI CHÍNH TẢ Ở ĐÂY ---
                            // (Đổi "AddEditMFragment" thành "AddEditExpenseActivity")
                            double amount = data.getDoubleExtra(AddEditExpenseActivity.EXTRA_AMOUNT, 0);
                            String categoryId = data.getStringExtra(AddEditExpenseActivity.EXTRA_CATEGORY_ID);
                            long date = data.getLongExtra(AddEditExpenseActivity.EXTRA_DATE, 0);
                            String note = data.getStringExtra(AddEditExpenseActivity.EXTRA_NOTE);

                            // (Không cần lấy sessionManager hay currentUserId ở đây nữa)

                            // --- SỬA LỖI LOGIC UPDATE ---
                            if (data.hasExtra(AddEditExpenseActivity.EXTRA_EXPENSE_ID)) {
                                // --- CHẾ ĐỘ SỬA ---
                                String expenseId = data.getStringExtra(AddEditExpenseActivity.EXTRA_EXPENSE_ID);

                                // Gửi dữ liệu chi tiết cho ViewModel
                                mExpenseViewModel.updateExpenseDetails(expenseId, amount, categoryId, date, note);

                                Toast.makeText(requireContext(), "Đã cập nhật giao dịch!", Toast.LENGTH_SHORT).show();

                            } else {
                                // --- CHẾ ĐỘ THÊM MỚI ---
                                Expense newExpense = new Expense();
                                newExpense.setExpenseId(UUID.randomUUID().toString());
                                // (ViewModel sẽ tự gán userId và createdAt)
                                newExpense.setAmount(amount);
                                newExpense.setCategoryId(categoryId);
                                newExpense.setDate(date);
                                newExpense.setNote(note);

                                mExpenseViewModel.insert(newExpense);
                                Toast.makeText(requireContext(), "Đã lưu chi tiêu!", Toast.LENGTH_SHORT).show();
                            }
                            // --- KẾT THÚC SỬA LỖI ---
                        }
                    } else {
                        Toast.makeText(requireContext(), "Đã hủy", Toast.LENGTH_SHORT).show();
                    }
                });

        // 7. SỬA SỰ KIỆN NHẤN NÚT FAB (CẬP NHẬT)
        mFab.setOnClickListener(fabView -> {
            Intent intent = new Intent(requireContext(), AddEditExpenseActivity.class);
            mAddExpenseLauncher.launch(intent);
        });

        setupSearchView();
    }

    // --- HÀM MỚI ĐỂ CÀI ĐẶT THANH TÌM KIẾM ---
    private void setupSearchView() {
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // Người dùng nhấn "Enter" (ít dùng)
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Người dùng đang gõ
                // Gửi truy vấn mới đến ViewModel
                mExpenseViewModel.setSearchQuery(newText);
                return true;
            }
        });
    }

    private void showDeleteConfirmationDialog(Expense expenseToDelete) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Xác nhận Xóa");
        builder.setMessage("Bạn có chắc chắn muốn xóa giao dịch này không?");

        // Nút "Xóa" (Hành động tích cực)
        builder.setPositiveButton("Xóa", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Người dùng đã xác nhận, gọi ViewModel để xóa
                mExpenseViewModel.delete(expenseToDelete);
                Toast.makeText(requireContext(), "Đã xóa giao dịch", Toast.LENGTH_SHORT).show();
            }
        });

        // Nút "Hủy" (Hành động tiêu cực)
        builder.setNegativeButton("Hủy", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Không làm gì, chỉ đóng hộp thoại
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Tạo và hiển thị hộp thoại
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }


    @Override
    public void onEditClick(Expense expense) {
        Intent intent = new Intent(requireContext(), AddEditExpenseActivity.class);
        intent.putExtra(AddEditExpenseActivity.EXTRA_EXPENSE_ID, expense.getExpenseId());
        mAddExpenseLauncher.launch(intent);
    }

    @Override
    public void onDeleteClick(Expense expense) {
        showDeleteConfirmationDialog(expense);
    }
}