package com.example.moneywise.data.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(
        tableName = "EXPENSES",
        foreignKeys = {
                @ForeignKey(
                        entity = User.class,
                        parentColumns = "user_id",
                        childColumns = "user_id",
                        onDelete = ForeignKey.CASCADE
                ),
                @ForeignKey(
                        entity = Category.class,
                        parentColumns = "category_id",
                        childColumns = "category_id",
                        onDelete = ForeignKey.SET_NULL // Giữ lại giao dịch nếu xóa danh mục
                )
        },
        indices = {@Index(value = "user_id"), @Index(value = "category_id")}
)
public class Expense {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "expense_id")
    private String expenseId; // UUID

    @NonNull
    @ColumnInfo(name = "user_id")
    private String userId;

    @ColumnInfo(name = "category_id")
    private String categoryId; // Cho phép null

    @ColumnInfo(name = "amount")
    private double amount;

    @ColumnInfo(name = "note")
    private String note;

    @ColumnInfo(name = "date")
    private long date; // Ngày giao dịch (Timestamp)

    @ColumnInfo(name = "payment_method")
    private String paymentMethod;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    @ColumnInfo(name = "updated_at")
    private Long updatedAt; // Cho phép null

    @ColumnInfo(name = "synced")
    private int synced;

    public Expense() {}

    @NonNull
    public String getExpenseId() {
        return expenseId;
    }

    public void setExpenseId(@NonNull String expenseId) {
        this.expenseId = expenseId;
    }

    @NonNull
    public String getUserId() {
        return userId;
    }

    public void setUserId(@NonNull String userId) {
        this.userId = userId;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public Long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public int getSynced() {
        return synced;
    }

    public void setSynced(int synced) {
        this.synced = synced;
    }
}