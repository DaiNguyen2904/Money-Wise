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
    public String expenseId; // UUID

    @NonNull
    @ColumnInfo(name = "user_id")
    public String userId;

    @ColumnInfo(name = "category_id")
    public String categoryId; // Cho phép null

    @ColumnInfo(name = "amount")
    public double amount;

    @ColumnInfo(name = "note")
    public String note;

    @ColumnInfo(name = "date")
    public long date; // Ngày giao dịch (Timestamp)

    @ColumnInfo(name = "payment_method")
    public String paymentMethod;

    @ColumnInfo(name = "created_at")
    public long createdAt;

    @ColumnInfo(name = "updated_at")
    public Long updatedAt; // Cho phép null

    @ColumnInfo(name = "synced")
    public int synced;

    public Expense() {}
}