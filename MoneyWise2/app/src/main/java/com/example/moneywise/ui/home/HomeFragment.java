package com.example.moneywise.ui.home;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.moneywise.MainActivity;
import com.example.moneywise.R;
import com.example.moneywise.data.entity.Category;
import com.example.moneywise.data.entity.Expense;
import com.example.moneywise.data.model.CategoryExpenseSummary;
import com.example.moneywise.data.model.ExpenseWithCategory;
import com.example.moneywise.data.model.HomeCategorySummary;
import com.example.moneywise.ui.expenses.ExpenseAdapter;
import com.google.firebase.auth.FirebaseUser;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
public class HomeFragment extends Fragment {

    private HomeViewModel mHomeViewModel;
    private NumberFormat mCurrencyFormat;

    // (Khai báo Views giữ nguyên)
    private TextView mTextGreetingName;
    private ImageButton mBtnNotifications;
    private TextView mTextTotalSpent;
    private TextView mTextComparison;
    private LinearLayout mLayoutHomeCategories;
    private LinearLayout mLayoutRecentTransactions;
    private TextView mTextHomeViewAll;

    // (Biến tạm giữ nguyên)
    private Double mCurrentTotal = null;
    private Double mPreviousTotal = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        // (toàn bộ hàm onViewCreated giữ nguyên: khởi tạo format, ViewModel, ánh xạ Views, gọi setupClickListeners, setupObservers)
        super.onViewCreated(view, savedInstanceState);

        mCurrencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        mHomeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        mTextGreetingName = view.findViewById(R.id.text_home_user_name);
        mBtnNotifications = view.findViewById(R.id.button_home_notifications);
        mTextTotalSpent = view.findViewById(R.id.text_home_total_spent);
        mTextComparison = view.findViewById(R.id.text_home_comparison);
        mLayoutHomeCategories = view.findViewById(R.id.layout_home_categories);
        mTextHomeViewAll = view.findViewById(R.id.text_home_view_all);
        mLayoutRecentTransactions = view.findViewById(R.id.list_view_recent_transactions);

