package com.example.moneywise.ui.user;

import android.app.Application;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.moneywise.data.entity.User;
import com.example.moneywise.repository.MoneyWiseRepository;
import com.example.moneywise.utils.SessionManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class UserViewModel extends AndroidViewModel {

    private MoneyWiseRepository mRepository;
    private SessionManager mSessionManager;
    private FirebaseAuth mAuth;
    private String currentUserId;

    // LiveData để báo cho Fragment biết khi nào đang tải (VD: đang lưu)
    private MutableLiveData<Boolean> mIsLoading = new MutableLiveData<>(false);
    public LiveData<Boolean> getIsLoading() {
        return mIsLoading;
    }

    // LiveData để báo thành công
    private MutableLiveData<String> mSuccessMessage = new MutableLiveData<>();
    public LiveData<String> getSuccessMessage() {
        return mSuccessMessage;
    }

    // LiveData để báo lỗi
    private MutableLiveData<String> mErrorMessage = new MutableLiveData<>();
    public LiveData<String> getErrorMessage() {
        return mErrorMessage;
    }


    public UserViewModel(@NonNull Application application) {
        super(application);
        mRepository = new MoneyWiseRepository(application);
        mSessionManager = new SessionManager(application);
        mAuth = FirebaseAuth.getInstance();
        currentUserId = mSessionManager.getUserId();
    }

    /**
     * Lấy thông tin User (Tên, SĐT) từ Room/Firestore
     * (Chúng ta sẽ dùng LiveData từ Repository)
     */
    public LiveData<User> getUserData() {
        return mRepository.getUser(currentUserId);
    }

    /**
     * Cập nhật Tên và SĐT
     */
    public void updateProfile(String newName, String newPhone) {
        mIsLoading.setValue(true);
        FirebaseUser fUser = mAuth.getCurrentUser();
        if (fUser == null) {
            mErrorMessage.setValue("Người dùng không hợp lệ.");
            mIsLoading.setValue(false);
            return;
        }

        mRepository.updateUserProfileData(fUser, newName, newPhone, (success, message) -> {
            if (success) {
                mSuccessMessage.setValue(message);
            } else {
                mErrorMessage.setValue(message);
            }
            mIsLoading.setValue(false);
        });
    }

    /**
     * Cập nhật ảnh đại diện
     */
    public void updateAvatar(Uri imageUri) {
        mIsLoading.setValue(true);
        FirebaseUser fUser = mAuth.getCurrentUser();
        if (fUser == null) {
            mErrorMessage.setValue("Người dùng không hợp lệ.");
            mIsLoading.setValue(false);
            return;
        }

        mRepository.updateUserAvatar(fUser, imageUri, (success, message) -> {
            if (success) {
                mSuccessMessage.setValue(message);
                // (Fragment sẽ tự động thấy ảnh mới qua FirebaseUser)
            } else {
                mErrorMessage.setValue(message);
            }
            mIsLoading.setValue(false);
        });
    }

    // Callback interface
    public interface OnProfileUpdateCallback {
        void onComplete(boolean success, String message);
    }
}