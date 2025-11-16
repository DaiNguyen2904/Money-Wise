package com.example.moneywise.data.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

import com.google.firebase.firestore.PropertyName;

@Entity(
        tableName = "CATEGORIES",
        foreignKeys = @ForeignKey(
                entity = User.class,
                parentColumns = "user_id",
                childColumns = "user_id",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index(value = "user_id")}
)
public class Category {
    // --- SỬA LỖI B: THÊM @PropertyName ---
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "category_id")
    @PropertyName("categoryId")
    private String categoryId;

    @NonNull
    @ColumnInfo(name = "user_id")
    @PropertyName("userId")
    private String userId;

    @NonNull
    @ColumnInfo(name = "name")
    @PropertyName("name")
    private String name;

    @ColumnInfo(name = "icon")
    @PropertyName("icon")
    private String icon;

    @ColumnInfo(name = "color")
    @PropertyName("color")
    private String color;

    @ColumnInfo(name = "is_default")
    @PropertyName("isDefault")
    private int isDefault;

    @ColumnInfo(name = "created_at")
    @PropertyName("createdAt")
    private long createdAt;

    @ColumnInfo(name = "synced")
    private int synced;
    // --- KẾT THÚC SỬA LỖI B ---

    public Category() {}

    // Constructor (giữ nguyên, không cần @PropertyName)


    public Category(@NonNull String categoryId, @NonNull String userId, @NonNull String name, String icon, String color, int isDefault, long createdAt) {
        this.categoryId = categoryId;
        this.userId = userId;
        this.name = name;
        this.icon = icon;
        this.color = color;
        this.isDefault = isDefault;
        this.createdAt = createdAt;
    }

    // --- SỬA LẠI GETTERS ĐỂ DÙNG @PropertyName ---
    @NonNull
    @PropertyName("categoryId")
    public String getCategoryId() { return categoryId; }
    @PropertyName("categoryId")
    public void setCategoryId(@NonNull String categoryId) { this.categoryId = categoryId; }

    @NonNull
    @PropertyName("userId")
    public String getUserId() { return userId; }
    @PropertyName("userId")
    public void setUserId(@NonNull String userId) { this.userId = userId; }

    @NonNull
    @PropertyName("name")
    public String getName() { return name; }
    @PropertyName("name")
    public void setName(@NonNull String name) { this.name = name; }

    @PropertyName("icon")
    public String getIcon() { return icon; }
    @PropertyName("icon")
    public void setIcon(String icon) { this.icon = icon; }

    @PropertyName("color")
    public String getColor() { return color; }
    @PropertyName("color")
    public void setColor(String color) { this.color = color; }

    @PropertyName("isDefault")
    public int getIsDefault() { return isDefault; }
    @PropertyName("isDefault")
    public void setIsDefault(int isDefault) { this.isDefault = isDefault; }

    @PropertyName("createdAt")
    public long getCreatedAt() { return createdAt; }
    @PropertyName("createdAt")
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    // 'synced' không cần @PropertyName
    public int getSynced() { return synced; }
    public void setSynced(int synced) { this.synced = synced; }
    // --- KẾT THÚC SỬA LỖI B ---
}