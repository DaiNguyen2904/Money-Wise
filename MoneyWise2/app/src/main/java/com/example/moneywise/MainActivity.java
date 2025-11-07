package com.example.moneywise;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.moneywise.repository.MoneyWiseRepository;
import com.example.moneywise.sync.SyncWorker;
import com.example.moneywise.ui.budgets.BudgetFragment;
import com.example.moneywise.ui.categories.CategoryFragment;
import com.example.moneywise.ui.expenses.ExpenseFragment;
import com.example.moneywise.ui.home.HomeFragment;
import com.example.moneywise.ui.user.UserFragment;
import com.example.moneywise.utils.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity{
    private BottomNavigationView mBottomNav;
    private MoneyWiseRepository mRepository;
    private SessionManager mSessionManager;

    private WorkManager mWorkManager;

    private Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Layout chính (chứa container)

        mRepository = new MoneyWiseRepository(getApplication());
        mSessionManager = new SessionManager(this);

        String userId = mSessionManager.getUserId();
        if (userId != null) {
            Log.d("MainActivity", "Bắt đầu lắng nghe thời gian thực cho user: " + userId);
            mRepository.startRealtimeSync(userId);
        }

        mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        mBottomNav = findViewById(R.id.bottom_navigation);
        setupBottomNavigation();

        // Tải Fragment mặc định khi mở ứng dụng
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
            mBottomNav.setSelectedItemId(R.id.nav_home);

            // --- BƯỚC 2: CÀI ĐẶT TIÊU ĐỀ CHO LẦN ĐẦU  ---
            if(getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Trang chủ");
                getSupportActionBar().setDisplayHomeAsUpEnabled(false); // Trang chủ không có nút back
            }
            // -------------------------------------------------------
        }

        mWorkManager = WorkManager.getInstance(getApplicationContext());

        // Lên lịch đồng bộ định kỳ
        schedulePeriodicSync();

    }

    @Override
    public boolean onSupportNavigateUp() {
        // Khi nhấn nút "Up" (mũi tên quay lại) trên Toolbar
        // Quay về HomeFragment và chọn icon Home trên BottomNav
        loadFragment(new HomeFragment());
        mBottomNav.setSelectedItemId(R.id.nav_home);
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mRepository != null) {
            mRepository.stopRealtimeSync();
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

            // SẮP XẾP LẠI VÀ BỔ SUNG
            if (itemId == R.id.nav_home) { // MỚI
                loadFragment(new HomeFragment());
                return true;
            } else if (itemId == R.id.nav_expenses) {
                loadFragment(new ExpenseFragment());
                return true;
            } else if (itemId == R.id.nav_budgets) {
                loadFragment(new BudgetFragment());
                return true;
            } else if (itemId == R.id.nav_categories) {
                loadFragment(new CategoryFragment());
                return true;
            } else if (itemId == R.id.nav_user) { // MỚI
                loadFragment(new UserFragment());
                return true;
            }
            return false;
        });
    }

    private void schedulePeriodicSync() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest syncRequest =
                new PeriodicWorkRequest.Builder(
                        SyncWorker.class,
                        1, TimeUnit.HOURS) // Bạn có thể tăng thời gian này lên 3-6 tiếng
                        .setConstraints(constraints)
                        .setInitialDelay(15, TimeUnit.SECONDS)
                        .build();

        mWorkManager.enqueueUniquePeriodicWork(
                "MoneyWiseSyncJob", // Tên duy nhất cho công việc định kỳ
                ExistingPeriodicWorkPolicy.KEEP, // Giữ lại nếu đã có
                syncRequest
        );

        Log.d("MainActivity", "Đã lên lịch đồng bộ định kỳ.");
    }
}