package com.example.moneywise;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.moneywise.ui.budgets.BudgetFragment;
import com.example.moneywise.ui.categories.CategoryFragment;
import com.example.moneywise.ui.expenses.ExpenseFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity{
    private BottomNavigationView mBottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Layout chính (chứa container)

        mBottomNav = findViewById(R.id.bottom_navigation);
        setupBottomNavigation();

        // Tải Fragment mặc định khi mở ứng dụng
        if (savedInstanceState == null) {
            loadFragment(new ExpenseFragment());
        }
    }

    /**
     * HÀM MỚI: Dùng để thay thế Fragment
     */
    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    /**
     * CẬP NHẬT: setupBottomNavigation (Không dùng startActivity)
     */
    private void setupBottomNavigation() {
        mBottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_expenses) {
                loadFragment(new ExpenseFragment());
                return true;
            } else if (itemId == R.id.nav_budgets) {
                loadFragment(new BudgetFragment()); // <-- SỬA
                return true;
            } else if (itemId == R.id.nav_categories) {
                loadFragment(new CategoryFragment()); // <-- SỬA
                return true;
            }
            return false;
        });

        // (Nếu bạn muốn tránh tải lại Fragment khi nhấn lại,
        //  logic sẽ phức tạp hơn, nhưng tạm thời như vậy là "mượt")
    }

    // --- XÓA TẤT CẢ LOGIC CŨ CỦA EXPENSE (đã chuyển sang Fragment) ---
    // Xóa: mExpenseViewModel, mAdapter, mListView, mFab, mAddExpenseLauncher
    // Xóa: showDeleteConfirmationDialog, onEditClick, onDeleteClick
}