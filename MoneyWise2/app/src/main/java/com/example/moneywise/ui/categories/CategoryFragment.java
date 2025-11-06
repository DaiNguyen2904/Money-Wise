package com.example.moneywise.ui.categories;

import static android.app.Activity.RESULT_OK;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.moneywise.R;
import com.example.moneywise.data.entity.Category;
import com.google.android.material.floatingactionbutton.FloatingActionButton;


import java.util.ArrayList;
import java.util.UUID;

public class CategoryFragment extends Fragment implements CategoryAdapter.OnCategoryItemClickListener {

    private CategoryViewModel mCategoryViewModel;
    private CategoryAdapter mAdapter;
    private ListView mListView;
    private FloatingActionButton mFab;

    // Launcher để mở màn hình Thêm/Sửa Danh mục
    private ActivityResultLauncher<Intent> mAddEditCategoryLauncher;

    // (Chúng ta sẽ cần các hằng số (EXTRA_) cho màn hình Thêm/Sửa,
    //  giống như đã làm cho Expense)
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate layout của Fragment
        return inflater.inflate(R.layout.fragment_category, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Kiểm tra Pick Mode (GIỮ NGUYÊN, nhưng dùng getArguments() thay vì getIntent())
        // (Chúng ta sẽ sửa lại phần Pick Mode này sau nếu cần, tạm thời cứ để logic Quản lý)
        // if (getIntent().hasExtra(EXTRA_PICK_MODE)) { ... }

        // 1. Ánh xạ Views (dùng "view.")
        mListView = view.findViewById(R.id.list_view_categories);
        mFab = view.findViewById(R.id.fab_add_category);

        // 2. Khởi tạo Adapter (dùng requireContext())
        mAdapter = new CategoryAdapter(requireContext(), new ArrayList<>(), this);
        mListView.setAdapter(mAdapter);

        // 3. Lấy ViewModel (dùng 'this')
        mCategoryViewModel = new ViewModelProvider(this).get(CategoryViewModel.class);

        // 4. Theo dõi LiveData (dùng getViewLifecycleOwner())
        mCategoryViewModel.getAllCategories().observe(getViewLifecycleOwner(), categories -> {
            mAdapter.setData(categories);
        });

        // 5. Đăng ký Launcher
        registerAddEditLauncher(); // (Hàm này giữ nguyên)

        // 6. Xử lý FAB Click (dùng requireContext())
        mFab.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), AddEditCategoryActivity.class);
            mAddEditCategoryLauncher.launch(intent);
        });
    }

    // --- TẤT CẢ CÁC HÀM LOGIC CŨ GIỮ NGUYÊN (CHỈ SỬA CONTEXT) ---

    private void registerAddEditLauncher() {
        mAddEditCategoryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            // Lấy dữ liệu
                            String name = data.getStringExtra(AddEditCategoryActivity.EXTRA_CATEGORY_NAME);
                            String icon = data.getStringExtra(AddEditCategoryActivity.EXTRA_CATEGORY_ICON);
                            String color = data.getStringExtra(AddEditCategoryActivity.EXTRA_CATEGORY_COLOR);

                            // Kiểm tra xem có ID không (để biết là Sửa hay Thêm)
                            if (data.hasExtra(AddEditCategoryActivity.EXTRA_CATEGORY_ID)) {
                                // --- CHẾ ĐỘ SỬA ---
                                String cateId = data.getStringExtra(AddEditCategoryActivity.EXTRA_CATEGORY_ID);

                                // (Tạm thời chúng ta sẽ tạo 1 đối tượng mới,
                                //  nhưng lý tưởng nhất là bạn lấy đối tượng cũ từ CSDL)
                                Category categoryToUpdate = new Category();
                                categoryToUpdate.categoryId = cateId; // ID cũ
                                categoryToUpdate.name = name;
                                categoryToUpdate.icon = icon;
                                categoryToUpdate.color = color;
                                // (updatedAt, userId sẽ do Repository xử lý)

                                mCategoryViewModel.update(categoryToUpdate);
                                Toast.makeText(requireContext(), "Đã cập nhật danh mục!", Toast.LENGTH_SHORT).show();

                            } else {
                                // --- CHẾ ĐỘ THÊM MỚI ---
                                Category newCategory = new Category();
                                newCategory.categoryId = UUID.randomUUID().toString();
                                newCategory.name = name;
                                newCategory.icon = icon;
                                newCategory.color = color;
                                newCategory.createdAt = System.currentTimeMillis();

                                mCategoryViewModel.insert(newCategory);
                                Toast.makeText(requireContext(), "Đã lưu danh mục!", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
    }



    @Override
    public void onEditClick(Category category) {
        // Đây là logic cũ từ OnItemClickListener
        Intent intent = new Intent(requireContext(), AddEditCategoryActivity.class);
        intent.putExtra(AddEditCategoryActivity.EXTRA_CATEGORY_ID, category.categoryId);
        intent.putExtra(AddEditCategoryActivity.EXTRA_CATEGORY_NAME, category.name);
        intent.putExtra(AddEditCategoryActivity.EXTRA_CATEGORY_ICON, category.icon);
        intent.putExtra(AddEditCategoryActivity.EXTRA_CATEGORY_COLOR, category.color);

        mAddEditCategoryLauncher.launch(intent);
    }

    /**
     * Được gọi TỪ ADAPTER khi nhấn nút "Xóa"
     */
    @Override
    public void onDeleteClick(Category category) {
        // Gọi hàm hiển thị Dialog (chúng ta sẽ tạo hàm này)
        showDeleteConfirmationDialog(category);
    }

    // --- BƯỚC 5: THÊM HÀM DIALOG XÁC NHẬN ---
    // (Bạn có thể copy hàm này từ ExpenseActivity và sửa tên)
    private void showDeleteConfirmationDialog(Category categoryToDelete) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Xác nhận Xóa");
        builder.setMessage("Bạn có chắc chắn muốn xóa danh mục '" + categoryToDelete.name + "' không?");
        // (Cảnh báo: Xóa danh mục có thể ảnh hưởng đến các giao dịch cũ...)

        builder.setPositiveButton("Xóa", (dialog, which) -> {
            mCategoryViewModel.delete(categoryToDelete);
            Toast.makeText(requireContext(), "Đã xóa danh mục", Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("Hủy", (dialog, which) -> {
            if (dialog != null) dialog.dismiss();
        });

        builder.create().show();
    }

}