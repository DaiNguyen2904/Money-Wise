package com.example.moneywise.ui.budgets;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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

        // 3. Đổ dữ liệu
        if (currentStatus != null) {
            textBudgetName.setText(currentStatus.categoryName);

            // TODO: Tạo một hàm tiện ích để chuyển Enum thành Tiếng Việt
            // Ví dụ: "MONTHLY" -> "Hàng tháng"


            // Định dạng tiền tệ
            textSpent.setText(currencyFormat.format(currentStatus.spentAmount));
            textTotal.setText(currencyFormat.format(currentStatus.budget.getAmount()));

            // Đặt tiến độ
            progressBar.setProgress(currentStatus.progressPercent);
            buttonEdit.setOnClickListener(v -> {
                if (mListener != null) {
                    mListener.onEditClick(currentStatus);
                }
            });

            buttonDelete.setOnClickListener(v -> {
                if (mListener != null) {
                    mListener.onDeleteClick(currentStatus);
                }
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