package com.example.smartwallet;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ExpensesFragment extends Fragment {

    private LinearLayout layoutAllCategories;
    private EditText edtTitle, edtAmount;
    private Button btnAdd;

    FirebaseFirestore db;
    FirebaseAuth auth;

    public ExpensesFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_expenses, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {

        layoutAllCategories = view.findViewById(R.id.layoutAllCategories);
        edtTitle = view.findViewById(R.id.edtTitle);
        edtAmount = view.findViewById(R.id.edtAmount);
        btnAdd = view.findViewById(R.id.btnAdd);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        btnAdd.setOnClickListener(v -> saveNewCategory());

        loadCategoriesFromFirestore();

        addBackSwipeGesture(view.findViewById(R.id.expensesRoot));
    }

    // --------------------------------------------------
    // 🔥 Save NEW Category
    // --------------------------------------------------
    private void saveNewCategory() {
        String name = edtTitle.getText().toString().trim();
        String limitStr = edtAmount.getText().toString().trim();

        if (name.isEmpty() || limitStr.isEmpty()) return;

        double limit = Double.parseDouble(limitStr);
        String uid = auth.getCurrentUser().getUid();

        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("limit", limit);
        data.put("spent", 0.0);

        db.collection("expenses")
                .document(uid)
                .collection("categories")
                .add(data)
                .addOnSuccessListener(ref -> {
                    addCategoryCard(ref.getId(), name, limit, 0.0);
                    edtTitle.setText("");
                    edtAmount.setText("");
                    updateTotalSpent(uid);
                });
    }

    // --------------------------------------------------
    // 🔥 Load Categories from Firestore
    // --------------------------------------------------
    private void loadCategoriesFromFirestore() {
        String uid = auth.getCurrentUser().getUid();

        db.collection("expenses")
                .document(uid)
                .collection("categories")
                .get()
                .addOnSuccessListener(q -> {
                    layoutAllCategories.removeAllViews();

                    for (var d : q) {
                        String id = d.getId();
                        String name = d.getString("name");
                        double limit = d.getDouble("limit");
                        double spent = d.getDouble("spent");

                        addCategoryCard(id, name, limit, spent);
                    }
                });
    }

    // --------------------------------------------------
    // 🔥 Add Category Card UI
    // --------------------------------------------------
    private void addCategoryCard(String id, String name, double limit, double spent) {

        View cardView = getLayoutInflater().inflate(R.layout.item_category_card, null);

        CardView card = cardView.findViewById(R.id.cardCategory);
        TextView tvName = cardView.findViewById(R.id.tvCategoryName);
        TextView tvLimit = cardView.findViewById(R.id.tvCategoryLimit);
        LinearLayout expandArea = cardView.findViewById(R.id.expandArea);
        TextView tvSpent = cardView.findViewById(R.id.tvExtraSpent);
        TextView tvRemaining = cardView.findViewById(R.id.tvExtraRemaining);
        TextView btnEditLimit = cardView.findViewById(R.id.btnEdit);
        TextView btnEditSpent = cardView.findViewById(R.id.btnEditSpent);
        TextView btnDelete = cardView.findViewById(R.id.btnDelete);

        tvName.setText(name);
        tvLimit.setText("Limit: $" + limit);
        tvSpent.setText("Spent: $" + spent);
        tvRemaining.setText("Remaining: $" + (limit - spent));

        // Expand / collapse
        card.setOnClickListener(v -> {
            if (expandArea.getVisibility() == View.GONE)
                expandArea.setVisibility(View.VISIBLE);
            else
                expandArea.setVisibility(View.GONE);
        });

        // ---------------------- EDIT LIMIT ----------------------
        btnEditLimit.setOnClickListener(v ->
                showLimitDialog(id, tvLimit, tvRemaining, limit, spent)
        );

        // ---------------------- EDIT SPENT ----------------------
        btnEditSpent.setOnClickListener(v ->
                showSpentDialog(id, tvSpent, tvRemaining, limit)
        );

        // ---------------------- DELETE CATEGORY ----------------------
        btnDelete.setOnClickListener(v -> {
            AlertDialog.Builder b = new AlertDialog.Builder(getContext());
            b.setTitle("Delete Category?");
            b.setMessage(name);
            b.setPositiveButton("Delete", (d, i) -> {
                deleteCategory(id);
                layoutAllCategories.removeView(cardView);
                updateTotalSpent(auth.getCurrentUser().getUid());
            });
            b.setNegativeButton("Cancel", null);
            b.show();
        });

        layoutAllCategories.addView(cardView);
    }

    // --------------------------------------------------
    // 🔥 EDIT LIMIT
    // --------------------------------------------------
    private void showLimitDialog(String id, TextView tvLimit, TextView tvRemaining,
                                 double oldLimit, double spentValue) {

        AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());
        View popup = LayoutInflater.from(getContext()).inflate(R.layout.ios_limit_dialog, null);
        dialog.setView(popup);

        EditText edtNew = popup.findViewById(R.id.edtNewLimit);
        TextView btnCancel = popup.findViewById(R.id.btnCancel);
        TextView btnSave = popup.findViewById(R.id.btnSave);

        edtNew.setHint("New Limit");

        AlertDialog alert = dialog.create();
        alert.show();

        btnCancel.setOnClickListener(v -> alert.dismiss());

        btnSave.setOnClickListener(v -> {
            String newLimitStr = edtNew.getText().toString().trim();
            if (newLimitStr.isEmpty()) return;

            double newLimit = Double.parseDouble(newLimitStr);
            String uid = auth.getCurrentUser().getUid();

            db.collection("expenses")
                    .document(uid)
                    .collection("categories")
                    .document(id)
                    .update("limit", newLimit)
                    .addOnSuccessListener(x -> {
                        tvLimit.setText("Limit: $" + newLimit);
                        tvRemaining.setText("Remaining: $" + (newLimit - spentValue));
                        alert.dismiss();
                    });
        });
    }

    // --------------------------------------------------
    // 🔥 EDIT SPENT (updates Dashboard & BudgetManager)
    // --------------------------------------------------
    private void showSpentDialog(String id, TextView tvSpent,
                                 TextView tvRemaining, double limitValue) {

        AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());
        View popup = LayoutInflater.from(getContext()).inflate(R.layout.ios_limit_dialog, null);
        dialog.setView(popup);

        EditText edtNew = popup.findViewById(R.id.edtNewLimit);
        TextView btnCancel = popup.findViewById(R.id.btnCancel);
        TextView btnSave = popup.findViewById(R.id.btnSave);

        edtNew.setHint("New Spent");

        AlertDialog alert = dialog.create();
        alert.show();

        btnCancel.setOnClickListener(v -> alert.dismiss());

        btnSave.setOnClickListener(v -> {

            String newSpentStr = edtNew.getText().toString().trim();
            if (newSpentStr.isEmpty()) return;

            double newSpent = Double.parseDouble(newSpentStr);
            String uid = auth.getCurrentUser().getUid();

            db.collection("expenses")
                    .document(uid)
                    .collection("categories")
                    .document(id)
                    .update("spent", newSpent)
                    .addOnSuccessListener(x -> {
                        tvSpent.setText("Spent: $" + newSpent);
                        tvRemaining.setText("Remaining: $" + (limitValue - newSpent));

                        updateTotalSpent(uid);  // 🔥 update dashboard + budget
                        alert.dismiss();
                    });
        });
    }

    // --------------------------------------------------
    // 🔥 Delete Category
    // --------------------------------------------------
    private void deleteCategory(String id) {
        String uid = auth.getCurrentUser().getUid();

        db.collection("expenses")
                .document(uid)
                .collection("categories")
                .document(id)
                .delete();
    }

    // --------------------------------------------------
    // 🔥 Compute total spent and update Firestore
    // DashboardFragment + BudgetManagerFragment uses this value
    // --------------------------------------------------
    private void updateTotalSpent(String uid) {

        db.collection("expenses")
                .document(uid)
                .collection("categories")
                .get()
                .addOnSuccessListener(q -> {

                    double totalSpent = 0;
                    for (var d : q) {
                        Double s = d.getDouble("spent");
                        if (s != null) totalSpent += s;
                    }

                    // Save total spent properly
                    Map<String, Object> map = new HashMap<>();
                    map.put("totalSpent", totalSpent);

                    db.collection("expenses")
                            .document(uid)
                            .set(map, com.google.firebase.firestore.SetOptions.merge());
                });
    }


    // --------------------------------------------------
    // iOS Swipe Back
    // --------------------------------------------------
    private void addBackSwipeGesture(View root) {

        root.setOnTouchListener(new View.OnTouchListener() {
            float downX = 0;
            final float SWIPE_DISTANCE = 160f;

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                switch (event.getAction()) {

                    case MotionEvent.ACTION_DOWN:
                        downX = event.getX();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float diff = event.getX() - downX;
                        if (diff > 0) root.setTranslationX(diff);
                        return true;

                    case MotionEvent.ACTION_UP:
                        if (event.getX() - downX > SWIPE_DISTANCE) {
                            requireActivity().onBackPressed();
                        } else {
                            root.animate().translationX(0).setDuration(150).start();
                        }
                        return true;
                }

                return false;
            }
        });
    }
}
