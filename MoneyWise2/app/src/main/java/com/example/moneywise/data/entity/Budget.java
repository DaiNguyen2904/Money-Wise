package com.example.moneywise.data.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;
import androidx.room.TypeConverters;

import com.example.moneywise.utils.Converters;
import com.google.firebase.firestore.PropertyName;

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
    @PropertyName("budgetId") // <-- THÊM
    private String budgetId;

    @NonNull
    @ColumnInfo(name = "user_id")
    @PropertyName("userId") // <-- THÊM
    private String userId;

    @ColumnInfo(name = "category_id")
    @PropertyName("categoryId") // <-- THÊM
    private String categoryId;

    @ColumnInfo(name = "amount")
    @PropertyName("amount") // <-- THÊM
    private double amount;

    @ColumnInfo(name = "created_at")
    @PropertyName("createdAt") // <-- THÊM
    private long createdAt;

    @ColumnInfo(name = "updated_at")
    @PropertyName("updatedAt") // <-- THÊM
    private Long updatedAt;

    @ColumnInfo(name = "synced")
    private int synced; // (Không cần)

    public Budget() {}

    public Budget(@NonNull String budgetId, @NonNull String userId, String categoryId, double amount, long createdAt) {
        this.budgetId = budgetId;
        this.userId = userId;
        this.categoryId = categoryId;
        this.amount = amount;
        this.createdAt = createdAt;
    }

    // --- THÊM @PropertyName VÀO GETTERS/SETTERS ---
    @NonNull
    @PropertyName("budgetId") // <-- THÊM
    public String getBudgetId() { return budgetId; }
    @PropertyName("budgetId") // <-- THÊM
    public void setBudgetId(@NonNull String budgetId) { this.budgetId = budgetId; }

    @NonNull
    @PropertyName("userId") // <-- THÊM
    public String getUserId() { return userId; }
    @PropertyName("userId") // <-- THÊM
    public void setUserId(@NonNull String userId) { this.userId = userId; }

    @PropertyName("categoryId") // <-- THÊM
    public String getCategoryId() { return categoryId; }
    @PropertyName("categoryId") // <-- THÊM
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    @PropertyName("amount") // <-- THÊM
    public double getAmount() { return amount; }
    @PropertyName("amount") // <-- THÊM
    public void setAmount(double amount) { this.amount = amount; }

    @PropertyName("createdAt") // <-- THÊM
    public long getCreatedAt() { return createdAt; }
    @PropertyName("createdAt") // <-- THÊM
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    @PropertyName("updatedAt") // <-- THÊM
    public Long getUpdatedAt() { return updatedAt; }
    @PropertyName("updatedAt") // <-- THÊM
    public void setUpdatedAt(Long updatedAt) { this.updatedAt = updatedAt; }

    // (synced không cần)
    public int getSynced() { return synced; }
    public void setSynced(int synced) { this.synced = synced; }
}