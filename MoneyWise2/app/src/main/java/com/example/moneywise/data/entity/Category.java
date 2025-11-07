package com.example.moneywise.data.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

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

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "category_id")
    private String categoryId; // UUID

    @NonNull
    @ColumnInfo(name = "user_id")
    private String userId;

    @NonNull
    @ColumnInfo(name = "name")
    private String name;

    @ColumnInfo(name = "icon")
    private String icon;

    @ColumnInfo(name = "color")
    private String color;

    @ColumnInfo(name = "is_default")
    private int isDefault; // 0 hoặc 1

    @ColumnInfo(name = "created_at")
    private long createdAt;

    @ColumnInfo(name = "synced")
    private int synced;

    public Category() {}

    public Category(@NonNull String categoryId, @NonNull String userId, @NonNull String name,
                    String icon, String color, int isDefault, long createdAt) {
        this.categoryId = categoryId;
        this.userId = userId;
        this.name = name;
        this.icon = icon;
        this.color = color;
        this.isDefault = isDefault;
        this.createdAt = createdAt;
        this.synced = 1; // Dữ liệu mặc định coi như đã đồng bộ
    }

    @NonNull
    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(@NonNull String categoryId) {
        this.categoryId = categoryId;
    }

    @NonNull
    public String getUserId() {
        return userId;
    }

    public void setUserId(@NonNull String userId) {
        this.userId = userId;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public int getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(int isDefault) {
        this.isDefault = isDefault;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public int getSynced() {
        return synced;
    }

    public void setSynced(int synced) {
        this.synced = synced;
    }
}