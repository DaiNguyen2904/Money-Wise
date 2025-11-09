package com.example.moneywise.data.model;

import com.example.moneywise.data.entity.Category;
import com.example.moneywise.data.entity.Expense;

/**
 * Một lớp POJO đơn giản để chứa một Expense và Category tương ứng.
 * Đây KHÔNG phải là một bảng (entity) trong Room.
 */
public class ExpenseWithCategory {
    public final Expense expense;
    public final Category category;

    public ExpenseWithCategory(Expense expense, Category category) {
        this.expense = expense;
        this.category = category;
    }
}
