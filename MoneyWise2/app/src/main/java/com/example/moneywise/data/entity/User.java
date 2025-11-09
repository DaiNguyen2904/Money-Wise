package com.example.moneywise.data.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

import com.google.firebase.firestore.PropertyName;

@Entity(
        tableName = "USERS",
        indices = {@Index(value = {"email"}, unique = true)}
)
public class User {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "user_id")
    @PropertyName("userId") // <-- THÊM
    private String userId;

    @NonNull
    @ColumnInfo(name = "email")
    @PropertyName("email") // <-- THÊM
    private String email;

    @ColumnInfo(name = "display_name")
    @PropertyName("displayName") // <-- THÊM
    private String displayName;

    @ColumnInfo(name = "avatar_url")
    @PropertyName("avatarUrl") // <-- THÊM
    private String avatarUrl;

    @ColumnInfo(name = "phone")
    @PropertyName("phone") // <-- THÊM
    private String phone;

    @ColumnInfo(name = "created_at")
    @PropertyName("createdAt") // <-- THÊM
    private long createdAt;

    @ColumnInfo(name = "updated_at")
    @PropertyName("updatedAt") // <-- THÊM
    private Long updatedAt;

    @ColumnInfo(name = "synced")
    private int synced; // (Không cần @PropertyName)

    public User() {}

    public User(@NonNull String userId, @NonNull String email, String displayName, long createdAt) {
        this.userId = userId;
        this.email = email;
        this.displayName = displayName;
        this.createdAt = createdAt;
        this.synced = 1;
    }

    // --- THÊM @PropertyName VÀO GETTERS/SETTERS ---
    @NonNull
    @PropertyName("userId") // <-- THÊM
    public String getUserId() {
        return userId;
    }
    @PropertyName("userId") // <-- THÊM
    public void setUserId(@NonNull String userId) {
        this.userId = userId;
    }

    @NonNull
    @PropertyName("email") // <-- THÊM
    public String getEmail() {
        return email;
    }
    @PropertyName("email") // <-- THÊM
    public void setEmail(@NonNull String email) {
        this.email = email;
    }

    @PropertyName("displayName") // <-- THÊM
    public String getDisplayName() {
        return displayName;
    }
    @PropertyName("displayName") // <-- THÊM
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @PropertyName("avatarUrl") // <-- THÊM
    public String getAvatarUrl() {
        return avatarUrl;
    }
    @PropertyName("avatarUrl") // <-- THÊM
    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    @PropertyName("phone") // <-- THÊM
    public String getPhone() {
        return phone;
    }
    @PropertyName("phone") // <-- THÊM
    public void setPhone(String phone) {
        this.phone = phone;
    }

    @PropertyName("createdAt") // <-- THÊM
    public long getCreatedAt() {
        return createdAt;
    }
    @PropertyName("createdAt") // <-- THÊM
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    @PropertyName("updatedAt") // <-- THÊM
    public Long getUpdatedAt() {
        return updatedAt;
    }
    @PropertyName("updatedAt") // <-- THÊM
    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt;
    }

    // (synced không cần)
    public int getSynced() {
        return synced;
    }
    public void setSynced(int synced) {
        this.synced = synced;
    }
}