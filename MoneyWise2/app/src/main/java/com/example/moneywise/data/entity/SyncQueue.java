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
    private int id;

    @NonNull
    @ColumnInfo(name = "table_name")
    private String tableName; // "EXPENSES", "CATEGORIES", v.v.

    @NonNull
    @ColumnInfo(name = "record_id")
    private String recordId; // UUID của bản ghi

    @NonNull
    @ColumnInfo(name = "action")
    private SyncAction action; // "CREATE", "UPDATE", "DELETE"

    @ColumnInfo(name = "timestamp")
    private long timestamp;

    @ColumnInfo(name = "retry_count")
    private int retryCount;

    public SyncQueue() {}

    public SyncQueue(@NonNull String tableName, @NonNull String recordId, @NonNull SyncAction action) {
        this.tableName = tableName;
        this.recordId = recordId;
        this.action = action;
        this.timestamp = System.currentTimeMillis(); // Tự động gán thời gian
        this.retryCount = 0;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @NonNull
    public String getTableName() {
        return tableName;
    }

    public void setTableName(@NonNull String tableName) {
        this.tableName = tableName;
    }

    @NonNull
    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(@NonNull String recordId) {
        this.recordId = recordId;
    }

    @NonNull
    public SyncAction getAction() {
        return action;
    }

    public void setAction(@NonNull SyncAction action) {
        this.action = action;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }
}