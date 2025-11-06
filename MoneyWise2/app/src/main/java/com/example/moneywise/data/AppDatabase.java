package com.example.moneywise.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.moneywise.data.dao.BudgetDao;
import com.example.moneywise.data.dao.CategoryDao;
import com.example.moneywise.data.dao.ExpenseDao;
import com.example.moneywise.data.dao.SyncQueueDao;
import com.example.moneywise.data.dao.UserDao;
import com.example.moneywise.data.entity.Budget;
import com.example.moneywise.data.entity.Category;
import com.example.moneywise.data.entity.Expense;
import com.example.moneywise.data.entity.SyncQueue;
import com.example.moneywise.data.entity.User;
import com.example.moneywise.utils.Converters;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// 1. Chú thích @Database: Liệt kê TẤT CẢ 5 entity của bạn
//    Và đặt phiên bản (version) là 1.
@Database(entities = {
        User.class,
        Category.class,
        Expense.class,
        Budget.class,
        SyncQueue.class
}, version = 1, exportSchema = false) // Tắt exportSchema cho đơn giản
@TypeConverters(Converters.class)
public abstract class AppDatabase extends RoomDatabase {

    // 2. Khai báo các DAO abstract để Room có thể "nhìn thấy" chúng
    public abstract UserDao userDao();
    public abstract CategoryDao categoryDao();
    public abstract ExpenseDao expenseDao();
    public abstract BudgetDao budgetDao();
    public abstract SyncQueueDao syncQueueDao();

    // 3. Bắt đầu phần Singleton
    private static volatile AppDatabase INSTANCE;
    private static final String DATABASE_NAME = "money_wise_db";

    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    private static RoomDatabase.Callback sRoomDatabaseCallback = new RoomDatabase.Callback() {

        /**
         * Được gọi khi CSDL được TẠO LẦN ĐẦU TIÊN
         * (Sau khi gỡ cài đặt, hoặc lần đầu cài)
         */
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);

            // Chạy việc tạo dữ liệu mặc định trên luồng nền
            databaseWriteExecutor.execute(() -> {

                // --- BẮT ĐẦU SỬA ĐỔI ---

                // 1. LẤY CẢ HAI DAO
                UserDao userDao = INSTANCE.userDao();
                CategoryDao categoryDao = INSTANCE.categoryDao();

                // 2. CHÈN USER TRƯỚC
                String userId = "USER_ID_TAM_THOI"; // ID phải khớp nhau

                User defaultUser = new User(
                        userId,
                        "test@example.com",
                        "Người dùng Test",
                        System.currentTimeMillis()
                );
                userDao.insert(defaultUser); // CHÈN USER VÀO BẢNG USERS

                // --- KẾT THÚC SỬA ĐỔI ---

                // 3. Bây giờ mới chèn CATEGORIES
                List<Category> defaultCategories = new ArrayList<>();
                // (userId đã được định nghĩa ở trên)

                // THAY THẾ BẰNG 9 MỤC NÀY
                defaultCategories.add(new Category(UUID.randomUUID().toString(), userId, "Ăn uống", "ic_food", "#FF5733", 1, System.currentTimeMillis()));
                defaultCategories.add(new Category(UUID.randomUUID().toString(), userId, "Phương tiện", "ic_car", "#33FF57", 1, System.currentTimeMillis()));
                defaultCategories.add(new Category(UUID.randomUUID().toString(), userId, "Giải trí", "ic_clapperboard", "#3357FF", 1, System.currentTimeMillis()));
                defaultCategories.add(new Category(UUID.randomUUID().toString(), userId, "Nhà ở", "ic_home", "#FF33A1", 1, System.currentTimeMillis()));
                defaultCategories.add(new Category(UUID.randomUUID().toString(), userId, "Y tế", "ic_medicine", "#A133FF", 1, System.currentTimeMillis()));
                defaultCategories.add(new Category(UUID.randomUUID().toString(), userId, "Học tập", "ic_study", "#33FFF6", 1, System.currentTimeMillis()));
                defaultCategories.add(new Category(UUID.randomUUID().toString(), userId, "Mỹ phẩm", "ic_cosmetics", "#F6FF33", 1, System.currentTimeMillis()));
                defaultCategories.add(new Category(UUID.randomUUID().toString(), userId, "Quần áo", "ic_clothes", "#FF8C33", 1, System.currentTimeMillis()));
                defaultCategories.add(new Category(UUID.randomUUID().toString(), userId, "Khác", "ic_other", "#808080", 1, System.currentTimeMillis()));

                categoryDao.insertAll(defaultCategories);
            });
        }
    };
    // 4. Phương thức Singleton để lấy CSDL
    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            // Khối synchronized để đảm bảo an toàn luồng
            // (tránh 2 luồng cùng tạo CSDL một lúc)
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    // 5. Xây dựng CSDL
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    DATABASE_NAME
                            )
                            // Quan trọng: Khi phát triển, nếu bạn thay đổi cấu trúc bảng
                            // (ví dụ: thêm cột), hàm này sẽ xóa CSDL cũ và tạo mới.
                            // Trong sản phẩm thật, bạn sẽ phải dùng .addMigrations()
                            .fallbackToDestructiveMigration()
                            .addCallback(sRoomDatabaseCallback)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
