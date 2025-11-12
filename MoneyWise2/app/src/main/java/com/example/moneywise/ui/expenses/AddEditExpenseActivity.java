package com.example.moneywise.ui.expenses;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import com.example.moneywise.R;
import com.example.moneywise.data.entity.Category;
import com.example.moneywise.data.entity.Expense;
import com.example.moneywise.repository.MoneyWiseRepository;
import com.example.moneywise.ui.categories.CategoryActivity;
import com.example.moneywise.utils.SessionManager;
import com.google.android.material.textfield.TextInputEditText;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class AddEditExpenseActivity extends AppCompatActivity {

    // Khai báo hằng số cho Intent
    public static final String EXTRA_AMOUNT = "com.tenban.moneywise.EXTRA_AMOUNT";
    public static final String EXTRA_CATEGORY_ID = "com.tenban.moneywise.EXTRA_CATEGORY_ID";
    public static final String EXTRA_DATE = "com.tenban.moneywise.EXTRA_DATE";
    public static final String EXTRA_NOTE = "com.tenban.moneywise.EXTRA_NOTE";

    public static final String EXTRA_EXPENSE_ID = "com.tenban.moneywise.EXTRA_EXPENSE_ID";
    private static final List<String> PRESET_CATEGORY_NAMES = Arrays.asList(
            "Ăn uống", "Phương tiện", "Giải trí",
            "Nhà ở", "Y tế", "Học tập",
            "Mỹ phẩm", "Quần áo", "Khác" // "Khác" này sẽ là nút thứ 9
    );
    /** Map để lưu 9 View của nút, dùng để highlight */
    private Map<String, View> mPresetButtonMap = new HashMap<>();
    /** Map để lưu Category ID của 9 nút */
    private Map<String, String> mPresetIdMap = new HashMap<>();
    private String mSelectedCategoryId = null; // ID của danh mục đang được chọn
    private View mSelectedButtonView = null; // View của nút đang được chọn
    private Drawable mDefaultButtonBackground; // Lưu background mặc định
    // Khai báo Views
    private TextInputEditText mEditTextAmount;
    private GridLayout mGridCategories; // <-- THÊM DÒNG NÀY
    private LinearLayout mBtnOtherCategory; // <-- THÊM DÒNG NÀY
    private Button mButtonSelectDate;
    private TextInputEditText mEditTextNote;
    private Button mButtonSave;

    private AddEditExpenseViewModel mViewModel;

    private long mSelectedDateTimestamp; // Để lưu ngày đã chọn
    private Calendar mCalendar;
    private String mCurrentExpenseId; // Lưu ID khi ở chế độ Sửa
    private boolean isEditMode = false;
    private boolean isCategoryListLoaded = false; // Cờ để xử lý Spinner
    private boolean isExpenseDataLoaded = false; // Cờ để xử lý Spinner

    private Toolbar mToolbar;

    private MoneyWiseRepository mRepository;

    private String currentUserId;

    private TextView mTextViewSelectedCategoryName;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_expense);

        // Ánh xạ Views
        mEditTextAmount = findViewById(R.id.edit_text_amount);
        mGridCategories = findViewById(R.id.grid_layout_categories); // <-- CẬP NHẬT
        mBtnOtherCategory = findViewById(R.id.cat_btn_other); // <-- CẬP NHẬT

        mButtonSelectDate = findViewById(R.id.button_select_date);
        mEditTextNote = findViewById(R.id.edit_text_note);
        mButtonSave = findViewById(R.id.button_save);
        mToolbar = findViewById(R.id.toolbar_add_edit_expense);
        mTextViewSelectedCategoryName = findViewById(R.id.text_view_selected_category_name);
        setSupportActionBar(mToolbar);

        // Khởi tạo
        mCalendar = Calendar.getInstance();
        mSelectedDateTimestamp = mCalendar.getTimeInMillis(); // Mặc định là hôm nay
        updateDateButtonText();

        mRepository = new MoneyWiseRepository(getApplication());
        currentUserId = new SessionManager(getApplication()).getUserId();



        // Lấy ViewModel
        ViewModelProvider.AndroidViewModelFactory factory =
                ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication());
        mViewModel = new ViewModelProvider(this, factory).get(AddEditExpenseViewModel.class);

        // Cài đặt các thành phần
