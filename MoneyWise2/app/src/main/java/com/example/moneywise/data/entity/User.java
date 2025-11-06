package com.example.moneywise.data.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(
        tableName = "USERS",
        indices = {@Index(value = {"email"}, unique = true)}
)
public class User {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "user_id")
    public String userId; // UUID

    @NonNull
    @ColumnInfo(name = "email")
    public String email;

    @ColumnInfo(name = "display_name")
    public String displayName;

    @ColumnInfo(name = "avatar_url")
    public String avatarUrl;

    @ColumnInfo(name = "phone")
    public String phone;

    @ColumnInfo(name = "created_at")
    public long createdAt; // Timestamp (long)

    @ColumnInfo(name = "updated_at")
    public Long updatedAt; // Dùng Long (wrapper) để cho phép null

    @ColumnInfo(name = "synced")
    public int synced; // 0 hoặc 1

    // Room sẽ sử dụng constructor mặc định này
    public User() {}

    public User(@NonNull String userId, @NonNull String email, String displayName, long createdAt) {
        this.userId = userId;
        this.email = email;
        this.displayName = displayName;
        this.createdAt = createdAt;
        this.synced = 1; // User mặc định coi như đã đồng bộ
    }
}