package com.example.moneywise.data.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;
import androidx.room.TypeConverters;

import com.example.moneywise.utils.Converters;

@Entity(tableName = "SYNC_QUEUE")
@TypeConverters(Converters.class)
public class SyncQueue {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    public int id;

    @NonNull
    @ColumnInfo(name = "table_name")
    public String tableName; // "EXPENSES", "CATEGORIES", v.v.

    @NonNull
    @ColumnInfo(name = "record_id")
    public String recordId; // UUID của bản ghi

    @NonNull
    @ColumnInfo(name = "action")
    public SyncAction action; // "CREATE", "UPDATE", "DELETE"

    @ColumnInfo(name = "timestamp")
    public long timestamp;

    @ColumnInfo(name = "retry_count")
    public int retryCount;

    public SyncQueue() {}

    public SyncQueue(@NonNull String tableName, @NonNull String recordId, @NonNull SyncAction action) {
        this.tableName = tableName;
        this.recordId = recordId;
        this.action = action;
        this.timestamp = System.currentTimeMillis(); // Tự động gán thời gian
        this.retryCount = 0;
    }
}