//        registerPickCategoryLauncher();
        setupCategoryGrid();
        setupDatePicker();
        setupSaveButton();
        mTextViewSelectedCategoryName.setVisibility(View.GONE);

        // 1. KIỂM TRA INTENT ĐỂ XEM LÀ "THÊM" HAY "SỬA"
        Intent intent = getIntent();
        if (intent.hasExtra(EXTRA_EXPENSE_ID)) {
            // Đây là chế độ "Sửa"
            isEditMode = true;
            mCurrentExpenseId = intent.getStringExtra(EXTRA_EXPENSE_ID);

            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Sửa Giao dịch");
            }

            // Yêu cầu ViewModel tải dữ liệu của Giao dịch này
            mViewModel.loadExpense(mCurrentExpenseId);

            // 2. THEO DÕI DỮ LIỆU GIAO DỊCH
            observeLoadedExpense();

        } else {
            // Đây là chế độ "Thêm mới"
            isEditMode = false;
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Thêm Giao dịch mới");
            }
        }

        // (Thêm nút Back cho ActionBar)
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     * Hàm mới: Theo dõi LiveData của Giao dịch đang được sửa
     */
    private void observeLoadedExpense() {
        mViewModel.getLoadedExpense().observe(this, expense -> {
            if (expense != null) {
                // Điền dữ liệu Giao dịch vào Form
                mEditTextAmount.setText(String.valueOf(expense.getAmount()));
                mEditTextNote.setText(expense.getNote());

                // Cập nhật Calendar và nút Ngày
                mCalendar.setTimeInMillis(expense.getDate());
                mSelectedDateTimestamp = expense.getDate();
                updateDateButtonText();

                isExpenseDataLoaded = true;
                // Cố gắng cập nhật Spinner (nếu danh mục cũng đã tải xong)
                trySetSpinnerSelection(expense.getCategoryId());
            }
        });
    }


    /**
     * Hàm mới: Chỉ chọn Spinner KHI CẢ 2 nguồn dữ liệu đã sẵn sàng
     */
    /**
     * HÀM ĐƯỢC CẬP NHẬT:
     * Cố gắng chọn nút, nếu không tìm thấy trong 8 nút đầu -> chọn nút "Khác"
     */
    private void trySetSpinnerSelection(String categoryIdToSelect) {
        if (!isCategoryListLoaded || !isExpenseDataLoaded) {
            return; // Chưa sẵn sàng, chờ
        }

        if (categoryIdToSelect == null) {
            // Lấy từ LiveData (nếu hàm này được gọi từ observe(categories))
            Expense expense = mViewModel.getLoadedExpense().getValue();
            if (expense != null) categoryIdToSelect = expense.getCategoryId();
            else return; // Không có gì để chọn
        }

        // 1. Kiểm tra xem ID có nằm trong 8 nút mặc định không
        String selectedPresetName = null;
        for (Map.Entry<String, String> entry : mPresetIdMap.entrySet()) {
            // (Lưu ý: mPresetIdMap giờ chỉ chứa 8 nút)
            if (entry.getValue().equals(categoryIdToSelect)) {
                selectedPresetName = entry.getKey();
                break;
            }
        }

        if (selectedPresetName != null) {
            // 2. Nằm trong 8 nút -> Highlight nút đó
            View buttonToSelect = mPresetButtonMap.get(selectedPresetName);
            if (buttonToSelect != null) {
                selectCategoryButton(categoryIdToSelect, buttonToSelect);
            }
        } else {
            // 3. KHÔNG nằm trong 8 nút -> Đây là danh mục "Khác"
            // Highlight nút "Khác"
            selectCategoryButton(categoryIdToSelect, mBtnOtherCategory);

            // Lấy tên danh mục đó và hiển thị
            String finalCategoryIdToSelect = categoryIdToSelect;
            mViewModel.getAllCategories().observe(this, categories -> {
                for (Category cat : categories) {
                    if (cat.getCategoryId().equals(finalCategoryIdToSelect)) {
                        TextView otherText = mBtnOtherCategory.findViewById(R.id.text_category_name);
                        if (otherText != null) {
                            otherText.setText(cat.getName());
                        }
                        break;
                    }
                }
            });
        }
    }

    private void setupDatePicker() {
        DatePickerDialog.OnDateSetListener dateSetListener = (view, year, month, dayOfMonth) -> {
            mCalendar.set(Calendar.YEAR, year);
            mCalendar.set(Calendar.MONTH, month);
            mCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            mSelectedDateTimestamp = mCalendar.getTimeInMillis();
            updateDateButtonText();
        };

        mButtonSelectDate.setOnClickListener(view -> {
            new DatePickerDialog(this, dateSetListener,
                    mCalendar.get(Calendar.YEAR),
                    mCalendar.get(Calendar.MONTH),
                    mCalendar.get(Calendar.DAY_OF_MONTH))
                    .show();
        });
    }

    private void updateDateButtonText() {
        // Định dạng ngày thành chuỗi (ví dụ: "Thứ 4, 05/11/2025")
        SimpleDateFormat sdf = new SimpleDateFormat("E, dd/MM/yyyy", new Locale("vi", "VN"));
        mButtonSelectDate.setText(sdf.format(mSelectedDateTimestamp));
    }

    private void setupSaveButton() {
        mButtonSave.setOnClickListener(view -> {
            saveExpense();
        });
    }

    private void saveExpense() {
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
        String note = mEditTextNote.getText().toString();
        // Lấy categoryId (đã được lưu trong mSelectedCategoryId)
        String categoryId = mSelectedCategoryId;



        // 3. Đóng gói dữ liệu vào Intent để gửi về
        Intent replyIntent = new Intent();
        replyIntent.putExtra(EXTRA_AMOUNT, amount);
        replyIntent.putExtra(EXTRA_CATEGORY_ID, categoryId);
        replyIntent.putExtra(EXTRA_DATE, mSelectedDateTimestamp);
        replyIntent.putExtra(EXTRA_NOTE, note);

        // Nếu là chế độ Sửa, gửi KÈM ID
        if (isEditMode) {
            replyIntent.putExtra(EXTRA_EXPENSE_ID, mCurrentExpenseId);
        }
        // --- KẾT THÚC THÊM MỚI ---

        // 4. Gửi kết quả (như cũ)
        setResult(RESULT_OK, replyIntent);
        finish();
    }

    /**
     * HÀM MỚI: Đăng ký Launcher cho nút "Khác"
     */
