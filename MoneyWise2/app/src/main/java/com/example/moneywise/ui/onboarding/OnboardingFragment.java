package com.example.moneywise.ui.onboarding;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.moneywise.R;

public class OnboardingFragment extends Fragment {

    // Khóa (keys) để lấy dữ liệu từ Bundle arguments
    private static final String ARG_IMAGE_RES = "ARG_IMAGE_RES";
    private static final String ARG_TITLE = "ARG_TITLE";
    private static final String ARG_DESCRIPTION = "ARG_DESCRIPTION";

    /**
     * Hàm 'constructor' tĩnh (static factory method)
     * Dùng hàm này để tạo một slide mới một cách an toàn
     */
    public static OnboardingFragment newInstance(int imageResId) {
        OnboardingFragment fragment = new OnboardingFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_IMAGE_RES, imageResId);
        fragment.setArguments(args); // Gửi dữ liệu vào Fragment
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Gắn layout "fragment_onboarding_slide.xml" vào Fragment này
        return inflater.inflate(R.layout.fragment_onboarding_slide, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Ánh xạ các View từ layout
        ImageView imageView = view.findViewById(R.id.image_view_onboarding_icon);

        // Lấy dữ liệu từ arguments
        if (getArguments() != null) {
            int imageRes = getArguments().getInt(ARG_IMAGE_RES);
            String title = getArguments().getString(ARG_TITLE);
            String description = getArguments().getString(ARG_DESCRIPTION);

            // Đặt dữ liệu lên View
            imageView.setImageResource(imageRes);
        }
    }
}