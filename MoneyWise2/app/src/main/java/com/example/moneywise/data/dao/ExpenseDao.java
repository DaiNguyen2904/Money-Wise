package com.example.moneywise.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.moneywise.data.entity.Expense;

import java.util.List;

@Dao
public interface ExpenseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Expense expense);

    @Update
    void update(Expense expense);

    @Delete
    void delete(Expense expense);

    // Lấy tất cả giao dịch (cho ListView lịch sử)
    @Query("SELECT * FROM EXPENSES WHERE user_id = :userId ORDER BY date DESC, created_at DESC")
    LiveData<List<Expense>> getAllExpenses(String userId);

    // Lấy giao dịch trong 1 khoảng thời gian (cho Báo cáo, Lọc)
    @Query("SELECT * FROM EXPENSES WHERE user_id = :userId AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    LiveData<List<Expense>> getExpensesByDateRange(String userId, long startDate, long endDate);

    // Lấy TỔNG TIỀN đã chi cho 1 danh mục trong 1 khoảng thời gian
    // (Rất quan trọng cho việc tính toán Ngân sách - UC-14)
    @Query("SELECT SUM(amount) FROM EXPENSES WHERE user_id = :userId AND category_id = :categoryId AND date BETWEEN :startDate AND :endDate")
    LiveData<Double> getSumForCategory(String userId, String categoryId, long startDate, long endDate);

    // Lấy TỔNG TIỀN đã chi (cho ngân sách tổng)
    @Query("SELECT SUM(amount) FROM EXPENSES WHERE user_id = :userId AND date BETWEEN :startDate AND :endDate")
    LiveData<Double> getTotalSum(String userId, long startDate, long endDate);

    // (Dùng cho đồng bộ)
    @Query("SELECT * FROM EXPENSES WHERE synced = 0")
    List<Expense> getUnsyncedExpenses();

    @Query("SELECT * FROM EXPENSES WHERE expense_id = :expenseId LIMIT 1")
    Expense getExpenseById(String expenseId);
}