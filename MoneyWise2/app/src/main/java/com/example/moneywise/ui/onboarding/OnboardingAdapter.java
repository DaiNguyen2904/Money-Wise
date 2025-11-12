package com.example.moneywise.ui.onboarding;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.moneywise.R;

public class OnboardingAdapter extends FragmentStateAdapter {

    // Định nghĩa nội dung cho 3 slide
    private static final int NUM_PAGES = 3;

    // (Chúng ta sẽ dùng các icon có sẵn trong drawable của bạn)
    private static final int[] IMAGE_RES_IDS = new int[]{
            R.drawable.anh_gioi_thieu_mot, // (Bạn có thể đổi thành logo)
            R.drawable.anh_gioi_thieu_hai, //
            R.drawable.anh_gioi_thieu_ba // (Minh họa cho 1 danh mục ngân sách)
    };


    public OnboardingAdapter(FragmentActivity fa) {
        super(fa);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // Dựa vào vị trí (position), tạo ra Fragment tương ứng
        // dùng hàm newInstance() chúng ta đã tạo ở Bước 3
        return OnboardingFragment.newInstance(
                IMAGE_RES_IDS[position]
        );
    }

    @Override
    public int getItemCount() {
        // Trả về tổng số lượng slide
        return NUM_PAGES;
    }
}