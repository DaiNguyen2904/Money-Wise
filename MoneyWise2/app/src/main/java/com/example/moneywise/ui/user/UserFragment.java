package com.example.moneywise.ui.user;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.bumptech.glide.Glide;
import com.example.moneywise.MainActivity;
import com.example.moneywise.R;
import com.example.moneywise.repository.MoneyWiseRepository;
import com.example.moneywise.sync.SyncWorker;
import com.example.moneywise.ui.auth.LoginActivity;
import com.example.moneywise.utils.SessionManager;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class UserFragment extends Fragment {

    private SessionManager mSessionManager;
    private FirebaseAuth mAuth;
    private MoneyWiseRepository mRepository;
    private WorkManager mWorkManager;

    // Khai báo các View mới
    private ScrollView mScrollViewLoggedIn;
    private LinearLayout mLayoutLoggedOut;
    private Button mBtnGoogleSignIn;
    private Button mBtnLogout;
    private Button mBtnSaveProfile;
    private ImageView mImageAvatar;
    private TextView mTextChangeAvatar;
    private TextInputEditText mEditTextUserName;
    private TextInputEditText mEditTextUserEmail;
    private TextInputEditText mEditTextUserPhone;
    private TextInputLayout mTextInputLayoutUserPhone;
    private UserViewModel mUserViewModel;
    private ProgressDialog mProgressDialog; // <-- THÊM
    private ActivityResultLauncher<Intent> mImagePickerLauncher; // <-- THÊM

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Khởi tạo các thành phần không thuộc View
        mSessionManager = new SessionManager(requireContext());
        mAuth = FirebaseAuth.getInstance();
        mRepository = new MoneyWiseRepository(requireActivity().getApplication());
        mWorkManager = WorkManager.getInstance(requireContext());

        // --- THÊM MỚI: KHỞI TẠO VIEWMODEL ---
        mUserViewModel = new ViewModelProvider(this).get(UserViewModel.class);

        // --- THÊM MỚI: ĐĂNG KÝ IMAGE PICKER ---
        mImagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            // Tải ảnh lên
                            mUserViewModel.updateAvatar(imageUri);
                        }
                    }
                });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate layout cho fragment này
        return inflater.inflate(R.layout.fragment_user, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Cài đặt ActionBar và Menu
        setupActionBar();
        setupMenu();

        // --- Ánh xạ các View ---
        // Layout chưa đăng nhập
        mLayoutLoggedOut = view.findViewById(R.id.layout_logged_out);
        mBtnGoogleSignIn = view.findViewById(R.id.btn_google_sign_in_fragment);

        // Layout đã đăng nhập (form mới)
        mScrollViewLoggedIn = view.findViewById(R.id.layout_logged_in);
        mBtnLogout = view.findViewById(R.id.btn_logout);
        mBtnSaveProfile = view.findViewById(R.id.button_save_profile);
        mImageAvatar = view.findViewById(R.id.image_user_avatar);
        mTextChangeAvatar = view.findViewById(R.id.text_change_avatar);
        mEditTextUserName = view.findViewById(R.id.edit_text_user_name);
        mEditTextUserEmail = view.findViewById(R.id.edit_text_user_email);
        mEditTextUserPhone = view.findViewById(R.id.edit_text_user_phone);
        mTextInputLayoutUserPhone = view.findViewById(R.id.text_input_layout_user_phone);
        // --- Kết thúc Ánh xạ ---

        // Cập nhật giao diện dựa trên trạng thái đăng nhập
        updateUI();

        // --- Cài đặt sự kiện Click ---
        // --- CÀI ĐẶT CÁC OBSERVER (LẮNG NGHE) TỪ VIEWMODEL ---

        // 1. Lắng nghe trạng thái Loading
        mUserViewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading) {
                showLoadingDialog("Đang xử lý...");
            } else {
                hideLoadingDialog();
            }
        });

        // 2. Lắng nghe thông báo Thành công
        mUserViewModel.getSuccessMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }
        });

        // 3. Lắng nghe thông báo Lỗi
        mUserViewModel.getErrorMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
            }
        });

        // 4. Lắng nghe dữ liệu User (từ Room)
        // (Chúng ta sẽ dùng cái này để điền Tên, SĐT thay vì fUser)
        mUserViewModel.getUserData().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                mEditTextUserName.setText(user.getDisplayName());
                mEditTextUserEmail.setText(user.getEmail()); // Email từ Room/Firestore

                if (user.getPhone() != null && !user.getPhone().isEmpty()) {
                    mEditTextUserPhone.setText(user.getPhone());
                    mTextInputLayoutUserPhone.setPlaceholderText(null);
                } else {
                    mEditTextUserPhone.setText(null);
                    mTextInputLayoutUserPhone.setPlaceholderText("Chưa có số điện thoại");
                }

                // Tải ảnh Avatar (nếu có) bằng Glide
                if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                    Glide.with(this)
                            .load(user.getAvatarUrl())
                            .circleCrop() // Biến ảnh thành hình tròn
                            .into(mImageAvatar);
                } else {
                    mImageAvatar.setImageResource(R.mipmap.ic_launcher_round); // Ảnh mặc định
                }
            }
        });

        // Nút Đăng nhập
        mBtnGoogleSignIn.setOnClickListener(v -> {
            // Chuyển đến LoginActivity để xử lý đăng nhập
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            startActivity(intent);
        });

        // Nút Đăng xuất
        mBtnLogout.setOnClickListener(v -> {
            // 1. Dừng lắng nghe realtime
            mRepository.stopRealtimeSync();
            // 2. Đăng xuất Firebase
            mAuth.signOut();
            // 3. Xóa session cục bộ (sẽ tự động chuyển về LOCAL_USER_ID)
            mSessionManager.logout();

            // 4. Khởi động lại ứng dụng để tải lại CSDL với ID cục bộ
            Intent intent = new Intent(requireContext(), MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            requireActivity().finish();
        });

        // Nút Lưu thay đổi
        mBtnSaveProfile.setOnClickListener(v -> {
            String name = mEditTextUserName.getText().toString();
            String phone = mEditTextUserPhone.getText().toString();

            // Gọi ViewModel
            mUserViewModel.updateProfile(name, phone);
        });

        // CẬP NHẬT: Nút Thay đổi ảnh
        mTextChangeAvatar.setOnClickListener(v -> {
            // Mở trình chọn ảnh
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            mImagePickerLauncher.launch(intent);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Cập nhật lại UI mỗi khi quay lại tab này
        // (Ví dụ: sau khi đăng nhập thành công từ LoginActivity)
        updateUI();

        // Cập nhật lại menu (để ẩn/hiện nút Sync)
        requireActivity().invalidateOptionsMenu();
    }

    private void setupActionBar() {
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity != null && activity.getSupportActionBar() != null) {
            ActionBar actionBar = activity.getSupportActionBar();
            actionBar.setTitle("Tài khoản");
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     * CẬP NHẬT: Chỉ ẩn/hiện layout
     * (Việc điền dữ liệu đã chuyển cho Observer của ViewModel)
     */
    private void updateUI() {
        if (mSessionManager.isLoggedIn()) {
            mLayoutLoggedOut.setVisibility(View.GONE);
            mScrollViewLoggedIn.setVisibility(View.VISIBLE);
        } else {
            mLayoutLoggedOut.setVisibility(View.VISIBLE);
            mScrollViewLoggedIn.setVisibility(View.GONE);
        }
    }

    /**
     * Thêm menu "Đồng bộ ngay" vào Toolbar
     */
    private void setupMenu() {
        MenuHost menuHost = requireActivity();
        menuHost.addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.user_menu, menu);
            }

            @Override
            public void onPrepareMenu(@NonNull Menu menu) {
                // Ẩn nút "Đồng bộ" nếu chưa đăng nhập
                MenuItem syncItem = menu.findItem(R.id.menu_manual_sync);
                if (syncItem != null) {
                    syncItem.setVisible(mSessionManager.isLoggedIn());
                }
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.menu_manual_sync) {
                    // Xử lý sự kiện nhấn nút Đồng bộ
                    Toast.makeText(getContext(), "Đang kích hoạt đồng bộ...", Toast.LENGTH_SHORT).show();
                    triggerImmediateSync();
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }

    /**
     * Kích hoạt SyncWorker chạy 1 lần ngay lập tức
     */
    private void triggerImmediateSync() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest immediateSyncRequest =
                new OneTimeWorkRequest.Builder(SyncWorker.class)
                        .setConstraints(constraints)
                        .build();

        // Dùng enqueueUniqueWork để đảm bảo không bị chồng chéo
        mWorkManager.enqueueUniqueWork(
                "MoneyWiseSyncJob_Immediate", // Tên duy nhất cho công việc 1 lần
                ExistingWorkPolicy.REPLACE, // Thay thế nếu có yêu cầu cũ
                immediateSyncRequest
        );

    }
    // --- THÊM 2 HÀM TIỆN ÍCH CHO LOADING DIALOG ---
    private void showLoadingDialog(String message) {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(requireContext());
            mProgressDialog.setCancelable(false);
        }
        mProgressDialog.setMessage(message);
        mProgressDialog.show();
    }

    private void hideLoadingDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

}