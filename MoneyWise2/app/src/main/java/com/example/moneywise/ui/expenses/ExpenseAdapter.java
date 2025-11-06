package com.example.moneywise.ui.expenses;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


import com.example.moneywise.R;
import com.example.moneywise.data.entity.Expense;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExpenseAdapter extends ArrayAdapter<Expense> {

    // Định dạng tiền tệ (ví dụ: 50.000 đ)
    private NumberFormat currencyFormat;

    private OnExpenseItemClickListener mListener;

    public ExpenseAdapter(@NonNull Context context, @NonNull List<Expense> expenses, @NonNull OnExpenseItemClickListener listener) {
        super(context, 0, expenses);
        this.currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        this.mListener = listener; // Lưu listener được truyền vào
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
        Expense currentExpense = getItem(position);

        // 3. Ánh xạ View
        TextView textViewNote = listItemView.findViewById(R.id.text_view_note);
        TextView textViewDate = listItemView.findViewById(R.id.text_view_date);
        TextView textViewAmount = listItemView.findViewById(R.id.text_view_amount);

        ImageButton buttonEdit = listItemView.findViewById(R.id.button_edit_expense);
        ImageButton buttonDelete = listItemView.findViewById(R.id.button_delete_expense);

        // 4. Đổ dữ liệu vào View
        if (currentExpense != null) {
            // Ghi chú (nếu không có thì hiển thị "Chưa có ghi chú")
            textViewNote.setText(currentExpense.note != null && !currentExpense.note.isEmpty()
                    ? currentExpense.note : "Không có ghi chú");

            // Chuyển Timestamp (long) thành đối tượng Date
            Date dateObject = new Date(currentExpense.date);
            // Định dạng nó thành chuỗi "dd/MM/yyyy"
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            textViewDate.setText(sdf.format(dateObject));

            // Số tiền
            String formattedAmount = currencyFormat.format(currentExpense.amount);
            textViewAmount.setText("- " + formattedAmount); // Vì đây là chi tiêu
        }

        // Xử lý nút Sửa
        buttonEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Gọi ra interface, truyền đối tượng expense hiện tại
                mListener.onEditClick(currentExpense);
            }
        });

        // Xử lý nút Xóa
        buttonDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Gọi ra interface, truyền đối tượng expense hiện tại
                mListener.onDeleteClick(currentExpense);
            }
        });

        // 5. Trả về View đã hoàn thiện
        return listItemView;
    }

    /**
     * Hàm tiện ích để cập nhật dữ liệu cho Adapter
     */
    public void setData(List<Expense> expenses) {
        clear();
        if (expenses != null) {
            addAll(expenses);
        }
        notifyDataSetChanged();
    }
}