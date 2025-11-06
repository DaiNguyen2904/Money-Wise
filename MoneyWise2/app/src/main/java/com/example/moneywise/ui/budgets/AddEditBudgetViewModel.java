package com.example.moneywise.ui.budgets;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;


import com.example.moneywise.data.entity.Category;
import com.example.moneywise.repository.MoneyWiseRepository;

import java.util.List;

public class AddEditBudgetViewModel extends AndroidViewModel {

    private MoneyWiseRepository mRepository;
    private LiveData<List<Category>> mAllCategories;

    public AddEditBudgetViewModel(@NonNull Application application) {
        super(application);
        mRepository = new MoneyWiseRepository(application);

        String currentUserId = "USER_ID_TAM_THOI"; // TODO: Thay thế ID thật
        mAllCategories = mRepository.getAllCategories(currentUserId);
    }

    /**
     * Cung cấp danh sách Danh mục (Categories) cho Spinner
     */
    public LiveData<List<Category>> getAllCategories() {
        return mAllCategories;
    }
}