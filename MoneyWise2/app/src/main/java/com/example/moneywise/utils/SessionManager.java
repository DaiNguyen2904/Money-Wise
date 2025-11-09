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

    /**
     * Xóa session (Đăng xuất)
     */
    public void logout() {
        mEditor.clear();
        mEditor.putBoolean(KEY_IS_FIRST_LOGIN, true);
        mEditor.commit();
    }
}