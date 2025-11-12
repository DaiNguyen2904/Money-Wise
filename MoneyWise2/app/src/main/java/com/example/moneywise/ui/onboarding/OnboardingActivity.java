package com.example.moneywise.ui.onboarding;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.moneywise.R;
import com.example.moneywise.ui.auth.LoginActivity;
import com.example.moneywise.utils.SessionManager;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class OnboardingActivity extends AppCompatActivity {

    private ViewPager2 mViewPager;
    private OnboardingAdapter mAdapter;
    private TabLayout mTabLayout;
    private Button mStartButton;
    private SessionManager mSessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSessionManager = new SessionManager(this);

        // --- BƯỚC 1: KIỂM TRA XEM ĐÃ XEM ONBOARDING CHƯA ---
        // Nếu đã xem rồi, bỏ qua Activity này và đi thẳng đến Login
        if (mSessionManager.hasSeenOnboarding()) {
            goToLoginActivity();
            return; // Dừng hàm onCreate tại đây
        }
        // --- KẾT THÚC BƯỚC 1 ---


        // Nếu chưa xem, thì mới hiển thị layout
        setContentView(R.layout.activity_onboarding);

        // Ẩn ActionBar (Toolbar) nếu có, để hiển thị toàn màn hình
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // --- BƯỚC 2: ÁNH XẠ VÀ CÀI ĐẶT VIEWS ---
        mViewPager = findViewById(R.id.view_pager_onboarding);
        mTabLayout = findViewById(R.id.tab_layout_indicator);
        mStartButton = findViewById(R.id.button_start_login);

        // Khởi tạo Adapter
        mAdapter = new OnboardingAdapter(this);
        mViewPager.setAdapter(mAdapter);

        // Liên kết ViewPager2 (khung lướt) với TabLayout (dấu chấm)
        new TabLayoutMediator(mTabLayout, mViewPager, (tab, position) -> {
            tab.setIcon(R.drawable.onboarding_tab_item);
        }).attach();


        // --- BƯỚC 3: XỬ LÝ SỰ KIỆN NHẤN NÚT ---
        mStartButton.setOnClickListener(view -> {
            // 1. Đánh dấu là đã xem Onboarding
            mSessionManager.setHasSeenOnboarding();

            // 2. Chuyển đến màn hình Đăng nhập
            goToLoginActivity();
        });
    }

    /**
     * Hàm tiện ích để chuyển sang LoginActivity và tự hủy (finish)
     */
    private void goToLoginActivity() {
        Intent intent = new Intent(OnboardingActivity.this, LoginActivity.class);
        startActivity(intent);
        finish(); // Đóng OnboardingActivity để người dùng không thể "Back" lại
    }
}