package com.example.moneywise.repository;

import static android.content.ContentValues.TAG;

import android.app.Application;
import android.net.Uri;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.example.moneywise.data.AppDatabase;
import com.example.moneywise.data.dao.*;
import com.example.moneywise.data.entity.*;
import com.example.moneywise.ui.user.UserViewModel;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private FirebaseFirestore mFirestore = FirebaseFirestore.getInstance();
    private FirebaseStorage mStorage = FirebaseStorage.getInstance();
    // Danh sách các trình lắng nghe
    private List<ListenerRegistration> mListeners = new ArrayList<>();

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

    public void startRealtimeSync(String userId) {
        // 1. Dừng các trình lắng nghe cũ (nếu có, ví dụ: user vừa đăng xuất/đăng nhập lại)
        stopRealtimeSync();

        // --- 2. LẮNG NGHE BẢNG "EXPENSES" ---
        ListenerRegistration expensesListener = mFirestore
                .collection("users").document(userId).collection("expenses")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Lỗi khi lắng nghe Expenses:", error);
                        return;
                    }
                    if (snapshots == null) return;

                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        Expense expense = dc.getDocument().toObject(Expense.class);
                        expense.setSynced(1); // Dữ liệu từ server luôn là "đã đồng bộ"

                        switch (dc.getType()) {
                            case ADDED:
                                // Dữ liệu MỚI từ server
                                // (Dùng insert (REPLACE) để chèn hoặc cập nhật)
                                insertExpense_Sync(expense);
                                break;
                            case MODIFIED:
                                // Dữ liệu BỊ THAY ĐỔI từ server
                                updateExpense_Sync(expense);
                                break;
                            case REMOVED:
                                // Dữ liệu BỊ XÓA từ server
                                deleteExpense_Sync(expense);
                                break;
                        }
                    }
                });
        mListeners.add(expensesListener);

        // --- 3. LẮNG NGHE BẢNG "CATEGORIES" ---
        ListenerRegistration categoriesListener = mFirestore
                .collection("users").document(userId).collection("categories")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) return;
                    if (snapshots == null) return;
                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        Category category = dc.getDocument().toObject(Category.class);
                        category.setSynced(1);
                        switch (dc.getType()) {
                            case ADDED:
                                // --- SỬA LOGIC "UPSERT" ---
                                // 1. Thử chèn (dùng IGNORE)
                                insertCategory_Sync(category);
                                // 2. Luôn cập nhật (để đảm bảo bản ghi mới nhất)
                                updateCategory_Sync(category);
                                // --- KẾT THÚC SỬA ---
                                break;
                            case MODIFIED:
                                updateCategory_Sync(category);
                                break;
                            case REMOVED:
                                deleteCategory_Sync(category);
                                break;
                        }
                    }
                });
        mListeners.add(categoriesListener);

        // --- 4. LẮNG NGHE BẢNG "BUDGETS" ---
        ListenerRegistration budgetListener = mFirestore
                .collection("users").document(userId).collection("budgets")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Lỗi khi lắng nghe Budgets:", error);
                        return;
                    }
                    if (snapshots == null) return;

                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        // --- SỬA LỖI ---
                        // Đổi Category.class -> Budget.class
                        Budget budget = dc.getDocument().toObject(Budget.class);
                        budget.setSynced(1);

                        // Gọi đúng các hàm _Sync của Budget
                        switch (dc.getType()) {
                            case ADDED: insertBudget_Sync(budget); break;
                            case MODIFIED: updateBudget_Sync(budget); break;
                            case REMOVED: deleteBudget_Sync(budget); break;
                        }
                        // --- KẾT THÚC SỬA LỖI ---
                    }
                });
        mListeners.add(budgetListener); // SỬA: Phải là budgetListener

    }

    /**
     * HÀM MỚI: Dừng tất cả Lắng nghe (khi đăng xuất)
     */
    public void stopRealtimeSync() {
        for (ListenerRegistration listener : mListeners) {
            listener.remove();
        }
        mListeners.clear();
    }

    // --- 5. TẠO CÁC HÀM GHI ĐỒNG BỘ MỚI (CHO TRÌNH LẮNG NGHE) ---

    public void insertExpense_Sync(Expense expense) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            mExpenseDao.insert(expense); // (Hàm insert đã có OnConflictStrategy.REPLACE)
        });
    }

    public void updateExpense_Sync(Expense expense) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            mExpenseDao.update(expense);
        });
    }

    public void deleteExpense_Sync(Expense expense) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            mExpenseDao.delete(expense);
        });
    }

