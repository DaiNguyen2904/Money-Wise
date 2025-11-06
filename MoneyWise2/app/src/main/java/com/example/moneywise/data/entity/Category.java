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
    public String categoryId; // UUID

    @NonNull
    @ColumnInfo(name = "user_id")
    public String userId;

    @NonNull
    @ColumnInfo(name = "name")
    public String name;

    @ColumnInfo(name = "icon")
    public String icon;

    @ColumnInfo(name = "color")
    public String color;

    @ColumnInfo(name = "is_default")
    public int isDefault; // 0 hoặc 1

    @ColumnInfo(name = "created_at")
    public long createdAt;

    @ColumnInfo(name = "synced")
    public int synced;

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
}