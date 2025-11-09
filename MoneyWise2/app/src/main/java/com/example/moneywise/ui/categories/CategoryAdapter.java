package com.example.moneywise.ui.categories;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;


import com.example.moneywise.R;
import com.example.moneywise.data.entity.Category;

import java.util.List;

public class CategoryAdapter extends ArrayAdapter<Category> {

    // --- BƯỚC 1: ĐỊNH NGHĨA INTERFACE ---
    public interface OnCategoryItemClickListener {
        void onEditClick(Category category);
        void onDeleteClick(Category category);
    }

    // --- BƯỚC 2: KHAI BÁO BIẾN LISTENER ---
    private OnCategoryItemClickListener mListener;


    // --- BƯỚC 3: CẬP NHẬT HÀM KHỞI TẠO (CONSTRUCTOR) ---
    public CategoryAdapter(@NonNull Context context, @NonNull List<Category> categories,
                           @NonNull OnCategoryItemClickListener listener) {
        super(context, 0, categories);
        this.mListener = listener;

    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        // 1. Lấy View của item
        View listItemView = convertView;
        if (listItemView == null) {
            listItemView = LayoutInflater.from(getContext()).inflate(
                    R.layout.list_item_category, parent, false
            );
        }

        // 2. Lấy đối tượng Category tại vị trí này
        Category currentCategory = getItem(position);

        // 3. Ánh xạ Views
        ImageView imageViewIcon = listItemView.findViewById(R.id.image_view_icon);
        TextView textViewName = listItemView.findViewById(R.id.text_view_category_name);

        View layoutButtons = listItemView.findViewById(R.id.layout_buttons_category);
        ImageButton buttonEdit = listItemView.findViewById(R.id.button_edit_category);
        ImageButton buttonDelete = listItemView.findViewById(R.id.button_delete_category);

        // 4. Đổ dữ liệu vào View
        if (currentCategory != null) {

            // Đặt tên danh mục
            textViewName.setText(currentCategory.getName());

            // Xử lý Icon & Màu sắc (Phần này sẽ cần bạn hoàn thiện)

            // ----- Logic xử lý Icon (Ví dụ) -----
            int iconResId = getIconResource(currentCategory.getIcon());
            if (iconResId != 0) { // Kiểm tra xem có tìm thấy icon không
                imageViewIcon.setImageResource(iconResId);
            }

            layoutButtons.setVisibility(View.VISIBLE);

            buttonEdit.setOnClickListener(v ->
                    mListener.onEditClick(currentCategory)
            );
            buttonDelete.setOnClickListener(v ->
                    mListener.onDeleteClick(currentCategory)
            );

            listItemView.setOnClickListener(null); // Không làm gì khi nhấn

        }

        // 5. Trả về View đã hoàn thiện
        return listItemView;
    }
    private int getIconResource(String iconName) {
        // (Bạn có thể thay R.drawable.ic_launcher_background bằng một icon mặc định khác)

        if (iconName == null || iconName.isEmpty()) {
            return R.drawable.ic_launcher_background;
        }

        try {
            Context context = getContext();
            // Lấy ID tài nguyên từ tên String
            return context.getResources().getIdentifier(iconName, "drawable", context.getPackageName());
        } catch (Exception e) {
            e.printStackTrace();
            // Trả về icon mặc định nếu lỗi
            return R.drawable.ic_launcher_background;
        }
    }
    /**
     * Hàm tiện ích để cập nhật dữ liệu cho Adapter
     */
    public void setData(List<Category> categories) {
        clear();
        if (categories != null) {
            addAll(categories);
        }
        notifyDataSetChanged();
    }
}