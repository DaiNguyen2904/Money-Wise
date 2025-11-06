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
    public String budgetId; // UUID

    @NonNull
    @ColumnInfo(name = "user_id")
    public String userId;

    @ColumnInfo(name = "category_id")
    public String categoryId; // Cho phép null (cho ngân sách tổng)

    @ColumnInfo(name = "amount")
    public double amount;

    @ColumnInfo(name = "created_at")
    public long createdAt;

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
}