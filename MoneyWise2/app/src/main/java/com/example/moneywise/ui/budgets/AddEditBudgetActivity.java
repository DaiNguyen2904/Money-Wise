package com.example.moneywise.ui.budgets;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.moneywise.R;
import com.example.moneywise.data.entity.Category;
import com.example.moneywise.data.entity.Expense;
import com.example.moneywise.ui.budgets.AddEditBudgetViewModel;
import com.example.moneywise.ui.categories.CategoryActivity;
import com.google.android.material.textfield.TextInputEditText;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AddEditBudgetActivity extends AppCompatActivity {

    // Khai báo hằng số cho Intent
    public static final String EXTRA_BUDGET_ID = "EXTRA_BUDGET_ID";
    public static final String EXTRA_BUDGET_CATEGORY_ID = "EXTRA_BUDGET_CATEGORY_ID";
    public static final String EXTRA_BUDGET_AMOUNT = "EXTRA_BUDGET_AMOUNT";
    private static final List<String> PRESET_CATEGORY_NAMES = Arrays.asList(
            "Ăn uống", "Phương tiện", "Giải trí",
            "Nhà ở", "Y tế", "Học tập",
            "Mỹ phẩm", "Quần áo", "Khác" // "Khác" này sẽ là nút thứ 9
    );

    private TextInputEditText mEditTextAmount;
    private GridLayout mGridCategories;
    private LinearLayout mBtnOtherCategory;
    private Button mButtonSave;
    /** Map để lưu 9 View của nút, dùng để highlight */
    private Map<String, View> mPresetButtonMap = new HashMap<>();
    /** Map để lưu Category ID của 9 nút */
    private Map<String, String> mPresetIdMap = new HashMap<>();
    private String mSelectedCategoryId = null; // ID của danh mục đang được chọn
    private View mSelectedButtonView = null; // View của nút đang được chọn
    private Drawable mDefaultButtonBackground; // Lưu background mặc định
    private ActivityResultLauncher<Intent> mPickCategoryLauncher;
    // Khai báo Views


    private AddEditBudgetViewModel mViewModel;
    private String mCurrentBudgetId; // Lưu ID khi ở chế độ Sửa
    private boolean isEditMode = false;
    private boolean isCategoryListLoaded = false; // Cờ để xử lý Spinner
    private boolean isBudgetDataLoaded = false; // Cờ để xử lý Spinner


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_budget);

        // Ánh xạ Views
        mEditTextAmount = findViewById(R.id.edit_text_amount_budget);
        mGridCategories = findViewById(R.id.grid_layout_categories_budget); // <-- CẬP NHẬT
        mBtnOtherCategory = findViewById(R.id.cat_btn_other_budget); // <-- CẬP NHẬT

        mButtonSave = findViewById(R.id.button_save);


        // Lấy ViewModel
        ViewModelProvider.AndroidViewModelFactory factory =
                ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication());
        mViewModel = new ViewModelProvider(this, factory).get(AddEditBudgetViewModel.class);

        registerPickCategoryLauncher();
        setupCategoryGrid(); // (Sẽ sửa hàm này)
        // XÓA: setupDatePicker();
        setupSaveButton();

        // 1. KIỂM TRA INTENT ĐỂ XEM LÀ "THÊM" HAY "SỬA"
        Intent intent = getIntent();
        if (intent.hasExtra(EXTRA_BUDGET_ID)) {
//            // Đây là chế độ "Sửa"
//            isEditMode = true;
//            mCurrentExpenseId = intent.getStringExtra(EXTRA_EXPENSE_ID);
//
//            if (getSupportActionBar() != null) {
//                getSupportActionBar().setTitle("Sửa Giao dịch");
//            }
//
//            // Yêu cầu ViewModel tải dữ liệu của Giao dịch này
//            mViewModel.loadExpense(mCurrentExpenseId);
//
//            // 2. THEO DÕI DỮ LIỆU GIAO DỊCH
//            observeLoadedExpense();
//
//        } else {
//            // Đây là chế độ "Thêm mới"
//            isEditMode = false;
//            if (getSupportActionBar() != null) {
//                getSupportActionBar().setTitle("Thêm Giao dịch mới");
//            }
        }

        // (Thêm nút Back cho ActionBar)
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }


    private void setupSaveButton() {
        mButtonSave.setOnClickListener(view -> {
            saveBudget();
        });
    }

    private void saveBudget() {
        // 1. Validation (Kiểm tra dữ liệu)
        String amountString = mEditTextAmount.getText().toString();
        if (amountString.isEmpty()) {
            mEditTextAmount.setError("Vui lòng nhập số tiền");
            mEditTextAmount.requestFocus();
            return;
        }

        if (mSelectedCategoryId == null) {
            Toast.makeText(this, "Vui lòng chọn một danh mục", Toast.LENGTH_SHORT).show();
            return; // Ngăn không cho lưu
        }

        // 2. Lấy dữ liệu
        double amount = Double.parseDouble(amountString);
        String categoryId = mSelectedCategoryId;

        Intent replyIntent = new Intent();
        replyIntent.putExtra(EXTRA_BUDGET_CATEGORY_ID, categoryId);
        replyIntent.putExtra(EXTRA_BUDGET_AMOUNT, amount);

        // --- KẾT THÚC THÊM MỚI ---

        // 4. Gửi kết quả (như cũ)
        setResult(RESULT_OK, replyIntent);
        finish();
    }

    /**
     * HÀM MỚI: Đăng ký Launcher cho nút "Khác"
     */
    private void registerPickCategoryLauncher() {
        mPickCategoryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        String categoryId = result.getData().getStringExtra(CategoryActivity.EXTRA_SELECTED_CATEGORY_ID);
                        if (categoryId != null) {
                            // Người dùng đã chọn một mục từ danh sách "Khác"
                            // Chúng ta sẽ highlight nút "Khác"
                            selectCategoryButton(categoryId, mBtnOtherCategory);
                        }
                    }
                });
    }

    /**
     * HÀM MỚI (THAY THẾ setupCategorySpinner)
     */
    private void setupCategoryGrid() {
        // Ánh xạ 9 nút đầu tiên
        mPresetButtonMap.put(PRESET_CATEGORY_NAMES.get(0), findViewById(R.id.cat_btn_1_budget));
        mPresetButtonMap.put(PRESET_CATEGORY_NAMES.get(1), findViewById(R.id.cat_btn_2_budget));
        mPresetButtonMap.put(PRESET_CATEGORY_NAMES.get(2), findViewById(R.id.cat_btn_3_budget));
        mPresetButtonMap.put(PRESET_CATEGORY_NAMES.get(3), findViewById(R.id.cat_btn_4_budget));
        mPresetButtonMap.put(PRESET_CATEGORY_NAMES.get(4), findViewById(R.id.cat_btn_5_budget));
        mPresetButtonMap.put(PRESET_CATEGORY_NAMES.get(5), findViewById(R.id.cat_btn_6_budget));
        mPresetButtonMap.put(PRESET_CATEGORY_NAMES.get(6), findViewById(R.id.cat_btn_7_budget));
        mPresetButtonMap.put(PRESET_CATEGORY_NAMES.get(7), findViewById(R.id.cat_btn_8_budget));
        mPresetButtonMap.put(PRESET_CATEGORY_NAMES.get(8), findViewById(R.id.cat_btn_other_budget)); // Nút "Khác" của bạn (số 9)

        // Lưu background mặc định để reset
        mDefaultButtonBackground = mPresetButtonMap.get(PRESET_CATEGORY_NAMES.get(0)).getBackground();

        // Gán sự kiện cho nút "Khác" thật (nút thứ 10)
        mBtnOtherCategory.setOnClickListener(v -> {
            Intent intent = new Intent(AddEditBudgetActivity.this, CategoryActivity.class);
            intent.putExtra(CategoryActivity.EXTRA_PICK_MODE, true); // Chạy ở chế độ "Chọn"
            mPickCategoryLauncher.launch(intent);
        });

        // Lấy danh sách TẤT CẢ danh mục từ ViewModel
        mViewModel.getAllCategories().observe(this, categories -> {
            // 1. Tạo Map để tra cứu Category bằng Tên
            Map<String, Category> categoryNameMap = new HashMap<>();
            for (Category cat : categories) {
                categoryNameMap.put(cat.name, cat);
            }

            // 2. Cấu hình 9 nút mặc định
            for (int i = 0; i < 8; i++) {

                String presetName = PRESET_CATEGORY_NAMES.get(i);
                View buttonView = mPresetButtonMap.get(presetName);
                Category category = categoryNameMap.get(presetName); // Tìm danh mục khớp tên

                if (category != null) {
                    // TÌM THẤY: Cấu hình nút
                    buttonView.setVisibility(View.VISIBLE);

                    // Ánh xạ icon và text bên trong nút
                    ImageView icon = buttonView.findViewById(R.id.image_category_icon);
                    TextView name = buttonView.findViewById(R.id.text_category_name);

                    name.setText(category.name);

                    int iconResId = getIconResource(category.icon);
                    if (iconResId != 0) { // Nếu tìm thấy icon
                        icon.setImageResource(iconResId);
                    }

                    // Lưu ID để dùng sau
                    mPresetIdMap.put(presetName, category.categoryId);

                    // Gán sự kiện
                    buttonView.setOnClickListener(v ->
                            selectCategoryButton(category.categoryId, buttonView)
                    );

                } else {
                    // KHÔNG TÌM THẤY (ví dụ: người dùng đã xóa "Ăn uống")
                    buttonView.setVisibility(View.GONE);
                }
            }
            // 3. XỬ LÝ NÚT THỨ 9 ("Khác" - R.id.cat_btn_other) RIÊNG BIỆT
            String otherPresetName = PRESET_CATEGORY_NAMES.get(8); // Tên là "Khác"
            View otherButtonView = mPresetButtonMap.get(otherPresetName); // View là R.id.cat_btn_other
            Category otherCategory = categoryNameMap.get(otherPresetName); // Category "Khác"

            if (otherCategory != null) {
                otherButtonView.setVisibility(View.VISIBLE);
                // Chỉ lưu ID của category "Khác" vào Map
                // (Để hàm trySetSpinnerSelection() có thể tìm và highlight nút này)
                mPresetIdMap.put(otherPresetName, otherCategory.categoryId);

                // **QUAN TRỌNG:**
                // Chúng ta KHÔNG cấu hình icon/text (vì layout khác, sẽ crash)
                // Chúng ta KHÔNG gán OnClickListener (vì Dòng 311 đã gán listener MỞ DANH SÁCH)
            } else {
                // Không tìm thấy category "Khác" trong DB
                otherButtonView.setVisibility(View.GONE);
            }

            // (Phần này để xử lý chế độ Sửa, giống như cũ)
            isCategoryListLoaded = true; // Dùng lại cờ này

        });
    }

    /**
     * HÀM MỚI: Xử lý khi nhấn 1 nút danh mục
     */
    private void selectCategoryButton(String categoryId, View buttonView) {
        // 1. Xóa highlight khỏi nút cũ (nếu có)
        if (mSelectedButtonView != null) {
            mSelectedButtonView.setBackground(mDefaultButtonBackground);
        }

        // 2. Highlight nút mới
        buttonView.setBackgroundColor(Color.LTGRAY); // (Bạn có thể dùng màu đẹp hơn)

        // 3. Lưu trạng thái
        mSelectedButtonView = buttonView;
        mSelectedCategoryId = categoryId;
    }

    private int getIconResource(String iconName) {
        if (iconName == null || iconName.isEmpty()) {
            return R.drawable.ic_launcher_background; // Icon mặc định nếu lỗi
        }
        try {
            // Lấy ID tài nguyên từ tên String
            return getResources().getIdentifier(iconName, "drawable", getPackageName());
        } catch (Exception e) {
            e.printStackTrace();
            return R.drawable.ic_launcher_background; // Icon mặc định nếu lỗi
        }
    }

    // (Thêm hàm này để xử lý nút Back trên ActionBar)
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}