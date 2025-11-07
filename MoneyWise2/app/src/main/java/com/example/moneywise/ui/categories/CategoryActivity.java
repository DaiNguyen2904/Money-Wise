package com.example.moneywise.ui.categories;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.moneywise.R;
import com.example.moneywise.data.entity.Category;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity này giờ chỉ có 1 vai trò:
 * Hiển thị danh sách Danh mục để người dùng "Chọn".
 */
public class CategoryActivity extends AppCompatActivity {

    // Hằng số để gửi kết quả về
    public static final String EXTRA_SELECTED_CATEGORY_ID = "EXTRA_SELECTED_CATEGORY_ID";
    /** Dùng để yêu cầu Activity này ở chế độ "Chọn" */
    public static final String EXTRA_PICK_MODE = "EXTRA_PICK_MODE";
    /** Dùng để gửi trả categoryId về */
    private CategoryViewModel mCategoryViewModel;
    private ListView mListView;
    private CategoryPickerAdapter mAdapter; // Dùng 1 Adapter nội bộ đơn giản

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Tái sử dụng layout của fragment, nhưng ẩn FAB đi
        setContentView(R.layout.fragment_category);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Chọn Danh mục");
        }

        // Ẩn FAB (vì đây là chế độ "Chọn")
        FloatingActionButton fab = findViewById(R.id.fab_add_category);
        fab.setVisibility(View.GONE);

        // 1. Ánh xạ ListView
        mListView = findViewById(R.id.list_view_categories);

        // 2. Khởi tạo Adapter (dùng Adapter đơn giản bên dưới)
        mAdapter = new CategoryPickerAdapter(this, new ArrayList<>());
        mListView.setAdapter(mAdapter);

        // 3. Lấy ViewModel
        mCategoryViewModel = new ViewModelProvider(this).get(CategoryViewModel.class);

        // 4. Theo dõi LiveData
        mCategoryViewModel.getAllCategories().observe(this, categories -> {
            mAdapter.setData(categories);
        });

        // 5. XỬ LÝ CHỌN (Quan trọng)
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Category selectedCategory = mAdapter.getItem(position);

                Intent replyIntent = new Intent();
                replyIntent.putExtra(EXTRA_SELECTED_CATEGORY_ID, selectedCategory.getCategoryId());

                // GỌI HÀM setResult() CỦA ACTIVITY (Giờ đã hợp lệ)
                setResult(Activity.RESULT_OK, replyIntent);
                finish(); // Đóng Activity "Picker" này lại
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}


/**
 * Adapter nội bộ (private class) đơn giản, chỉ để hiển thị
 * Tái sử dụng layout list_item_category, nhưng ẩn các nút Sửa/Xóa
 */
class CategoryPickerAdapter extends ArrayAdapter<Category> {

    public CategoryPickerAdapter(@NonNull Context context, @NonNull List<Category> categories) {
        super(context, 0, categories);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItemView = convertView;
        if (listItemView == null) {
            listItemView = LayoutInflater.from(getContext()).inflate(
                    R.layout.list_item_category, parent, false
            );
        }

        Category currentCategory = getItem(position);

        // Ẩn các nút Sửa/Xóa
        View layoutButtons = listItemView.findViewById(R.id.layout_buttons_category);
        layoutButtons.setVisibility(View.GONE);

        // Ánh xạ và hiển thị (Icon, Tên)
        TextView textViewName = listItemView.findViewById(R.id.text_view_category_name);
        ImageView imageViewIcon = listItemView.findViewById(R.id.image_view_icon);

        if (currentCategory != null) {
            textViewName.setText(currentCategory.getName());
            // (Bạn có thể thêm logic hiển thị icon/màu ở đây nếu muốn)
        }

        return listItemView;
    }

    public void setData(List<Category> categories) {
        clear();
        if (categories != null) addAll(categories);
        notifyDataSetChanged();
    }
}