package com.example.moneywise.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Lớp tiện ích đơn giản để lưu và lấy ID người dùng đã đăng nhập
 */
public class SessionManager {

    private static final String PREF_NAME = "MoneyWiseSession";
    private static final String KEY_USER_ID = "USER_ID";
    private static final String KEY_IS_FIRST_LOGIN = "IS_FIRST_LOGIN";
    private static final String KEY_HAS_SEEN_ONBOARDING = "HAS_SEEN_ONBOARDING";

    private SharedPreferences mPrefs;
    private SharedPreferences.Editor mEditor;
    private Context mContext;

    public SessionManager(Context context) {
        this.mContext = context;
        mPrefs = mContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        mEditor = mPrefs.edit();
    }

    /**
     * Lưu ID của Firebase User
     */
    public void saveUserId(String userId) {
        mEditor.putString(KEY_USER_ID, userId);
        mEditor.commit();
    }

    /**
     * Lấy ID của Firebase User
     * (Trả về null nếu chưa đăng nhập)
     */
    public String getUserId() {
        return mPrefs.getString(KEY_USER_ID, null);
    }

    /**
     * Kiểm tra xem đã đăng nhập chưa
     */
    public boolean isLoggedIn() {
        return getUserId() != null;
    }

    /**
     * Kiểm tra xem đây có phải là lần đầu tiên người dùng này đăng nhập không.
     * Nó chỉ trả về 'true' MỘT LẦN DUY NHẤT.
     */
    public boolean isFirstLogin() {
        // Lấy giá trị, mặc định là 'true' (lần đầu)
        boolean isFirst = mPrefs.getBoolean(KEY_IS_FIRST_LOGIN, true);

        if (isFirst) {
            // Nếu đúng là lần đầu, lưu lại là 'false' cho lần sau
            mEditor.putBoolean(KEY_IS_FIRST_LOGIN, false);
            mEditor.commit();
        }
        return isFirst;
    }

    // --- HÀM MỚI: Đánh dấu là đã xem Onboarding ---
    /**
     * Đánh dấu rằng người dùng đã hoàn thành màn hình giới thiệu (Onboarding)
     */
    public void setHasSeenOnboarding() {
        mEditor.putBoolean(KEY_HAS_SEEN_ONBOARDING, true);
        mEditor.commit();
    }

    // --- HÀM MỚI: Kiểm tra xem đã xem Onboarding chưa ---
    /**
     * Kiểm tra xem người dùng đã xem màn hình giới thiệu chưa
     * @return true nếu đã xem, false nếu chưa (mặc định)
     */
    public boolean hasSeenOnboarding() {
        return mPrefs.getBoolean(KEY_HAS_SEEN_ONBOARDING, false);
    }

    /**
     * Xóa session (Đăng xuất) (Không xoá Onboarding)
     */
    public void logout() {
        // mEditor.clear(); // <-- Không dùng hàm này nữa

        // Chỉ xóa các key liên quan đến phiên đăng nhập
        mEditor.remove(KEY_USER_ID);
        mEditor.putBoolean(KEY_IS_FIRST_LOGIN, true); // Đặt lại cờ này

        mEditor.commit();
    }
}