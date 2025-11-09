package com.example.moneywise.ui.expenses;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
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
import com.example.moneywise.data.entity.Expense;
import com.example.moneywise.data.model.ExpenseWithCategory;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExpenseAdapter extends ArrayAdapter<ExpenseWithCategory> {

    // Định dạng tiền tệ (ví dụ: 50.000 đ)
    private NumberFormat currencyFormat;

    private OnExpenseItemClickListener mListener;
    private Context mContext;

    public ExpenseAdapter(@NonNull Context context, @NonNull List<ExpenseWithCategory> items, @NonNull OnExpenseItemClickListener listener) {
        super(context, 0, items);
        this.mContext = context;
        this.currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        this.mListener = listener;
    }

    public interface OnExpenseItemClickListener {
        void onEditClick(Expense expense);
        void onDeleteClick(Expense expense);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        // 1. Lấy View của item
        View listItemView = convertView;
        if (listItemView == null) {
            listItemView = LayoutInflater.from(getContext()).inflate(
                    R.layout.list_item_expense, parent, false
            );
        }

        // 2. Lấy đối tượng Expense tại vị trí này
        ExpenseWithCategory currentItem = getItem(position);

        // Ánh xạ View (THÊM CÁC VIEW MỚI)
        ImageView iconView = listItemView.findViewById(R.id.image_view_category_icon);
        TextView categoryNameView = listItemView.findViewById(R.id.text_view_category_name);
        TextView noteView = listItemView.findViewById(R.id.text_view_note);
        TextView dateView = listItemView.findViewById(R.id.text_view_date);
        TextView amountView = listItemView.findViewById(R.id.text_view_amount);
        ImageButton buttonEdit = listItemView.findViewById(R.id.button_edit_expense);
        ImageButton buttonDelete = listItemView.findViewById(R.id.button_delete_expense);

        if (currentItem != null) {
            Expense currentExpense = currentItem.expense;
            Category currentCategory = currentItem.category;

            // --- ĐỔ DỮ LIỆU CATEGORY (MỚI) ---
            if (currentCategory != null) {
                categoryNameView.setText(currentCategory.getName());

                // Đặt icon
                int iconResId = getIconResource(currentCategory.getIcon());
                iconView.setImageResource(iconResId);

                // Đặt màu nền cho icon
                try {
                    int color = Color.parseColor(currentCategory.getColor());
                    Drawable background = ContextCompat.getDrawable(mContext, R.drawable.circle_background);
                    if(background != null) {
                        // background.setColorFilter(color, PorterDuff.Mode.SRC_IN); // Cách cũ
                        // Dùng setTint() an toàn hơn
                        androidx.core.graphics.drawable.DrawableCompat.setTint(background, color);
                        iconView.setBackground(background);
                    }
                    iconView.setImageTintList(ColorStateList.valueOf(Color.WHITE));

                } catch (Exception e) {
                    // Nếu màu bị lỗi, dùng màu mặc định
                    iconView.setBackgroundResource(R.drawable.circle_background);
                }

            } else {
                // Xử lý trường hợp danh mục đã bị xóa
                categoryNameView.setText("Không có danh mục");
                iconView.setImageResource(R.drawable.ic_other); // Dùng icon mặc định 'khác'
                iconView.setBackgroundResource(R.drawable.circle_background);
            }

            // --- ĐỔ DỮ LIỆU EXPENSE (như cũ) ---
            noteView.setText(currentExpense.getNote() != null && !currentExpense.getNote().isEmpty()
                    ? currentExpense.getNote() : "Không có ghi chú");

            Date dateObject = new Date(currentExpense.getDate());
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            dateView.setText(sdf.format(dateObject));

            String formattedAmount = currencyFormat.format(currentExpense.getAmount());
            amountView.setText("- " + formattedAmount);

            // BƯỚC 3: SỬA CLICK LISTENER
            // Đảm bảo truyền vào currentExpense (đối tượng gốc)
            buttonEdit.setOnClickListener(v -> mListener.onEditClick(currentExpense));
            buttonDelete.setOnClickListener(v -> mListener.onDeleteClick(currentExpense));
        }

        return listItemView;
    }

    /**
     * Hàm tiện ích để cập nhật dữ liệu (Sửa kiểu dữ liệu)
     */
    public void setData(List<ExpenseWithCategory> items) {
        clear();
        if (items != null) {
            addAll(items);
        }
        notifyDataSetChanged();
    }

    // Hàm tiện ích lấy icon (sao chép từ AddEditExpenseActivity)
    private int getIconResource(String iconName) {
        if (iconName == null || iconName.isEmpty()) {
            return R.drawable.ic_other; // Icon mặc định
        }
        try {
            int resId = mContext.getResources().getIdentifier(iconName, "drawable", mContext.getPackageName());
            return resId == 0 ? R.drawable.ic_other : resId; // Trả về mặc định nếu không tìm thấy
        } catch (Exception e) {
            e.printStackTrace();
            return R.drawable.ic_other; // Icon mặc định nếu lỗi
        }
    }

}