package com.example.moneywise.data.model;


import com.example.moneywise.data.entity.Budget;

/**
 * Đây là một lớp POJO (không phải Entity)
 * dùng để chứa dữ liệu đã được xử lý cho Budget Adapter.
 */
public class BudgetStatus {

    public Budget budget; // Ngân sách gốc
    public String categoryName; // Tên danh mục (ví dụ: "Ăn uống" hoặc "Ngân sách tổng")
    public double spentAmount; // Tổng đã chi
    public int progressPercent; // Phần trăm tiến độ (0-100)

    public BudgetStatus(Budget budget, String categoryName, double spentAmount, int progressPercent) {
        this.budget = budget;
        this.categoryName = categoryName;
        this.spentAmount = spentAmount;
        this.progressPercent = progressPercent;
    }

    // Thêm Enum trạng thái
    public enum State {
        SAFE, WARNING, EXCEEDED
    }
    // Hàm lấy trạng thái hiện tại
    public State getState() {
        if (progressPercent >= 100) {
            return State.EXCEEDED;
        } else if (progressPercent >= 80) {
            return State.WARNING;
        } else {
            return State.SAFE;
        }
    }
}