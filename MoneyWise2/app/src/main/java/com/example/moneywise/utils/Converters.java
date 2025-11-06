package com.example.moneywise.utils;

import androidx.room.TypeConverter;

import com.example.moneywise.data.entity.BudgetPeriod;
import com.example.moneywise.data.entity.SyncAction;

public class Converters {

    // --- Bộ chuyển đổi cho SyncAction ---
    @TypeConverter
    public static String fromSyncAction(SyncAction action) {
        return action == null ? null : action.name();
    }

    @TypeConverter
    public static SyncAction toSyncAction(String actionString) {
        return actionString == null ? null : SyncAction.valueOf(actionString);
    }
}