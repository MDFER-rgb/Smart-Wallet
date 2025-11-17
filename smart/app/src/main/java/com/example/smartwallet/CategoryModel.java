package com.example.smartwallet;

public class CategoryModel {
    public String name;
    public double amount;
    public int transactions;
    public int icon;

    public CategoryModel(String name, double amount, int transactions, int icon) {
        this.name = name;
        this.amount = amount;
        this.transactions = transactions;
        this.icon = icon;
    }
}
