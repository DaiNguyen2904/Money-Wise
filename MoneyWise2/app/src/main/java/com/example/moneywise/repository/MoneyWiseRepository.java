package com.example.moneywise.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;

import com.example.moneywise.data.AppDatabase;
import com.example.moneywise.data.dao.*;
import com.example.moneywise.data.entity.*;


import java.util.List;

/**
 * Repository (Kho chứa) đóng vai trò là Nguồn sự thật duy nhất (Single Source of Truth).
 * Nó che giấu logic lấy dữ liệu (từ Room, từ Firebase) khỏi ViewModel.
 */
public class MoneyWiseRepository {

    // 1. Khai báo tất cả các DAO
    private UserDao mUserDao;
    private CategoryDao mCategoryDao;
    private ExpenseDao mExpenseDao;
    private BudgetDao mBudgetDao;
    private SyncQueueDao mSyncQueueDao;

    private AppDatabase mDatabase;

    // 2. Constructor
    public MoneyWiseRepository(Application application) {

        mDatabase = AppDatabase.getDatabase(application);
        // Khởi tạo CSDL và lấy tất cả các DAO
        AppDatabase db = AppDatabase.getDatabase(application);
        mUserDao = db.userDao();
        mCategoryDao = db.categoryDao();
        mExpenseDao = db.expenseDao();
        mBudgetDao = db.budgetDao();
        mSyncQueueDao = db.syncQueueDao();
    }

    // --- PHƯƠNG THỨC CHO LIVE DATA (Getters) ---
    // Room tự động chạy LiveData trên luồng nền. Chúng ta chỉ cần trả về.

    public LiveData<User> getUser(String userId) {
        return mUserDao.getUserById(userId);
    }

    public LiveData<List<Category>> getAllCategories(String userId) {
        return mCategoryDao.getAllCategories(userId);
    }

    public LiveData<List<Expense>> getAllExpenses(String userId) {
        return mExpenseDao.getAllExpenses(userId);
    }

    public LiveData<List<Expense>> getExpensesByDateRange(String userId, long start, long end) {
        return mExpenseDao.getExpensesByDateRange(userId, start, end);
    }

    public LiveData<Double> getSumForCategory(String userId, String categoryId, long start, long end) {
        return mExpenseDao.getSumForCategory(userId, categoryId, start, end);
    }

    // --- PHƯƠNG THỨC GHI (Insert, Update, Delete) ---
    // Phải chạy trên luồng nền (background) bằng Executor

    public void insertCategory(Category category) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            category.synced = 0;
            SyncQueue syncItem = new SyncQueue(
                    "CATEGORIES",
                    category.categoryId,
                    SyncAction.CREATE
            );

            mDatabase.runInTransaction(() -> {
                mCategoryDao.insert(category);
                mSyncQueueDao.insert(syncItem);
            });
        });
    }

    public void updateCategory(Category category) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            category.synced = 0;
            SyncQueue syncItem = new SyncQueue(
                    "CATEGORIES",
                    category.categoryId,
                    SyncAction.UPDATE
            );

            mDatabase.runInTransaction(() -> {
                mCategoryDao.update(category);
                mSyncQueueDao.insert(syncItem);
            });
        });
    }

    public void deleteCategory(Category category) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            SyncQueue syncItem = new SyncQueue(
                    "CATEGORIES",
                    category.categoryId,
                    SyncAction.DELETE
            );

            mDatabase.runInTransaction(() -> {
                mCategoryDao.delete(category);
                mSyncQueueDao.insert(syncItem);
            });
        });
    }

    public void insertExpense(Expense expense) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            // Chuẩn bị dữ liệu
            expense.synced = 0; // Đánh dấu là chưa đồng bộ
            // (ID, createdAt, userId nên được gán từ ViewModel/Activity)

            // Tạo mục trong hàng đợi
            SyncQueue syncItem = new SyncQueue(
                    "EXPENSES",
                    expense.expenseId,
                    SyncAction.CREATE
            );

            // Chạy trong Transaction
            mDatabase.runInTransaction(() -> {
                mExpenseDao.insert(expense);
                mSyncQueueDao.insert(syncItem);
            });
        });
    }

    public void updateExpense(Expense expense) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            expense.synced = 0; // Đánh dấu là chưa đồng bộ
            expense.updatedAt = System.currentTimeMillis(); // Cập nhật thời gian

            SyncQueue syncItem = new SyncQueue(
                    "EXPENSES",
                    expense.expenseId,
                    SyncAction.UPDATE
            );

            mDatabase.runInTransaction(() -> {
                mExpenseDao.update(expense);
                mSyncQueueDao.insert(syncItem);
            });
        });
    }

    public void deleteExpense(Expense expense) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            // Chúng ta không thực sự xóa bản ghi,
            // mà chúng ta cần GHI LẠI HÀNH ĐỘNG XÓA
            SyncQueue syncItem = new SyncQueue(
                    "EXPENSES",
                    expense.expenseId,
                    SyncAction.DELETE
            );

            mDatabase.runInTransaction(() -> {
                mExpenseDao.delete(expense); // Xóa cục bộ
                mSyncQueueDao.insert(syncItem); // Ghi lại hành động xóa để đẩy lên server
            });
        });
    }



    // (Tương tự cho các phương thức của Budget và User...)

    // --- PHƯƠNG THỨC CHO SYNC_QUEUE ---
    // (Đây là logic nâng cao cho dịch vụ đồng bộ)


    public List<SyncQueue> getPendingSyncItems() {
        return mSyncQueueDao.getPendingItems();
    }

    public void deleteSyncItem(SyncQueue item) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            mSyncQueueDao.delete(item);
        });
    }

    // (Bạn cũng có thể cần hàm updateSyncItem để tăng retry_count)
    public void updateSyncItem(SyncQueue item) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            mSyncQueueDao.update(item);
        });
    }

    public void insertBudget(Budget budget) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            budget.synced = 0;
            SyncQueue syncItem = new SyncQueue(
                    "BUDGETS",
                    budget.budgetId,
                    SyncAction.CREATE
            );

            mDatabase.runInTransaction(() -> {
                mBudgetDao.insert(budget);
                mSyncQueueDao.insert(syncItem);
            });
        });
    }

    // --- CÁC HÀM TRUY CẬP ĐỒNG BỘ CHO WORKER ---
    // (Không dùng LiveData, không dùng Executor)

    public Expense getExpenseById_Sync(String id) {
        return mExpenseDao.getExpenseById(id);
    }

    public Category getCategoryById_Sync(String id) {
        return mCategoryDao.getCategoryById(id);
    }

    public Budget getBudgetById_Sync(String id) {
        return mBudgetDao.getBudgetById(id);
    }

    public void deleteBudget(Budget budget) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            SyncQueue syncItem = new SyncQueue(
                    "BUDGETS",
                    budget.budgetId,
                    SyncAction.DELETE
            );

            mDatabase.runInTransaction(() -> {
                mBudgetDao.delete(budget);
                mSyncQueueDao.insert(syncItem);
            });
        });
    }

    public LiveData<List<Budget>> getAllBudgets(String currentUserId) {
        return mBudgetDao.getAllBudgets(currentUserId);
    }
}