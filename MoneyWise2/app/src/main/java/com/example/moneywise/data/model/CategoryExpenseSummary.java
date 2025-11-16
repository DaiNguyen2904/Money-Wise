package com.example.moneywise.data.model;

import androidx.room.ColumnInfo;

/**
 * POJO (Plain Old Java Object)
 * Dùng để chứa kết quả truy vấn TỔNG TIỀN (SUM)
 * cho mỗi Danh mục (Category)
 */
public class CategoryExpenseSummary {

    // Room sẽ tự động map cột "category_id" vào đây
    @ColumnInfo(name = "category_id")
    public String categoryId;

    // Room sẽ tự động map cột "total_amount" (từ SUM) vào đây
    @ColumnInfo(name = "total_amount")
    public double totalAmount;

    // (Room có thể tự động điền vào các trường public này,
    //  không cần constructor phức tạp)
}