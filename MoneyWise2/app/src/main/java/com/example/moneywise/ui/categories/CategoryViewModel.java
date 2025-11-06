package com.example.moneywise.ui.categories;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.moneywise.data.entity.Category;
import com.example.moneywise.repository.MoneyWiseRepository;
import com.example.moneywise.utils.SessionManager;

import java.util.List;

public class CategoryViewModel extends AndroidViewModel {

    private MoneyWiseRepository mRepository;
    private LiveData<List<Category>> mAllCategories;

    // Giả sử lấy ID người dùng (sẽ thay thế sau)
    private String currentUserId;

    public CategoryViewModel(@NonNull Application application) {
        super(application);
        mRepository = new MoneyWiseRepository(application);
        mAllCategories = mRepository.getAllCategories(currentUserId);
        SessionManager sessionManager = new SessionManager(application);
        currentUserId = sessionManager.getUserId(); // Lấy ID đã lưu
        mAllCategories = mRepository.getAllCategories(currentUserId);
    }

    /**
     * Cung cấp LiveData<List<Category>> cho View (Activity)
     */
    public LiveData<List<Category>> getAllCategories() {
        return mAllCategories;
    }

    /**
     * Ủy quyền hành động 'insert' cho Repository
     */
    public void insert(Category category) {
        // Gán userId trước khi lưu
        category.userId = currentUserId;
        mRepository.insertCategory(category);
    }

    /**
     * Ủy quyền hành động 'update' cho Repository
     */
    public void update(Category category) {
        category.userId = currentUserId;
        mRepository.updateCategory(category);
    }

    /**
     * Ủy quyền hành động 'delete' cho Repository
     */
    public void delete(Category category) {
        mRepository.deleteCategory(category);
    }
}
