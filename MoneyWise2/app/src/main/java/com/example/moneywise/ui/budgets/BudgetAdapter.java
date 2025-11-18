package com.example.moneywise.ui.budgets;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.moneywise.R;
import com.example.moneywise.data.model.BudgetStatus;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class BudgetAdapter extends ArrayAdapter<BudgetStatus> {

    private NumberFormat currencyFormat;
    public interface OnBudgetItemClickListener {
        void onEditClick(BudgetStatus budgetStatus);
        void onDeleteClick(BudgetStatus budgetStatus);
    }

    private OnBudgetItemClickListener mListener;

    public BudgetAdapter(@NonNull Context context, @NonNull List<BudgetStatus> statuses, @NonNull OnBudgetItemClickListener listener) {
        super(context, 0, statuses);
        this.currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        this.mListener = listener; // Lưu listener
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItemView = convertView;
        if (listItemView == null) {
            listItemView = LayoutInflater.from(getContext()).inflate(
                    R.layout.list_item_budget, parent, false
            );
        }

        // 1. Lấy dữ liệu đã xử lý
        BudgetStatus currentStatus = getItem(position);

        // 2. Ánh xạ Views
        TextView textBudgetName = listItemView.findViewById(R.id.text_view_budget_name);
        TextView textBudgetPeriod = listItemView.findViewById(R.id.text_view_budget_period);
        ProgressBar progressBar = listItemView.findViewById(R.id.progress_bar_budget);
        TextView textSpent = listItemView.findViewById(R.id.text_view_spent);
        TextView textTotal = listItemView.findViewById(R.id.text_view_total);

        ImageButton buttonEdit = listItemView.findViewById(R.id.button_edit_budget);
        ImageButton buttonDelete = listItemView.findViewById(R.id.button_delete_budget);

        TextView textWarning = listItemView.findViewById(R.id.text_view_budget_warning);

        // 3. Đổ dữ liệu
        if (currentStatus != null) {
            textBudgetName.setText(currentStatus.categoryName);

            // Hiển thị số tiền
            textSpent.setText(currencyFormat.format(currentStatus.spentAmount));
            textTotal.setText(currencyFormat.format(currentStatus.budget.getAmount()));
            progressBar.setProgress(currentStatus.progressPercent);

            // --- LOGIC CẢNH BÁO VÀ MÀU SẮC ---
            int colorResId;
            if (currentStatus.progressPercent >= 100) {
                // Vượt mức -> Màu Đỏ
                colorResId = R.color.budget_exceeded;
                textWarning.setVisibility(View.VISIBLE);
                textWarning.setText("⚠️ Đã vượt quá hạn mức!");
            } else if (currentStatus.progressPercent >= 80) {
                // Sắp vượt -> Màu Cam
                colorResId = R.color.budget_warning;
                textWarning.setVisibility(View.VISIBLE);
                textWarning.setText("⚠️ Sắp chạm ngưỡng hạn mức");
            } else {
                // An toàn -> Màu Xanh
                colorResId = R.color.budget_safe;
                textWarning.setVisibility(View.GONE);
            }

            // Lấy mã màu thực tế
            int color = ContextCompat.getColor(getContext(), colorResId);

            // Áp dụng màu
            textSpent.setTextColor(color); // Đổi màu số tiền đã tiêu
            textWarning.setTextColor(color); // Đổi màu text cảnh báo
            progressBar.setProgressTintList(ColorStateList.valueOf(color)); // Đổi màu thanh progress

            // Sự kiện Click
            buttonEdit.setOnClickListener(v -> {
                if (mListener != null) mListener.onEditClick(currentStatus);
            });

            buttonDelete.setOnClickListener(v -> {
                if (mListener != null) mListener.onDeleteClick(currentStatus);
            });
        }

        return listItemView;
    }

    /**
     * Hàm tiện ích để cập nhật dữ liệu cho Adapter
     */
    public void setData(List<BudgetStatus> statuses) {
        clear();
        if (statuses != null) {
            addAll(statuses);
        }
        notifyDataSetChanged();
    }
}