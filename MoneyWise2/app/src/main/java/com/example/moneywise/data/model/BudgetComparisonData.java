package com.example.moneywise.data.model;

/**
 * POJO (Model) này chứa dữ liệu đã được kết hợp
 * để dùng cho Biểu đồ Cột so sánh Ngân sách.
 */
public class BudgetComparisonData {

    public String categoryName;
    public double spentAmount; // Tổng đã chi
    public double budgetAmount; // Ngân sách đặt ra

    public BudgetComparisonData(String categoryName, double spentAmount, double budgetAmount) {
        this.categoryName = categoryName;
        this.spentAmount = spentAmount;
        this.budgetAmount = budgetAmount;
    }
}
