package com.example.moneywise.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.moneywise.data.entity.Category;

import java.util.List;

@Dao
public interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Category category);

    @Update
    void update(Category category);

    @Delete
    void delete(Category category);

    // Lấy tất cả danh mục của 1 user (cho màn hình Quản lý danh mục)
    @Query("SELECT * FROM CATEGORIES WHERE user_id = :userId ORDER BY name ASC")
    LiveData<List<Category>> getAllCategories(String userId);


    // (Dùng cho đồng bộ) Lấy các danh mục chưa đồng bộ
    @Query("SELECT * FROM CATEGORIES WHERE synced = 0")
    List<Category> getUnsyncedCategories();

    @Query("SELECT * FROM CATEGORIES WHERE category_id = :categoryId LIMIT 1")
    Category getCategoryById(String categoryId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Category> categories);
}