package com.example.moneywise.ui.auth;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.moneywise.MainActivity; // Màn hình chính (Fragments)
import com.example.moneywise.R;
import com.example.moneywise.data.AppDatabase;
import com.example.moneywise.data.entity.Budget;
import com.example.moneywise.data.entity.Category;
import com.example.moneywise.data.entity.Expense;
import com.example.moneywise.data.entity.User;
import com.example.moneywise.repository.MoneyWiseRepository;
import com.example.moneywise.sync.SyncWorker;
import com.example.moneywise.utils.SessionManager; // Lớp tiện ích
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth;
    private ActivityResultLauncher<Intent> mGoogleSignInLauncher;
    private SessionManager mSessionManager;
    private MoneyWiseRepository mRepository;
    private ExecutorService mExecutor; // Lấy từ AppDatabase

    private FirebaseFirestore mFirestore; // Cần để tải dữ liệu
    private ProgressDialog mProgressDialog; // Hiển thị "Đang tải"
    private WorkManager mWorkManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        mSessionManager = new SessionManager(this);
        mRepository = new MoneyWiseRepository(getApplication());
        mExecutor = AppDatabase.databaseWriteExecutor;
        mFirestore = FirebaseFirestore.getInstance();
        mWorkManager = WorkManager.getInstance(getApplicationContext());

        // --- 1. KIỂM TRA XEM ĐÃ ĐĂNG NHẬP CHƯA ---
        if (mAuth.getCurrentUser() != null && mSessionManager.isLoggedIn()) {
            // Nếu đã đăng nhập (phiên Firebase + SharedPreferences)
            // -> Đi thẳng vào màn hình chính
            goToMainActivity();
            return;
        }

        // --- 2. CẤU HÌNH GOOGLE SIGN-IN ---
        // (Rất quan trọng: Phải yêu cầu ID Token)
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // Lấy từ google-services.json
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // --- 3. ĐĂNG KÝ LAUNCHER (Thay thế cho onActivityResult) ---
        mGoogleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        try {
                            // Lấy tài khoản Google thành công
                            GoogleSignInAccount account = task.getResult(ApiException.class);
                            Log.d(TAG, "Đăng nhập Google thành công, xác thực với Firebase...");
                            // Dùng token để xác thực với Firebase
                            firebaseAuthWithGoogle(account.getIdToken());
                        } catch (ApiException e) {
                            Log.w(TAG, "Đăng nhập Google thất bại", e);
                            Toast.makeText(this, "Đăng nhập Google thất bại", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        // --- 4. XỬ LÝ NÚT NHẤN ---
        SignInButton googleSignInButton = findViewById(R.id.btn_google_sign_in);
        googleSignInButton.setOnClickListener(v -> signIn());
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        mGoogleSignInLauncher.launch(signInIntent);
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);

        // Hiển thị "Đang đăng nhập..."
        showLoadingDialog("Đang xác thực...");

        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Xác thực Firebase thành công.");
                        FirebaseUser user = mAuth.getCurrentUser();
                        String firebaseUid = user.getUid();

                        // 1. Lưu ID thật (như cũ)
                        mSessionManager.saveUserId(firebaseUid);

                        schedulePeriodicSync();


                        if (mSessionManager.isFirstLogin()) {
                            // --- TÀI KHOẢN MỚI ---
                            Log.d(TAG, "Lần đầu đăng nhập, tạo dữ liệu mặc định...");
                            // (Hàm này chạy trên luồng nền)
                            createDefaultData(user, () -> {
                                triggerImmediateSync();
                                hideLoadingDialog();
                                goToMainActivity();
                            });
                        } else {
                            // --- TÀI KHOẢN CŨ ---
                            Log.d(TAG, "Tài khoản cũ, thực hiện tải về ban đầu...");
                            mProgressDialog.setMessage("Đang tải dữ liệu của bạn...");
                            // (Hàm này chạy trên luồng nền)
                            performInitialSync(firebaseUid, () -> {
                                // Sau khi tải xong, đi vào app
                                triggerImmediateSync();
                                hideLoadingDialog();
                                goToMainActivity();
                            });
                        }

                    } else {
                        // Đăng nhập Firebase thất bại
                        Log.w(TAG, "Xác thực Firebase thất bại", task.getException());
                        hideLoadingDialog();
                        Toast.makeText(this, "Xác thực Firebase thất bại", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * HÀM MỚI: Tải toàn bộ dữ liệu cũ từ Firebase về Room
     * (Chạy khi isFirstLogin() == false)
     */
    private void performInitialSync(String userId, Runnable onComplete) {
        mExecutor.execute(() -> {
            try {
                // 1. Tạo 3 Task để lấy 3 collection
                Task<QuerySnapshot> categoriesTask = mFirestore
                        .collection("users").document(userId).collection("categories").get();

                Task<QuerySnapshot> expensesTask = mFirestore
                        .collection("users").document(userId).collection("expenses").get();

                Task<QuerySnapshot> budgetsTask = mFirestore
                        .collection("users").document(userId).collection("budgets").get();

                // 2. Chờ (await) cho cả 3 Task hoàn thành
                Tasks.await(categoriesTask);
                Tasks.await(expensesTask);
                Tasks.await(budgetsTask);

                // 3. Xử lý kết quả Categories
                if (categoriesTask.isSuccessful()) {
                    List<Category> categories = categoriesTask.getResult().toObjects(Category.class);
                    // (Đánh dấu đã đồng bộ)
                    for(Category c : categories) c.synced = 1;
                    mRepository.insertCategories_Sync(categories);
                    Log.d(TAG, "Tải về thành công " + categories.size() + " danh mục.");
                }

                // 4. Xử lý kết quả Expenses
                if (expensesTask.isSuccessful()) {
                    List<Expense> expenses = expensesTask.getResult().toObjects(Expense.class);
                    for(Expense e : expenses) e.synced = 1;
                    mRepository.insertExpenses_Sync(expenses);
                    Log.d(TAG, "Tải về thành công " + expenses.size() + " giao dịch.");
                }

                // 5. Xử lý kết quả Budgets
                if (budgetsTask.isSuccessful()) {
                    List<Budget> budgets = budgetsTask.getResult().toObjects(Budget.class);
                    for(Budget b : budgets) b.synced = 1;
                    mRepository.insertBudgets_Sync(budgets);
                    Log.d(TAG, "Tải về thành công " + budgets.size() + " ngân sách.");
                }

                // 6. Chạy về luồng UI và gọi hàm onComplete (để đi vào MainActivity)
                runOnUiThread(onComplete);

            } catch (Exception e) {
                Log.e(TAG, "Lỗi khi tải về ban đầu: ", e);
                runOnUiThread(() -> {
                    hideLoadingDialog();
                    Toast.makeText(this, "Lỗi khi tải dữ liệu cũ", Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    /**
     * HÀM MỚI: (Logic này được chuyển từ AppDatabase Callback)
     * Tạo dữ liệu mặc định cho người dùng MỚI
     */
    private void createDefaultData(FirebaseUser user, Runnable onComplete) {
        mExecutor.execute(() -> {
            String userId = user.getUid(); // Dùng ID thật
            String userEmail = user.getEmail();
            String userName = user.getDisplayName();

            // 1. Tạo User trong Room
            User newUser = new User(userId, userEmail, userName, System.currentTimeMillis());
            mRepository.insertUser_Sync(newUser); // (Sẽ tạo hàm này)

            // 2. Tạo 9 Categories mặc định
            List<Category> defaultCategories = new ArrayList<>();
            defaultCategories.add(new Category(UUID.randomUUID().toString(), userId, "Ăn uống", "ic_food", "#FF5733", 1, System.currentTimeMillis()));
            defaultCategories.add(new Category(UUID.randomUUID().toString(), userId, "Phương tiện", "ic_car", "#33FF57", 1, System.currentTimeMillis()));
            defaultCategories.add(new Category(UUID.randomUUID().toString(), userId, "Giải trí", "ic_clapperboard", "#3357FF", 1, System.currentTimeMillis()));
            defaultCategories.add(new Category(UUID.randomUUID().toString(), userId, "Nhà ở", "ic_home", "#FF33A1", 1, System.currentTimeMillis()));
            defaultCategories.add(new Category(UUID.randomUUID().toString(), userId, "Y tế", "ic_medicine", "#A133FF", 1, System.currentTimeMillis()));
            defaultCategories.add(new Category(UUID.randomUUID().toString(), userId, "Học tập", "ic_study", "#33FFF6", 1, System.currentTimeMillis()));
            defaultCategories.add(new Category(UUID.randomUUID().toString(), userId, "Mỹ phẩm", "ic_cosmetics", "#F6FF33", 1, System.currentTimeMillis()));
            defaultCategories.add(new Category(UUID.randomUUID().toString(), userId, "Quần áo", "ic_clothes", "#FF8C33", 1, System.currentTimeMillis()));
            defaultCategories.add(new Category(UUID.randomUUID().toString(), userId, "Khác", "ic_other", "#808080", 1, System.currentTimeMillis()));

            mRepository.insertCategories_Sync(defaultCategories); // (Sẽ tạo hàm này)
            runOnUiThread(onComplete);
        });
    }

    // (Đây là code ví dụ cho chức năng Đăng xuất)
//    private void handleLogout() {
//        FirebaseAuth.getInstance().signOut(); // Đăng xuất Firebase Auth
//        GoogleSignIn.getClient(this, gso).signOut(); // Đăng xuất Google
//
//        new SessionManager(this).logout(); // Xóa user_id cục bộ
//
//        // Dừng lắng nghe
//        new MoneyWiseRepository(getApplication()).stopRealtimeSync();
//
//        // Quay về màn hình Login
//        Intent intent = new Intent(this, LoginActivity.class);
//        startActivity(intent);
//        finish();
//    }
    // --- CÁC HÀM TIỆN ÍCH CHO LOADING DIALOG ---
    private void showLoadingDialog(String message) {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setCancelable(false);
        }
        mProgressDialog.setMessage(message);
        mProgressDialog.show();
    }

    private void hideLoadingDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    /**
     * HÀM MỚI: Lên lịch chạy định kỳ (1 giờ/lần)
     * (Code này chuyển từ MyApplication sang)
     */
    private void schedulePeriodicSync() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest syncRequest =
                new PeriodicWorkRequest.Builder(
                        SyncWorker.class,
                        1, TimeUnit.HOURS)
                        .setConstraints(constraints)
                        .build();

        mWorkManager.enqueueUniquePeriodicWork(
                "MoneyWiseSyncJob", // Tên duy nhất
                ExistingPeriodicWorkPolicy.KEEP, // Giữ lịch cũ nếu đã có
                syncRequest
        );
    }

    /**
     * HÀM MỚI: Kích hoạt một Worker chạy NGAY BÂY GIỜ
     * (Để đẩy dữ liệu vừa tạo offline)
     */
    private void triggerImmediateSync() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest immediateSyncRequest =
                new OneTimeWorkRequest.Builder(SyncWorker.class)
                        .setConstraints(constraints)
                        .build();

        mWorkManager.enqueue(immediateSyncRequest);
    }

    private void goToMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish(); // Đóng LoginActivity, không cho quay lại
    }
}