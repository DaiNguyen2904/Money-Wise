package com.example.moneywise.ui.auth;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.moneywise.MainActivity;
import com.example.moneywise.R;
import com.example.moneywise.data.AppDatabase;
import com.example.moneywise.data.entity.Budget;
import com.example.moneywise.data.entity.Category;
import com.example.moneywise.data.entity.Expense;
import com.example.moneywise.data.entity.User;
import com.example.moneywise.repository.MoneyWiseRepository;
import com.example.moneywise.sync.SyncWorker;
import com.example.moneywise.utils.SessionManager;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private FirebaseAuth mAuth;
    private SessionManager mSessionManager;
    private MoneyWiseRepository mRepository;
    private ExecutorService mExecutor;
    private FirebaseFirestore mFirestore;
    private ProgressDialog mProgressDialog;
    private WorkManager mWorkManager;

    private CredentialManager mCredentialManager;

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
        mCredentialManager = CredentialManager.create(this);

        if (mAuth.getCurrentUser() != null && mSessionManager.isLoggedIn()) {
            goToMainActivity();
            return;
        }

        Button googleSignInButton = findViewById(R.id.btn_google_sign_in);
        googleSignInButton.setOnClickListener(v -> signInWithGoogle());
    }

    private void signInWithGoogle() {
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(getString(R.string.default_web_client_id))
                .setAutoSelectEnabled(false) // Tắt auto-select để dễ debug
                .setNonce(null) // Không dùng nonce cho đơn giản
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        showLoadingDialog("Đang đăng nhập...");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            mCredentialManager.getCredentialAsync(
                    this,
                    request,
                    null,
                    getMainExecutor(),
                    new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                        @Override
                        public void onResult(GetCredentialResponse result) {
                            handleSignIn(result);
                        }

                        @Override
                        public void onError(@NonNull GetCredentialException e) {
                            hideLoadingDialog();
                            Log.e(TAG, "Credential Manager Error: ", e);
                            Toast.makeText(LoginActivity.this,
                                    "Đăng nhập thất bại: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
            );
        }
    }

    private void handleSignIn(GetCredentialResponse result) {
        Credential credential = result.getCredential();

        Log.d(TAG, "Credential type: " + credential.getType());
        Log.d(TAG, "Credential class: " + credential.getClass().getName());

        // Kiểm tra nếu là GoogleIdTokenCredential
        if (credential instanceof GoogleIdTokenCredential) {
            GoogleIdTokenCredential googleIdTokenCredential = (GoogleIdTokenCredential) credential;
            String idToken = googleIdTokenCredential.getIdToken();
            Log.d(TAG, "Đăng nhập Google thành công với GoogleIdTokenCredential");
            firebaseAuthWithGoogle(idToken);
        }
        // Kiểm tra nếu là CustomCredential với type GoogleIdToken
        else if (credential instanceof CustomCredential) {
            CustomCredential customCredential = (CustomCredential) credential;

            if (GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(customCredential.getType())) {
                // Parse CustomCredential thành GoogleIdTokenCredential
                GoogleIdTokenCredential googleIdTokenCredential =
                        GoogleIdTokenCredential.createFrom(customCredential.getData());

                String idToken = googleIdTokenCredential.getIdToken();
                Log.d(TAG, "Đăng nhập Google thành công với CustomCredential");
                firebaseAuthWithGoogle(idToken);

            } else {
                // Credential type không được hỗ trợ
                hideLoadingDialog();
                Log.e(TAG, "Unsupported credential type: " + customCredential.getType());
                Toast.makeText(this, "Loại xác thực không được hỗ trợ", Toast.LENGTH_SHORT).show();
            }
        }
        else {
            hideLoadingDialog();
            Log.e(TAG, "Unexpected credential type: " + credential.getType());
            Toast.makeText(this, "Lỗi xác thực: Loại credential không hợp lệ", Toast.LENGTH_SHORT).show();
        }
    }

// Sửa lại hàm firebaseAuthWithGoogle (Cách tốt hơn):

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);

        mProgressDialog.setMessage("Đang xác thực...");
        Log.d(TAG, "Chuẩn bị gọi mAuth.signInWithCredential...");

        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Xác thực Firebase thành công.");

                        // Lấy cờ isNewUser từ Firebase
                        boolean isNewUser = task.getResult().getAdditionalUserInfo().isNewUser();

                        FirebaseUser user = mAuth.getCurrentUser();
                        String firebaseUid = user.getUid();

                        // Bây giờ bạn có thể lưu UserId bất cứ lúc nào
                        mSessionManager.saveUserId(firebaseUid);

                        // Dùng cờ isNewUser của Firebase để quyết định
                        if (isNewUser) {
                            Log.d(TAG, "Lần đầu đăng nhập (isNewUser=true), tạo dữ liệu mặc định...");
                            createDefaultData(user, () -> {
                                // TÀI KHOẢN MỚI: Cần kích hoạt để đẩy 9 danh mục lên
                                triggerImmediateSync();
                                hideLoadingDialog();
                                goToMainActivity();
                            });
                        } else {
                            // --- SỬA LỖI A: KIỂM TRA CSDL CỤC BỘ TRƯỚC ---
                            Log.d(TAG, "Tài khoản cũ (isNewUser=false), kiểm tra CSDL cục bộ...");
                            mExecutor.execute(() -> {
                                // Kiểm tra xem user đã tồn tại trong Room chưa
                                User localUser = mRepository.getUserById_Sync(firebaseUid);
                                if (localUser == null) {
                                    // CSDL cục bộ rỗng (ví dụ: cài lại app)
                                    // -> Chạy tải về ban đầu
                                    Log.d(TAG, "CSDL cục bộ rỗng. Đang chạy performInitialSync...");
                                    runOnUiThread(() -> mProgressDialog.setMessage("Đang tải dữ liệu của bạn..."));
                                    performInitialSync(user, () -> {
                                        runOnUiThread(() -> {
                                            hideLoadingDialog();
                                            goToMainActivity();
                                        });
                                    });
                                } else {
                                    // CSDL cục bộ ĐÃ CÓ DỮ LIỆU
                                    // -> Không chạy tải về ban đầu (để tránh ghi đè)
                                    // -> Chỉ cần kích hoạt SyncWorker để đẩy
                                    //    các thay đổi (Sửa/Xóa) đang chờ
                                    Log.d(TAG, "CSDL cục bộ đã có. Kích hoạt SyncWorker và vào app.");
                                    triggerImmediateSync();
                                    runOnUiThread(() -> {
                                        hideLoadingDialog();
                                        goToMainActivity();
                                    });
                                }
                            });
                            // --- KẾT THÚC SỬA LỖI A ---
                        }
                    } else {
                        Log.w(TAG, "Xác thực Firebase thất bại", task.getException());
                        hideLoadingDialog();
                        Toast.makeText(this, "Xác thực Firebase thất bại", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void performInitialSync(FirebaseUser user, Runnable onComplete) {
        mExecutor.execute(() -> {
            try {
                // --- BƯỚC BẮT BUỘC: TẠO USER TRONG ROOM TRƯỚC ---
                // (Sao chép logic từ createDefaultData)
                Log.d(TAG, "Đang tạo bản ghi User cục bộ cho tài khoản cũ...");
                String userId = user.getUid();
                String userEmail = user.getEmail();
                String userName = user.getDisplayName();
                User localUser = new User(userId, userEmail, userName, System.currentTimeMillis());
                mRepository.insertUser_Sync(localUser);
                // ----------------------------------------------------

                // (Phần còn lại giữ nguyên)
                Task<QuerySnapshot> categoriesTask = mFirestore
                        .collection("users").document(userId).collection("categories").get();

                Task<QuerySnapshot> expensesTask = mFirestore
                        .collection("users").document(userId).collection("expenses").get();

                Task<QuerySnapshot> budgetsTask = mFirestore
                        .collection("users").document(userId).collection("budgets").get();

                Tasks.await(categoriesTask);
                Tasks.await(expensesTask);
                Tasks.await(budgetsTask);

                // Lệnh insert này sẽ không bị lỗi nữa
                if (categoriesTask.isSuccessful()) {
                    List<Category> categories = categoriesTask.getResult().toObjects(Category.class);
                    for(Category c : categories) c.setSynced(1);
                    mRepository.insertCategories_Sync(categories); // <--- Lỗi xảy ra ở đây
                    Log.d(TAG, "Tải về thành công " + categories.size() + " danh mục.");
                }

                if (expensesTask.isSuccessful()) {
                    List<Expense> expenses = expensesTask.getResult().toObjects(Expense.class);
                    for(Expense e : expenses) e.setSynced(1);
                    mRepository.insertExpenses_Sync(expenses);
                    Log.d(TAG, "Tải về thành công " + expenses.size() + " giao dịch.");
                }

                if (budgetsTask.isSuccessful()) {
                    List<Budget> budgets = budgetsTask.getResult().toObjects(Budget.class);
                    for(Budget b : budgets) b.setSynced(1);
                    mRepository.insertBudgets_Sync(budgets);
                    Log.d(TAG, "Tải về thành công " + budgets.size() + " ngân sách.");
                }

                onComplete.run();

            } catch (Exception e) {
                Log.e(TAG, "Lỗi khi tải về ban đầu: ", e); // Lỗi sẽ được log ở đây
                runOnUiThread(() -> {
                    hideLoadingDialog();
                    Toast.makeText(this, "Lỗi khi tải dữ liệu cũ", Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    /**
     * CẬP NHẬT: Hàm này giờ sẽ chèn vào Room VÀ SYNC_QUEUE
     * (Chạy khi isFirstLogin() == true)
     */
    private void createDefaultData(FirebaseUser user, Runnable onComplete) {
        mExecutor.execute(() -> {
            // KHÔNG CẦN try-catch ở đây nữa

            String userId = user.getUid();
            String userEmail = user.getEmail();
            String userName = user.getDisplayName();

            Log.d(TAG, "Đang tạo User trong Room...");
            // 1. Tạo User trong Room
            User newUser = new User(userId, userEmail, userName, System.currentTimeMillis());
            mRepository.insertUser_Sync(newUser); // (Hàm này giữ nguyên)

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
            // 3. SỬA LẠI: Dùng hàm "insert" (có logic SYNC_QUEUE)
            // Thay vì insertCategories_Sync
            Log.d(TAG, "Đã tạo 9 danh mục, đang thêm vào Room và SYNC_QUEUE...");
            for (Category category : defaultCategories) {
                // Đánh dấu synced = 0 để SyncWorker đẩy lên
                category.setSynced(0);
                mRepository.insertCategory_Sync_WithQueue(category); // (Sẽ tạo hàm này)
            }

            // 4. Chạy về luồng UI và gọi onComplete
            runOnUiThread(onComplete);
        });
    }

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
                "MoneyWiseSyncJob",
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
        );
    }

    private void triggerImmediateSync() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest immediateSyncRequest =
                new OneTimeWorkRequest.Builder(SyncWorker.class)
                        .setConstraints(constraints)
                        .build();

        // DÙNG enqueueUniqueWork VỚI TÊN RIÊNG
        mWorkManager.enqueueUniqueWork(
                "MoneyWiseSyncJob_Immediate", // Tên duy nhất cho công việc 1 lần
                ExistingWorkPolicy.REPLACE, // Thay thế nếu có yêu cầu cũ
                immediateSyncRequest
        );
    }

    private void goToMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}