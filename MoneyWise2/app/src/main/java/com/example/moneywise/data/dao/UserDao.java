package com.example.moneywise.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.moneywise.data.entity.User;

@Dao
public interface UserDao {

    // Dùng REPLACE: Nếu chèn user có cùng user_id, nó sẽ được cập nhật
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(User user);

    @Update
    void update(User user);

    // Lấy thông tin user (dùng LiveData để UI tự cập nhật khi có thay đổi)
    @Query("SELECT * FROM USERS WHERE user_id = :userId LIMIT 1")
    LiveData<User> getUserById(String userId);

    // --- HÀM MỚI ---
    // Lấy user đồng bộ (không dùng LiveData)
    // Dùng cho LoginActivity và Repository
    @Query("SELECT * FROM USERS WHERE user_id = :userId LIMIT 1")
    User getUserById_Sync(String userId);
}