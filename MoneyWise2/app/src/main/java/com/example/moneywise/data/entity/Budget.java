package com.example.moneywise.data.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;
import androidx.room.TypeConverters;

import com.example.moneywise.utils.Converters;

@Entity(
        tableName = "BUDGETS",
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
                        onDelete = ForeignKey.CASCADE // Xóa ngân sách nếu xóa danh mục
                )
        },
        indices = {@Index(value = "user_id"), @Index(value = "category_id")}
)
public class Budget {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "budget_id")
    private String budgetId; // UUID

    @NonNull
    @ColumnInfo(name = "user_id")
    private String userId;

    @ColumnInfo(name = "category_id")
    private String categoryId; // Cho phép null (cho ngân sách tổng)

    @ColumnInfo(name = "amount")
    private double amount;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    @ColumnInfo(name = "updated_at")
    public Long updatedAt; // Cho phép null

    @ColumnInfo(name = "synced")
    public int synced;

    public Budget() {}

    public Budget(@NonNull String budgetId, @NonNull String userId, String categoryId, double amount, long createdAt) {
        this.budgetId = budgetId;
        this.userId = userId;
        this.categoryId = categoryId;
        this.amount = amount;
        this.createdAt = createdAt;
        this.synced = 0; // Mặc định khi tạo
    }

    @NonNull
    public String getBudgetId() {
        return budgetId;
    }

    public void setBudgetId(@NonNull String budgetId) {
        this.budgetId = budgetId;
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