        setupClickListeners();
        setupObservers();
    }

    // (onResume, onStop, setupClickListeners giữ nguyên)
    @Override
    public void onResume() {
        super.onResume();
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity != null && activity.getSupportActionBar() != null) {
            activity.getSupportActionBar().hide();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity != null && activity.getSupportActionBar() != null) {
            activity.getSupportActionBar().show();
        }
    }

    private void setupClickListeners() {
        mBtnNotifications.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Chức năng thông báo sắp ra mắt!", Toast.LENGTH_SHORT).show();
        });

        // --- CẬP NHẬT LOGIC CLICK "XEM TẤT CẢ" ---
        mTextHomeViewAll.setOnClickListener(v -> {
            // Lấy Activity cha (là MainActivity)
            if (getActivity() instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) getActivity();

                // Gọi hàm public để chuyển sang tab Chi tiêu
                // (R.id.nav_expenses là ID trong bottom_nav_menu.xml)
                mainActivity.navigateToTab(R.id.nav_expenses);
            }
        });
    }


    /**
     * CẬP NHẬT: Lắng nghe LiveData từ ViewModel
     */
    private void setupObservers() {
        // 1. Lắng nghe Tên Người dùng (Giữ nguyên)
        mHomeViewModel.getUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null && user.getDisplayName() != null) {
                mTextGreetingName.setText(user.getDisplayName());
            } else {
                FirebaseUser fUser = mHomeViewModel.getFirebaseUser();
                if (fUser != null && fUser.getDisplayName() != null) {
                    mTextGreetingName.setText(fUser.getDisplayName());
                } else if (fUser != null && fUser.getEmail() != null) {
                    mTextGreetingName.setText(fUser.getEmail());
                } else {
                    mTextGreetingName.setText("Người dùng");
                }
            }
        });

        // 2. Lắng nghe Tổng chi tháng này (SỬA LỖI HÊN XUI)
        mHomeViewModel.getCurrentMonthTotal().observe(getViewLifecycleOwner(), total -> {
            mCurrentTotal = (total != null) ? total : 0.0;
            mTextTotalSpent.setText(mCurrencyFormat.format(mCurrentTotal));
            updateComparisonText(); // Cập nhật %

            // XÓA DÒNG GÂY LỖI NÀY
            // updateCategorySummaryList(mHomeViewModel.getCategorySummary().getValue());
        });

        // 3. Lắng nghe Tổng chi tháng trước (Giữ nguyên)
        mHomeViewModel.getPreviousMonthTotal().observe(getViewLifecycleOwner(), total -> {
            mPreviousTotal = (total != null) ? total : 0.0;
            updateComparisonText(); // Cập nhật %
        });

        // 4. Lắng nghe Tóm tắt Danh mục (SỬA LỖI CRASH)
        // Thay vì gọi hàm cũ, gọi LiveData mới 'getHomeCategorySummary'
        mHomeViewModel.getHomeCategorySummary().observe(getViewLifecycleOwner(), this::displayCategorySummaryList);

        // 5. Lắng nghe Giao dịch gần đây (List dọc) (Giữ nguyên)
        mHomeViewModel.getRecentTransactions().observe(getViewLifecycleOwner(), this::updateRecentTransactionsList);
    }

    // (hàm updateComparisonText giữ nguyên)
    private void updateComparisonText() {
        if (mCurrentTotal == null || mPreviousTotal == null) {
            mTextComparison.setVisibility(View.GONE);
            return;
        }
        if (mPreviousTotal == 0) {
            if (mCurrentTotal > 0) {
                mTextComparison.setText("Tháng trước không chi tiêu");
                mTextComparison.setTextColor(Color.GRAY);
                mTextComparison.setVisibility(View.VISIBLE);
            } else {
                mTextComparison.setVisibility(View.GONE);
            }
            return;
        }
        double percentChange = ((mCurrentTotal - mPreviousTotal) / mPreviousTotal) * 100;
        if (percentChange > 0) {
            mTextComparison.setText(String.format(Locale.US, "+%.0f%% so với tháng trước", percentChange));
            mTextComparison.setTextColor(ContextCompat.getColor(getContext(), R.color.colorNegative));
        } else if (percentChange < 0) {
            mTextComparison.setText(String.format(Locale.US, "%.0f%% so với tháng trước", percentChange));
            mTextComparison.setTextColor(Color.parseColor("#388E3C")); // Màu xanh lá
        } else {
            mTextComparison.setText("Bằng tháng trước");
            mTextComparison.setTextColor(Color.GRAY);
        }
        mTextComparison.setVisibility(View.VISIBLE);
    }

    /**
     * THAY THẾ HÀM CŨ: (updateCategorySummaryList)
     * Hàm này giờ rất "ngốc", chỉ hiển thị dữ liệu đã được ViewModel xử lý
     */
    private void displayCategorySummaryList(List<HomeCategorySummary> summaries) {
        // Kiểm tra an toàn (nếu fragment bị hủy hoặc data là null)
        if (summaries == null || !isAdded()) {
            mLayoutHomeCategories.removeAllViews(); // Dọn dẹp nếu data rỗng
            return;
        }

        mLayoutHomeCategories.removeAllViews(); // Xóa thẻ cũ
        LayoutInflater inflater = LayoutInflater.from(getContext());

        for (HomeCategorySummary summary : summaries) {
            View itemView = inflater.inflate(R.layout.list_item_home_category, mLayoutHomeCategories, false);

            TextView name = itemView.findViewById(R.id.text_category_name);
            TextView amount = itemView.findViewById(R.id.text_category_amount);
            TextView percent = itemView.findViewById(R.id.text_category_percent);
            View colorBar = itemView.findViewById(R.id.view_category_color);

            // DÙNG DỮ LIỆU ĐÃ XỬ LÝ (Không cần .stream() hay .getValue() nữa)
            name.setText(summary.categoryName);
            amount.setText(mCurrencyFormat.format(summary.totalAmount));
            percent.setText(String.format(Locale.US, "%.0f%%", summary.percentage));

            try {
                colorBar.setBackgroundColor(Color.parseColor(summary.categoryColor));
            } catch (Exception e) {
                colorBar.setBackgroundColor(Color.GRAY); // Màu mặc định nếu lỗi
            }

            mLayoutHomeCategories.addView(itemView);
        }
    }

    /**
     * (Hàm updateRecentTransactionsList giữ nguyên)
     */
    private void updateRecentTransactionsList(List<ExpenseWithCategory> transactions) {
        if (transactions == null || !isAdded()) {
            mLayoutRecentTransactions.removeAllViews(); // Dọn dẹp
            return;
        }

        mLayoutRecentTransactions.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(getContext());
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        for (ExpenseWithCategory item : transactions) {
            // ... (toàn bộ logic bên trong hàm này giữ nguyên)
            View itemView = inflater.inflate(R.layout.list_item_expense, mLayoutRecentTransactions, false);
            Expense currentExpense = item.expense;
            Category currentCategory = item.category;
            ImageView iconView = itemView.findViewById(R.id.image_view_category_icon);
            TextView categoryNameView = itemView.findViewById(R.id.text_view_category_name);
            TextView noteView = itemView.findViewById(R.id.text_view_note);
            TextView dateView = itemView.findViewById(R.id.text_view_date);
            TextView amountView = itemView.findViewById(R.id.text_view_amount);
            itemView.findViewById(R.id.button_edit_expense).setVisibility(View.GONE);
            itemView.findViewById(R.id.button_delete_expense).setVisibility(View.GONE);
            if (currentCategory != null) {
                categoryNameView.setText(currentCategory.getName());
                int iconResId = getIconResource(currentCategory.getIcon());
                iconView.setImageResource(iconResId);
                try {
                    int color = Color.parseColor(currentCategory.getColor());
                    Drawable background = ContextCompat.getDrawable(getContext(), R.drawable.circle_background);
                    if(background != null) {
                        DrawableCompat.setTint(background, color);
                        iconView.setBackground(background);
                    }
                    iconView.setImageTintList(ColorStateList.valueOf(Color.WHITE));
                } catch (Exception e) {
                    iconView.setBackgroundResource(R.drawable.circle_background);
                }
            } else {
                categoryNameView.setText("Không có danh mục");
                iconView.setImageResource(R.drawable.ic_other);
                iconView.setBackgroundResource(R.drawable.circle_background);
            }
            noteView.setText(currentExpense.getNote() != null && !currentExpense.getNote().isEmpty()
                    ? currentExpense.getNote() : "Không có ghi chú");
            dateView.setText(sdf.format(new Date(currentExpense.getDate())));
            String formattedAmount = mCurrencyFormat.format(currentExpense.getAmount());
            amountView.setText("- " + formattedAmount);
            mLayoutRecentTransactions.addView(itemView);
        }
    }

    // (hàm getIconResource giữ nguyên)
    private int getIconResource(String iconName) {
        if (iconName == null || iconName.isEmpty()) {
            return R.drawable.ic_other;
        }
        try {
            // SỬA LỖI: Phải kiểm tra getContext() không bị null
            if (getContext() == null) return R.drawable.ic_other;
            int resId = getContext().getResources().getIdentifier(iconName, "drawable", getContext().getPackageName());
            return resId == 0 ? R.drawable.ic_other : resId;
        } catch (Exception e) {
            return R.drawable.ic_other;
        }
    }
}