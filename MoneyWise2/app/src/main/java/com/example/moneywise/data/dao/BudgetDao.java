package com.example.moneywise.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.moneywise.data.entity.Budget;

import java.util.List;

@Dao
public interface BudgetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Budget budget);

    @Update
    void update(Budget budget);

    @Delete
    void delete(Budget budget);

    // Lấy tất cả ngân sách (cho màn hình Quản lý ngân sách)
    @Query("SELECT * FROM BUDGETS WHERE user_id = :userId")
    LiveData<List<Budget>> getAllBudgets(String userId);

    // (Dùng cho đồng bộ)
    @Query("SELECT * FROM BUDGETS WHERE synced = 0")
    List<Budget> getUnsyncedBudgets();
    @Query("SELECT * FROM BUDGETS WHERE budget_id = :budgetId LIMIT 1")
    Budget getBudgetById(String budgetId);

    @Query("UPDATE BUDGETS SET synced = :syncedValue WHERE budget_id = :budgetId")
    void setSynced(String budgetId, int syncedValue);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Budget> budgets);
}