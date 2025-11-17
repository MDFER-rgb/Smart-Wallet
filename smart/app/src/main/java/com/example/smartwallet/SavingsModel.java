package com.example.smartwallet;

public class SavingsModel {

    private double value;
    private int percentage;

    public SavingsModel() {
        // empty constructor needed for Firestore
    }

    public SavingsModel(double value, int percentage) {
        this.value = value;
        this.percentage = percentage;
    }

    public double getValue() {
        return value;
    }

    public int getPercentage() {
        return percentage;
    }
}
