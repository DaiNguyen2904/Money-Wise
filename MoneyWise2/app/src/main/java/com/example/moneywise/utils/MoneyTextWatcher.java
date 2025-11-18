package com.example.moneywise.utils;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class MoneyTextWatcher implements TextWatcher {
    private final EditText editText;

    public MoneyTextWatcher(EditText editText) {
        this.editText = editText;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {}

    @Override
    public void afterTextChanged(Editable s) {
        editText.removeTextChangedListener(this); // Tạm dừng lắng nghe để tránh vòng lặp vô tận

        try {
            String originalString = s.toString();

            if (!originalString.isEmpty()) {
                // 1. Xóa các ký tự format cũ (ví dụ dấu chấm)
                String cleanString = originalString.replace(".", "").replace(",", "");

                // 2. Parse sang số
                double parsed = Double.parseDouble(cleanString);

                // 3. Format lại (Dùng Locale Việt Nam để có dấu chấm phân cách hàng nghìn)
                // Hoặc dùng DecimalFormat tùy chỉnh
                DecimalFormat formatter = (DecimalFormat) DecimalFormat.getInstance(new Locale("vi", "VN"));
                DecimalFormatSymbols symbols = formatter.getDecimalFormatSymbols();
                symbols.setGroupingSeparator('.'); // Ép dùng dấu chấm
                formatter.setDecimalFormatSymbols(symbols);

                String formattedString = formatter.format(parsed);

                // 4. Set lại text
                editText.setText(formattedString);
                // Di chuyển con trỏ về cuối
                editText.setSelection(formattedString.length());
            }
        } catch (NumberFormatException e) {
            // Bỏ qua lỗi nếu nhập linh tinh
        }

        editText.addTextChangedListener(this); // Lắng nghe trở lại
    }
}