package com.example.smartwallet;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.Locale;

public class AlertsFragment extends Fragment {

    private EditText edtTarget;
    private Spinner spnCoin;
    private Spinner spnOperator;
    private Button btnAdd;
    private LinearLayout layoutList;

    FirebaseAuth auth;
    FirebaseFirestore db;

    public AlertsFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_alerts, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        edtTarget = view.findViewById(R.id.edtTarget);
        spnCoin = view.findViewById(R.id.spnCoin);
        spnOperator = view.findViewById(R.id.spnOperator);
        btnAdd = view.findViewById(R.id.btnAddAlert);
        layoutList = view.findViewById(R.id.layoutAlertList);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();


        // ----------------------------------------------------
        // iOS-style WHITE TEXT SPINNER INITIALIZATION
        // ----------------------------------------------------
        ArrayAdapter<String> coinAdapter = new ArrayAdapter<>(
                requireContext(),
                R.layout.spinner_item_white,               // custom layout
                new String[]{"bitcoin", "ethereum", "solana", "dogecoin", "cardano"}
        );
        coinAdapter.setDropDownViewResource(R.layout.spinner_dropdown_white);
        spnCoin.setAdapter(coinAdapter);


        ArrayAdapter<String> opAdapter = new ArrayAdapter<>(
                requireContext(),
                R.layout.spinner_item_white,
                new String[]{"above", "below"}
        );
        opAdapter.setDropDownViewResource(R.layout.spinner_dropdown_white);
        spnOperator.setAdapter(opAdapter);


        // ----------------------------------------------------
        // Button add action
        // ----------------------------------------------------
        btnAdd.setOnClickListener(v -> addAlert());

        loadAlerts();
    }


    // ------------------------------------------------------------
    // ADD PRICE ALERT
    // ------------------------------------------------------------
    private void addAlert() {

        String coin = spnCoin.getSelectedItem().toString();
        String op = spnOperator.getSelectedItem().toString();
        String targetStr = edtTarget.getText().toString().trim();

        if (TextUtils.isEmpty(targetStr)) {
            Toast.makeText(requireContext(), "Enter target price", Toast.LENGTH_SHORT).show();
            return;
        }

        double target = Double.parseDouble(targetStr);

        String uid = auth.getCurrentUser().getUid();

        PriceAlert alert = new PriceAlert(coin, op, target);

        db.collection("price_alerts")
                .document(uid)
                .collection("items")
                .add(alert.toMap())
                .addOnSuccessListener(doc -> {
                    Toast.makeText(requireContext(), "Alert saved", Toast.LENGTH_SHORT).show();
                    edtTarget.setText("");
                    loadAlerts();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }


    // ------------------------------------------------------------
    // LOAD ALL ALERTS
    // ------------------------------------------------------------
    private void loadAlerts() {

        layoutList.removeAllViews();

        String uid = auth.getCurrentUser().getUid();

        db.collection("price_alerts")
                .document(uid)
                .collection("items")
                .get()
                .addOnSuccessListener(q -> {

                    for (QueryDocumentSnapshot d : q) {

                        View row = getLayoutInflater().inflate(R.layout.item_alert_row, null);

                        TextView tv = row.findViewById(R.id.tvAlertText);
                        ToggleButton toggle = row.findViewById(R.id.toggleActive);
                        Button btnDelete = row.findViewById(R.id.btnDeleteAlert);

                        String coin = d.getString("coinId");
                        String op = d.getString("operator");
                        Double target = d.getDouble("target");
                        Boolean active = d.getBoolean("active");
                        Boolean triggered = d.getBoolean("triggered");

                        if (coin == null) coin = "unknown";

                        String label = coin.toUpperCase(Locale.getDefault()) + " " +
                                op + " $" + String.format(Locale.getDefault(), "%.2f", target);

                        if (triggered != null && triggered)
                            label += " (Triggered)";

                        tv.setText(label);
                        toggle.setChecked(active != null && active);

                        // UPDATE ACTIVE STATE
                        toggle.setOnCheckedChangeListener((btn, checked) ->
                                db.collection("price_alerts")
                                        .document(uid)
                                        .collection("items")
                                        .document(d.getId())
                                        .update("active", checked)
                        );

                        // DELETE ALERT
                        btnDelete.setOnClickListener(v ->
                                db.collection("price_alerts")
                                        .document(uid)
                                        .collection("items")
                                        .document(d.getId())
                                        .delete()
                                        .addOnSuccessListener(x -> loadAlerts())
                        );

                        // ⭐ ADD REAL IOS-STYLE SPACING BETWEEN ROWS ⭐
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                        );
                        params.setMargins(0, 0, 0, 26);  // SPACE
                        row.setLayoutParams(params);

                        layoutList.addView(row);
                    }
                });
    }

}
