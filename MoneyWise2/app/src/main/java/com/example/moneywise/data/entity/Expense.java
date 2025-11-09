package com.example.moneywise.data.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

import com.google.firebase.firestore.PropertyName;

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
    @PropertyName("expenseId") // Giả sử trên Firestore là "expenseId"
    private String expenseId;

    @NonNull
    @ColumnInfo(name = "user_id")
    @PropertyName("userId") // Giả sử trên Firestore là "userId"
    private String userId;

    @ColumnInfo(name = "category_id")
    @PropertyName("categoryId") // Giả sử trên Firestore là "categoryId"
    private String categoryId;

    @ColumnInfo(name = "amount")
    @PropertyName("amount")
    private double amount;

    @ColumnInfo(name = "note")
    @PropertyName("note")
    private String note;

    @ColumnInfo(name = "date")
    @PropertyName("date")
    private long date;

    @ColumnInfo(name = "payment_method")
    @PropertyName("paymentMethod")
    private String paymentMethod;

    @ColumnInfo(name = "created_at")
    @PropertyName("createdAt")
    private long createdAt;

    @ColumnInfo(name = "updated_at")
    @PropertyName("updatedAt")
    private Long updatedAt;

    @ColumnInfo(name = "synced")
    // Chúng ta không cần @PropertyName cho 'synced'
    // vì chúng ta không muốn đọc/ghi nó từ Firestore
    private int synced;

    public Expense() {}

    // --- SỬA LẠI GETTERS ĐỂ DÙNG @PropertyName ---
    // (Lưu ý: Firestore dùng getters/setters, không dùng biến trực tiếp)
    @NonNull
    @PropertyName("expenseId")
    public String getExpenseId() { return expenseId; }
    @PropertyName("expenseId")
    public void setExpenseId(@NonNull String expenseId) { this.expenseId = expenseId; }

    @NonNull
    @PropertyName("userId")
    public String getUserId() { return userId; }
    @PropertyName("userId")
    public void setUserId(@NonNull String userId) { this.userId = userId; }

    @PropertyName("categoryId")
    public String getCategoryId() { return categoryId; }
    @PropertyName("categoryId")
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    @PropertyName("amount")
    public double getAmount() { return amount; }
    @PropertyName("amount")
    public void setAmount(double amount) { this.amount = amount; }

    @PropertyName("note")
    public String getNote() { return note; }
    @PropertyName("note")
    public void setNote(String note) { this.note = note; }

    @PropertyName("date")
    public long getDate() { return date; }
    @PropertyName("date")
    public void setDate(long date) { this.date = date; }

    @PropertyName("paymentMethod")
    public String getPaymentMethod() { return paymentMethod; }
    @PropertyName("paymentMethod")
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    @PropertyName("createdAt")
    public long getCreatedAt() { return createdAt; }
    @PropertyName("createdAt")
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    @PropertyName("updatedAt")
    public Long getUpdatedAt() { return updatedAt; }
    @PropertyName("updatedAt")
    public void setUpdatedAt(Long updatedAt) { this.updatedAt = updatedAt; }

    // 'synced' không cần @PropertyName
    public int getSynced() { return synced; }
    public void setSynced(int synced) { this.synced = synced; }
    // --- KẾT THÚC SỬA LỖI B ---
}