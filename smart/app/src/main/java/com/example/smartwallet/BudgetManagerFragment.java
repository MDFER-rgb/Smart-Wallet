package com.example.smartwallet;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class BudgetManagerFragment extends Fragment {

    TextView tvBudget, tvSpent, tvRemaining;

    FirebaseFirestore db;
    FirebaseAuth auth;

    public BudgetManagerFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_budget_manager, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        tvBudget = view.findViewById(R.id.tvBMTotalBudget);
        tvSpent = view.findViewById(R.id.tvBMTotalSpent);
        tvRemaining = view.findViewById(R.id.tvBMRemaining);

        loadBudgetUI();

        View cardEdit = view.findViewById(R.id.cardEditMonthlyBudget);
        cardEdit.setOnClickListener(v -> showEditDialog());
    }

    // -------------------------------------------------------------
    // ⭐ Load Budget + Total Spent (CRASH SAFE)
    // -------------------------------------------------------------
    private void loadBudgetUI() {
        if (auth.getCurrentUser() == null) return;
        String uid = auth.getCurrentUser().getUid();

        db.collection("budget").document(uid).get()
                .addOnSuccessListener(doc -> {

                    Double monthlyValue = doc.getDouble("monthlyBudget");
                    double monthly = monthlyValue != null ? monthlyValue : 0;

                    // Now load spent from categories
                    db.collection("expenses")
                            .document(uid)
                            .collection("categories")
                            .get()
                            .addOnSuccessListener(q -> {

                                double spent = 0;

                                for (var d : q) {
                                    Double s = d.getDouble("spent");
                                    if (s != null) spent += s;
                                }

                                double remaining = monthly - spent;

                                tvBudget.setText("$" + String.format(Locale.getDefault(),"%.2f", monthly));
                                tvSpent.setText("$" + String.format(Locale.getDefault(),"%.2f", spent));
                                tvRemaining.setText("$" + String.format(Locale.getDefault(),"%.2f", remaining));
                            })
                            .addOnFailureListener(e -> {
                                tvBudget.setText("$0.00");
                                tvSpent.setText("$0.00");
                                tvRemaining.setText("$0.00");
                            });
                })
                .addOnFailureListener(e -> {
                    tvBudget.setText("$0.00");
                    tvSpent.setText("$0.00");
                    tvRemaining.setText("$0.00");
                });
    }

    // -------------------------------------------------------------
    // ⭐ Edit Monthly Budget (CRASH SAFE + AUTO-SAVINGS UPDATE)
    // -------------------------------------------------------------
    private void showEditDialog() {

        android.app.AlertDialog.Builder dialog = new android.app.AlertDialog.Builder(getContext());
        View popup = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_budget, null);
        dialog.setView(popup);

        EditText edtAmount = popup.findViewById(R.id.edtNewBudget);
        Button btnSave = popup.findViewById(R.id.btnSaveBudget);

        android.app.AlertDialog alert = dialog.create();
        alert.show();

        btnSave.setOnClickListener(v -> {

            String input = edtAmount.getText().toString().trim();

            if (input.isEmpty()) {
                Toast.makeText(getContext(), "Please enter a budget amount", Toast.LENGTH_SHORT).show();
                return;
            }

            double newBudget;
            try {
                newBudget = Double.parseDouble(input);
            } catch (Exception e) {
                Toast.makeText(getContext(), "Invalid number", Toast.LENGTH_SHORT).show();
                return;
            }

            if (auth.getCurrentUser() == null) return;
            String uid = auth.getCurrentUser().getUid();

            Map<String, Object> map = new HashMap<>();
            map.put("monthlyBudget", newBudget);

            db.collection("budget").document(uid)
                    .set(map)
                    .addOnSuccessListener(ok -> {
                        Toast.makeText(getContext(), "Budget updated", Toast.LENGTH_SHORT).show();

                        loadBudgetUI();                      // refresh UI
                        updateSavings(uid);                  // ⭐ automatically recalc savings

                        alert.dismiss();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(),
                                "Failed to update budget: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        });
    }

    // -------------------------------------------------------------
    // ⭐ UPDATE SAVINGS after budget change
    // -------------------------------------------------------------
    private void updateSavings(String uid) {
        // Recalculate savings using Dashboard logic
        new DashboardFragment().calculateSavings(uid);
    }
}