//    private void registerPickCategoryLauncher() {
//        mPickCategoryLauncher = registerForActivityResult(
//                new ActivityResultContracts.StartActivityForResult(),
//                result -> {
//                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
//                        String categoryId = result.getData().getStringExtra(CategoryActivity.EXTRA_SELECTED_CATEGORY_ID);
//                        if (categoryId != null) {
//                            // Người dùng đã chọn một mục từ danh sách "Khác"
//                            // Chúng ta sẽ highlight nút "Khác"
//                            selectCategoryButton(categoryId, mBtnOtherCategory);
//                        }
//                    }
//                });
//    }

    /**
     * HÀM MỚI (THAY THẾ setupCategorySpinner)
     */
    private void setupCategoryGrid() {
        // Ánh xạ 9 nút đầu tiên
        mPresetButtonMap.put(PRESET_CATEGORY_NAMES.get(0), findViewById(R.id.cat_btn_1));
        mPresetButtonMap.put(PRESET_CATEGORY_NAMES.get(1), findViewById(R.id.cat_btn_2));
        mPresetButtonMap.put(PRESET_CATEGORY_NAMES.get(2), findViewById(R.id.cat_btn_3));
        mPresetButtonMap.put(PRESET_CATEGORY_NAMES.get(3), findViewById(R.id.cat_btn_4));
        mPresetButtonMap.put(PRESET_CATEGORY_NAMES.get(4), findViewById(R.id.cat_btn_5));
        mPresetButtonMap.put(PRESET_CATEGORY_NAMES.get(5), findViewById(R.id.cat_btn_6));
        mPresetButtonMap.put(PRESET_CATEGORY_NAMES.get(6), findViewById(R.id.cat_btn_7));
        mPresetButtonMap.put(PRESET_CATEGORY_NAMES.get(7), findViewById(R.id.cat_btn_8));


        // Lưu background mặc định để reset
        mDefaultButtonBackground = mPresetButtonMap.get(PRESET_CATEGORY_NAMES.get(0)).getBackground();

        mBtnOtherCategory.setOnClickListener(v -> {
            showCreateCategoryDialog();
        });

        // Lấy danh sách TẤT CẢ danh mục từ ViewModel
        mViewModel.getAllCategories().observe(this, categories -> {
            // 1. Tạo Map để tra cứu Category bằng Tên
            Map<String, Category> categoryNameMap = new HashMap<>();
            for (Category cat : categories) {
                categoryNameMap.put(cat.getName(), cat);
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

                    name.setText(category.getName());

                    int iconResId = getIconResource(category.getIcon());
                    if (iconResId != 0) { // Nếu tìm thấy icon
                        icon.setImageResource(iconResId);
                    }

                    // Lưu ID để dùng sau
                    mPresetIdMap.put(presetName, category.getCategoryId());

                    // Gán sự kiện
                    buttonView.setOnClickListener(v ->
                            selectCategoryButton(category.getCategoryId(), buttonView)
                    );

                } else {
                    // KHÔNG TÌM THẤY (ví dụ: người dùng đã xóa "Ăn uống")
                    buttonView.setVisibility(View.GONE);
                }
            }

            // (Phần này để xử lý chế độ Sửa, giống như cũ)
            isCategoryListLoaded = true; // Dùng lại cờ này
            trySetSpinnerSelection(null); // Gọi hàm này để check
        });
    }

    /**
     * HÀM MỚI: Tạo một danh mục mới và lưu vào CSDL.
     * Trả về đối tượng Category vừa tạo để UI có thể sử dụng ngay lập tức.
     */
    public Category insertNewCategory(String categoryName) {
        Category newCategory = new Category();
        newCategory.setCategoryId(UUID.randomUUID().toString());
        newCategory.setUserId(currentUserId);
        newCategory.setName(categoryName);
        newCategory.setIcon("ic_other"); // Icon mặc định
        newCategory.setColor("#808080"); // Màu mặc định
        newCategory.setIsDefault(0); // Không phải mặc định
        newCategory.setCreatedAt(System.currentTimeMillis());
        // (synced = 0 sẽ được Repo tự động xử lý)

        mRepository.insertCategory(newCategory); // Chạy async

        return newCategory; // Trả về đối tượng ngay cho UI
    }

    /**
     * HÀM MỚI: Xử lý khi nhấn 1 nút danh mục
     */
    /**
     * HÀM MỚI: Xử lý khi nhấn 1 nút danh mục
     */
    private void selectCategoryButton(String categoryId, View buttonView) {
        // 1. Xóa highlight khỏi nút cũ (nếu có)
        if (mSelectedButtonView != null) {
            mSelectedButtonView.setBackground(mDefaultButtonBackground);
            // Nếu nút cũ là nút "Khác", reset text của nó
            if (mSelectedButtonView == mBtnOtherCategory) {
                TextView otherText = mBtnOtherCategory.findViewById(R.id.text_category_name);
                if (otherText != null) {
                    otherText.setText("Khác"); // Reset về chữ "Khác"
                }
            }
        }

        // 2. Highlight nút mới
        buttonView.setBackgroundColor(Color.LTGRAY); // (Bạn có thể dùng màu đẹp hơn)

        // 3. Lưu trạng thái
        mSelectedButtonView = buttonView;
        mSelectedCategoryId = categoryId;

        // 4. Cập nhật lên Text View
        if (categoryId == null) {
            mTextViewSelectedCategoryName.setVisibility(View.GONE);
        } else {
            // Chúng ta cần lấy tên danh mục từ ID
            // Chúng ta lắng nghe LiveData để tìm tên
            mViewModel.getAllCategories().observe(this, categories -> {
                if (categories == null) return;
                for (Category cat : categories) {
                    if (cat.getCategoryId().equals(mSelectedCategoryId)) {
                        mTextViewSelectedCategoryName.setText(cat.getName());
                        mTextViewSelectedCategoryName.setVisibility(View.VISIBLE);

                        // Cập nhật text của nút "Khác" (nếu nó được chọn)
                        if (mSelectedButtonView == mBtnOtherCategory) {
                            TextView otherText = mBtnOtherCategory.findViewById(R.id.text_category_name);
                            if (otherText != null) {
                                otherText.setText(cat.getName());
                            }
                        }
                        break;
                    }
                }
            });
        }

    }

    /**
     * HÀM MỚI: Hiển thị Dialog để tạo Category
     */
    private void showCreateCategoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Tạo Danh mục mới");

        // Set up the input
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        input.setHint("Tên danh mục (ví dụ: Du lịch)");

        // Thêm padding cho EditText
        LinearLayout container = new LinearLayout(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        int padding = (int) (16 * getResources().getDisplayMetrics().density); // 16dp
        container.setPadding(padding, padding / 2, padding, padding / 2);
        container.addView(input, lp);
        builder.setView(container);

        // Set up the buttons
        builder.setPositiveButton("Lưu", (dialog, which) -> {
            String categoryName = input.getText().toString().trim();
            if (categoryName.isEmpty()) {
                Toast.makeText(AddEditExpenseActivity.this, "Tên không được để trống", Toast.LENGTH_SHORT).show();
            } else {
                // 1. Tạo category mới thông qua ViewModel
                Category newCategory = mViewModel.insertNewCategory(categoryName);

                // 2. Cập nhật UI để chọn nút "Khác"
                //    và lưu ID của category mới tạo
                selectCategoryButton(newCategory.getCategoryId(), mBtnOtherCategory);

                // 3. Cập nhật text của nút "Khác" để người dùng biết
                //    (Vì `mBtnOtherCategory` là LinearLayout, ta tìm TextView bên trong)
                TextView otherText = mBtnOtherCategory.findViewById(R.id.text_category_name);
                if (otherText != null) {
                    otherText.setText(newCategory.getName()); // Hiển thị tên mới
                }
            }
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());

        builder.show();
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