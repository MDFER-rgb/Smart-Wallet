package com.example.smartwallet;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;

public class SpendingBreakdownSheet extends BottomSheetDialogFragment {

    private static final String PREF_EXPENSES = "expenses";
    private static final String PREF_CATEGORIES = "categories";
    private static final String PREF_BUDGET = "budget";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.bottom_spending_breakdown, container, false);

        RecyclerView rv = view.findViewById(R.id.rvCategoryList);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        // Load category spending + limits + counts
        List<CategoryModel> categories = loadCategoryData();

        // Load total spent
        double totalSpent = loadTotalSpent();

        // Set Adapter
        CategoryAdapter adapter = new CategoryAdapter(categories, totalSpent);
        rv.setAdapter(adapter);

        return view;
    }

    // ---------------------------------------------------------
    // 🔥 LOAD CATEGORY DATA FROM SHARED PREFERENCES (REAL DATA)
    // ---------------------------------------------------------
    private List<CategoryModel> loadCategoryData() {
        SharedPreferences spendPrefs = requireActivity().getSharedPreferences(PREF_EXPENSES, 0);
        SharedPreferences limitPrefs = requireActivity().getSharedPreferences(PREF_CATEGORIES, 0);

        List<CategoryModel> list = new ArrayList<>();

        // ⁍ Food
        list.add(new CategoryModel(
                "Food",
                spendPrefs.getFloat("FoodSpent", 0f),
                spendPrefs.getInt("FoodCount", 0),
                R.drawable.ic_food
        ));

        // ⁍ Transport
        list.add(new CategoryModel(
                "Transport",
                spendPrefs.getFloat("TransportSpent", 0f),
                spendPrefs.getInt("TransportCount", 0),
                R.drawable.ic_transport
        ));

        // ⁍ Shopping
        list.add(new CategoryModel(
                "Shopping",
                spendPrefs.getFloat("ShoppingSpent", 0f),
                spendPrefs.getInt("ShoppingCount", 0),
                R.drawable.ic_shopping
        ));

        // ⁍ Bills
        list.add(new CategoryModel(
                "Bills",
                spendPrefs.getFloat("BillsSpent", 0f),
                spendPrefs.getInt("BillsCount", 0),
                R.drawable.ic_bills
        ));

        return list;
    }

    // ---------------------------------------------------------
    // 🔥 TOTAL SPENT FROM SHARED PREFERENCES
    // ---------------------------------------------------------
    private double loadTotalSpent() {
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREF_BUDGET, 0);
        return Double.parseDouble(prefs.getString("totalSpent", "0"));
    }
}
