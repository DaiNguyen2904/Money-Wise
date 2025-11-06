package com.example.moneywise.ui.categories;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.moneywise.R;
import com.google.android.material.textfield.TextInputEditText;


public class AddEditCategoryActivity extends AppCompatActivity {

    // Khai báo hằng số để trao đổi Intent (Phải khớp với CategoryActivity)
    public static final String EXTRA_CATEGORY_ID = "EXTRA_CATEGORY_ID";
    public static final String EXTRA_CATEGORY_NAME = "EXTRA_CATEGORY_NAME";
    public static final String EXTRA_CATEGORY_ICON = "EXTRA_CATEGORY_ICON";
    public static final String EXTRA_CATEGORY_COLOR = "EXTRA_CATEGORY_COLOR";

    // Khai báo Views
    private TextInputEditText mEditTextName;
    private TextInputEditText mEditTextIcon;
    private TextInputEditText mEditTextColor;
    private Button mButtonSave;

    private String mCurrentCategoryId; // Để biết đang ở chế độ Sửa hay không
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_category);

        // 1. Ánh xạ Views
        mEditTextName = findViewById(R.id.edit_text_category_name);
        mEditTextIcon = findViewById(R.id.edit_text_category_icon);
        mEditTextColor = findViewById(R.id.edit_text_category_color);
        mButtonSave = findViewById(R.id.button_save_category);

        // 2. Kiểm tra Intent để xem là "Thêm" hay "Sửa"
        Intent intent = getIntent();
        if (intent.hasExtra(EXTRA_CATEGORY_ID)) {
            // Đây là chế độ "Sửa"
            isEditMode = true;
            mCurrentCategoryId = intent.getStringExtra(EXTRA_CATEGORY_ID);

            // Điền dữ liệu cũ vào form
            mEditTextName.setText(intent.getStringExtra(EXTRA_CATEGORY_NAME));
            mEditTextIcon.setText(intent.getStringExtra(EXTRA_CATEGORY_ICON));
            mEditTextColor.setText(intent.getStringExtra(EXTRA_CATEGORY_COLOR));

            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Chỉnh sửa Danh mục");
            }

        } else {
            // Đây là chế độ "Thêm mới"
            isEditMode = false;
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Thêm Danh mục mới");
            }
        }

        // Cài đặt nút "Back"
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // 3. Xử lý sự kiện nhấn nút "Lưu"
        mButtonSave.setOnClickListener(view -> {
            saveCategory();
        });
    }

    private void saveCategory() {
        // 1. Lấy dữ liệu từ form
        String name = mEditTextName.getText().toString().trim();
        String icon = mEditTextIcon.getText().toString().trim();
        String color = mEditTextColor.getText().toString().trim();

        // 2. Validation (Kiểm tra)
        if (name.isEmpty()) {
            mEditTextName.setError("Tên danh mục là bắt buộc");
            mEditTextName.requestFocus();
            return;
        }

        // 3. Đóng gói dữ liệu vào Intent trả về
        Intent replyIntent = new Intent();
        replyIntent.putExtra(EXTRA_CATEGORY_NAME, name);
        replyIntent.putExtra(EXTRA_CATEGORY_ICON, icon);
        replyIntent.putExtra(EXTRA_CATEGORY_COLOR, color);

        // Nếu là chế độ Sửa, gửi kèm ID để CategoryActivity biết
        if (isEditMode) {
            replyIntent.putExtra(EXTRA_CATEGORY_ID, mCurrentCategoryId);
        }

        // 4. Gửi kết quả (RESULT_OK) và đóng Activity
        setResult(RESULT_OK, replyIntent);
        finish();
    }

    // (Xử lý khi nhấn nút Back trên ActionBar)
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
