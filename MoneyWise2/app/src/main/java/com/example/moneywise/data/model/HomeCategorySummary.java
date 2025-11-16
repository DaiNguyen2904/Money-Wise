package com.example.moneywise.data.model;

/**
 * POJO (Model) này chứa dữ liệu ĐÃ KẾT HỢP
 * cho danh sách thẻ cuộn ngang trên Trang chủ
 */
public class HomeCategorySummary {
    public String categoryName;
    public String categoryColor;
    public double totalAmount;
    public double percentage; // 0-100

    public HomeCategorySummary(String categoryName, String categoryColor, double totalAmount, double percentage) {
        this.categoryName = categoryName;
        this.categoryColor = categoryColor;
        this.totalAmount = totalAmount;
        this.percentage = percentage;
    }
}
