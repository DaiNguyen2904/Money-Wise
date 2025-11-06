package com.example.moneywise.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.moneywise.data.entity.SyncQueue;

import java.util.List;

@Dao
public interface SyncQueueDao {

    // Thêm một hành động vào hàng đợi
    @Insert
    void insert(SyncQueue syncQueue);

    @Update
    void update(SyncQueue syncQueue); // Dùng để cập nhật retry_count

    // Xóa một hành động (sau khi đã đồng bộ thành công)
    @Delete
    void delete(SyncQueue syncQueue);

    // Lấy TẤT CẢ các hành động đang chờ, theo thứ tự cũ -> mới (FIFO)
    // Dịch vụ đồng bộ (WorkManager) sẽ gọi hàm này
    @Query("SELECT * FROM SYNC_QUEUE ORDER BY timestamp ASC")
    List<SyncQueue> getPendingItems();
}