// ----- CÁC HÀM _Sync CHO CATEGORY (Dùng cho "Pull") -----

    public void insertCategory_Sync(Category category) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            mCategoryDao.insert(category); // Dùng REPLACE
        });
    }

    public void updateCategory_Sync(Category category) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            mCategoryDao.update(category);
        });
    }

    public void deleteCategory_Sync(Category category) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            mCategoryDao.delete(category);
        });
    }

    // ----- CÁC HÀM _Sync CHO BUDGET (Dùng cho "Pull") -----

    public void insertBudget_Sync(Budget budget) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            mBudgetDao.insert(budget); // Dùng REPLACE
        });
    }

    public void updateBudget_Sync(Budget budget) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            mBudgetDao.update(budget);
        });
    }

    public void deleteBudget_Sync(Budget budget) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            mBudgetDao.delete(budget);
        });
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
            category.setSynced(0);
            SyncQueue syncItem = new SyncQueue(
                    "CATEGORIES",
                    category.getCategoryId(),
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
            category.setSynced(0);
            SyncQueue syncItem = new SyncQueue(
                    "CATEGORIES",
                    category.getCategoryId(),
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
                    category.getCategoryId(),
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
            expense.setSynced(0); // Đánh dấu là chưa đồng bộ
            // (ID, createdAt, userId nên được gán từ ViewModel/Activity)

            // Tạo mục trong hàng đợi
            SyncQueue syncItem = new SyncQueue(
                    "EXPENSES",
                    expense.getExpenseId(),
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
            expense.setSynced(0); // Đánh dấu là chưa đồng bộ
            expense.setUpdatedAt(System.currentTimeMillis()); // Cập nhật thời gian

            SyncQueue syncItem = new SyncQueue(
                    "EXPENSES",
                    expense.getExpenseId(),
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
                    expense.getExpenseId(),
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
            budget.setSynced(0);
            SyncQueue syncItem = new SyncQueue(
                    "BUDGETS",
                    budget.getBudgetId(),
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
                    budget.getBudgetId(),
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
    public void updateBudget(Budget budget) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            budget.setSynced(0);

            SyncQueue syncItem = new SyncQueue(
                    "BUDGETS",
                    budget.getBudgetId(),
                    SyncAction.UPDATE
            );

            mDatabase.runInTransaction(() -> {
                mBudgetDao.update(budget);
                mSyncQueueDao.insert(syncItem);
            });
        });
    }

    public User getUserById_Sync(String id) {
        // Hàm này chạy đồng bộ (sẽ được gọi từ Executor của LoginActivity)
        return mUserDao.getUserById_Sync(id);
    }
    // --- THÊM 2 HÀM NÀY (Cho LoginActivity) ---
    public void insertUser_Sync(User user) {
        // Hàm này chạy đồng bộ (trên luồng đã gọi nó)
        user.setSynced(1); // User này lấy từ Firebase, coi như đã đồng bộ
        mUserDao.insert(user);
        // (Không cần SYNC_QUEUE vì đang tạo user)
    }

    public void insertCategories_Sync(List<Category> categories) {
        // Hàm này chạy đồng bộ
        // (Chúng ta có thể set synced = 1 vì đây là dữ liệu mặc định)
        mCategoryDao.insertAll(categories);
    }

    // --- THÊM HÀM NÀY (Cho SyncWorker) ---
    public void markAsSynced_Sync(String tableName, String recordId) {
        // Hàm này chạy đồng bộ (trên luồng của Worker)
        switch (tableName) {
            case "EXPENSES":
                // (Bạn sẽ cần tạo hàm "setSynced" trong DAO)
                mExpenseDao.setSynced(recordId, 1);
                break;
            case "CATEGORIES":
                mCategoryDao.setSynced(recordId, 1);
                break;
            case "BUDGETS":
                mBudgetDao.setSynced(recordId, 1);
                break;
        }
    }
    /**
     * Chèn hàng loạt Expenses (dùng khi tải về ban đầu)
     * Chạy trên luồng nền.
     */
    public void insertExpenses_Sync(List<Expense> expenses) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            mExpenseDao.insertAll(expenses);
        });
    }

    /**
     * Chèn hàng loạt Budgets (dùng khi tải về ban đầu)
     * Chạy trên luồng nền.
     */
    public void insertBudgets_Sync(List<Budget> budgets) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            mBudgetDao.insertAll(budgets);
        });
    }

    public void insertCategory_Sync_WithQueue(Category category) {
        // Hàm này chạy đồng bộ (trên luồng của Executor)
        SyncQueue syncItem = new SyncQueue(
                "CATEGORIES",
                category.getCategoryId(),
                SyncAction.CREATE
        );

        mDatabase.runInTransaction(() -> {
            mCategoryDao.insert(category);
            mSyncQueueDao.insert(syncItem);
        });
    }
    // --- === HÀM MỚI ĐỂ CẬP NHẬT USER PROFILE === ---

    /**
     * Cập nhật thông tin (Tên, SĐT) của người dùng trên
     * 1. Firebase Auth (chỉ Tên)
     * 2. Firebase Firestore (Tên và SĐT)
     * 3. Room (Tên và SĐT)
     */
    public void updateUserProfileData(FirebaseUser fUser, String newName, String newPhone,
                                      UserViewModel.OnProfileUpdateCallback callback) {

        // 1. Cập nhật Firebase Auth (Display Name)
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(newName)
                .build();
        Task<Void> authTask = fUser.updateProfile(profileUpdates);

        // 2. Cập nhật Firestore
        DocumentReference userDocRef = mFirestore.collection("users").document(fUser.getUid());
        Map<String, Object> updates = new HashMap<>();
        updates.put("displayName", newName);
        updates.put("phone", newPhone);
        Task<Void> firestoreTask = userDocRef.set(updates, SetOptions.merge());

        // Chạy cả hai song song
        Tasks.whenAll(authTask, firestoreTask).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // 3. Cập nhật Room (chạy nền)
                AppDatabase.databaseWriteExecutor.execute(() -> {
                    User localUser = mUserDao.getUserById_Sync(fUser.getUid());
                    if (localUser != null) {
                        localUser.setDisplayName(newName);
                        localUser.setPhone(newPhone);
                        localUser.setSynced(1); // Coi như đã sync
                        mUserDao.update(localUser);
                    } else {
                        // (Trường hợp hiếm: chưa có user local)
                        User newUser = new User(fUser.getUid(), fUser.getEmail(), newName, System.currentTimeMillis());
                        newUser.setPhone(newPhone);
                        mUserDao.insert(newUser);
                    }
                });
                callback.onComplete(true, "Cập nhật thành công!");
            } else {
                callback.onComplete(false, "Cập nhật thất bại: " + task.getException().getMessage());
            }
        });
    }

    /**
     * Cập nhật ảnh đại diện của người dùng
     * 1. Tải ảnh lên Firebase Storage
     * 2. Lấy URL ảnh
     * 3. Cập nhật URL vào Firebase Auth
     * 4. Cập nhật URL vào Firestore
     * 5. Cập nhật URL vào Room
     */
    public void updateUserAvatar(FirebaseUser fUser, Uri imageUri,
                                 UserViewModel.OnProfileUpdateCallback callback) {

        // 1. Tải ảnh lên Storage
        StorageReference avatarRef = mStorage.getReference()
                .child("avatars")
                .child(fUser.getUid() + ".jpg"); // Đặt tên file theo UID

        UploadTask uploadTask = avatarRef.putFile(imageUri);

        uploadTask.continueWithTask(task -> {
            if (!task.isSuccessful()) {
                throw task.getException();
            }
            // 2. Lấy URL ảnh
            return avatarRef.getDownloadUrl();
        }).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Uri downloadUri = task.getResult();
                String imageUrl = downloadUri.toString();

                // 3. Cập nhật Firebase Auth (Avatar URL)
                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                        .setPhotoUri(downloadUri)
                        .build();
                Task<Void> authTask = fUser.updateProfile(profileUpdates);

                // 4. Cập nhật Firestore
                DocumentReference userDocRef = mFirestore.collection("users").document(fUser.getUid());
                Task<Void> firestoreTask = userDocRef.update("avatarUrl", imageUrl);

                Tasks.whenAll(authTask, firestoreTask).addOnCompleteListener(updateTask -> {
                    if (updateTask.isSuccessful()) {
                        // 5. Cập nhật Room
                        AppDatabase.databaseWriteExecutor.execute(() -> {
                            User localUser = mUserDao.getUserById_Sync(fUser.getUid());
                            if (localUser != null) {
                                localUser.setAvatarUrl(imageUrl);
                                mUserDao.update(localUser);
                            }
                        });
                        callback.onComplete(true, "Cập nhật ảnh thành công!");
                    } else {
                        callback.onComplete(false, "Lỗi khi lưu URL ảnh: " + updateTask.getException().getMessage());
                    }
                });

            } else {
                // Lỗi khi tải ảnh lên (Bước 1 hoặc 2)
                callback.onComplete(false, "Tải ảnh lên thất bại: " + task.getException().getMessage());
            }
        });
    }
}