package com.example.smartwallet;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartwallet.models.Subscription;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

public class SubscriptionsFragment extends Fragment implements SubscriptionAdapter.Listener {

    private RecyclerView rv;
    private TextView tvEmpty;
    private SubscriptionAdapter adapter;
    private final List<Subscription> list = new ArrayList<>();

    FirebaseFirestore db;
    FirebaseAuth auth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_subscriptions, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        rv = view.findViewById(R.id.rvSubscriptions);
        tvEmpty = view.findViewById(R.id.tvEmpty);

        view.findViewById(R.id.btnAddSubscription)
                .setOnClickListener(v -> showAddDialog());

        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        loadSubscriptions();
    }

    // ---------------------------------------------------------
    // LOAD DATA
    // ---------------------------------------------------------
    private void loadSubscriptions() {

        list.clear();
        String uid = auth.getCurrentUser().getUid();

        db.collection("subscriptions")
                .document(uid)
                .collection("items")
                .get()
                .addOnSuccessListener(q -> {

                    for (var doc : q.getDocuments()) {
                        if (doc.getId().equals("init")) continue;
                        Subscription s = Subscription.fromMap(doc.getData());
                        list.add(s);
                    }

                    tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);

                    adapter = new SubscriptionAdapter(list, this);
                    rv.setAdapter(adapter);
                });
    }

    // ---------------------------------------------------------
    // SAVE
    // ---------------------------------------------------------
    private void saveSubscription(Subscription s) {
        String uid = auth.getCurrentUser().getUid();
        db.collection("subscriptions")
                .document(uid)
                .collection("items")
                .document(s.id)
                .set(s.toMap());
    }

    // ---------------------------------------------------------
    // DELETE
    // ---------------------------------------------------------
    private void deleteSubscription(Subscription s) {
        String uid = auth.getCurrentUser().getUid();
        db.collection("subscriptions")
                .document(uid)
                .collection("items")
                .document(s.id)
                .delete();
    }

    // ---------------------------------------------------------
    // ADD SUBSCRIPTION
    // ---------------------------------------------------------
    private void showAddDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View v = LayoutInflater.from(getContext()).inflate(R.layout.dialog_subscription, null);
        builder.setView(v);

        EditText edtProvider = v.findViewById(R.id.edtProvider);
        EditText edtPlan = v.findViewById(R.id.edtPlan);
        EditText edtAmount = v.findViewById(R.id.edtAmount);
        EditText edtCurrency = v.findViewById(R.id.edtCurrency);
        EditText edtRenewal = v.findViewById(R.id.edtRenewal);
        Spinner spinnerFrequency = v.findViewById(R.id.spinnerFrequency);
        EditText edtNotes = v.findViewById(R.id.edtNotes);

        TextView btnCancel = v.findViewById(R.id.btnCancelDialog);
        TextView btnSave = v.findViewById(R.id.btnSaveDialog);

        String[] freqList = {"Monthly", "Weekly", "Yearly"};
        spinnerFrequency.setAdapter(new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_spinner_item,
                freqList
        ));

        edtRenewal.setOnClickListener(v2 -> pickDate(edtRenewal));

        AlertDialog dialog = builder.create();
        dialog.show();

        btnCancel.setOnClickListener(v2 -> dialog.dismiss());

        btnSave.setOnClickListener(v2 -> {

            if (edtProvider.getText().toString().trim().isEmpty()) return;
            if (edtAmount.getText().toString().trim().isEmpty()) return;

            Subscription s = new Subscription(
                    UUID.randomUUID().toString(),
                    edtProvider.getText().toString(),
                    edtPlan.getText().toString(),
                    Double.parseDouble(edtAmount.getText().toString()),
                    edtCurrency.getText().toString(),
                    edtRenewal.getText().toString(),
                    spinnerFrequency.getSelectedItem().toString(),
                    true,
                    edtNotes.getText().toString()
            );

            saveSubscription(s);
            loadSubscriptions();
            dialog.dismiss();
        });
    }

    // ---------------------------------------------------------
    // EDIT SUBSCRIPTION
    // ---------------------------------------------------------
    @Override
    public void onEdit(Subscription old) {
        showEditDialog(old);
    }

    private void showEditDialog(Subscription old) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View v = LayoutInflater.from(getContext()).inflate(R.layout.dialog_subscription, null);
        builder.setView(v);

        EditText edtProvider = v.findViewById(R.id.edtProvider);
        EditText edtPlan = v.findViewById(R.id.edtPlan);
        EditText edtAmount = v.findViewById(R.id.edtAmount);
        EditText edtCurrency = v.findViewById(R.id.edtCurrency);
        EditText edtRenewal = v.findViewById(R.id.edtRenewal);
        Spinner spinnerFrequency = v.findViewById(R.id.spinnerFrequency);
        EditText edtNotes = v.findViewById(R.id.edtNotes);

        TextView btnCancel = v.findViewById(R.id.btnCancelDialog);
        TextView btnSave = v.findViewById(R.id.btnSaveDialog);

        edtProvider.setText(old.provider);
        edtPlan.setText(old.plan);
        edtAmount.setText(String.valueOf(old.amount));
        edtCurrency.setText(old.currency);
        edtRenewal.setText(old.renewalDate);
        edtNotes.setText(old.notes);

        String[] freqList = {"Monthly", "Weekly", "Yearly"};
        spinnerFrequency.setAdapter(new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_spinner_item,
                freqList
        ));

        spinnerFrequency.setSelection(
                old.frequency.equals("Weekly") ? 1 :
                        old.frequency.equals("Yearly") ? 2 : 0
        );

        edtRenewal.setOnClickListener(v2 -> pickDate(edtRenewal));

        AlertDialog dialog = builder.create();
        dialog.show();

        btnCancel.setOnClickListener(v2 -> dialog.dismiss());

        btnSave.setOnClickListener(v2 -> {

            old.provider = edtProvider.getText().toString();
            old.plan = edtPlan.getText().toString();
            old.amount = Double.parseDouble(edtAmount.getText().toString());
            old.currency = edtCurrency.getText().toString();
            old.renewalDate = edtRenewal.getText().toString();
            old.frequency = spinnerFrequency.getSelectedItem().toString();
            old.notes = edtNotes.getText().toString();

            // update logo
            old.logoUrl = "https://logo.clearbit.com/" +
                    old.provider.toLowerCase().replace(" ", "") + ".com";

            saveSubscription(old);
            loadSubscriptions();
            dialog.dismiss();
        });
    }

    // ---------------------------------------------------------
    // DELETE
    // ---------------------------------------------------------
    @Override
    public void onDelete(Subscription s) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View v = LayoutInflater.from(getContext()).inflate(R.layout.dialog_ios_delete, null);
        builder.setView(v);

        TextView tvMessage = v.findViewById(R.id.tvMessage);
        TextView btnCancel = v.findViewById(R.id.btnCancel);
        TextView btnDelete = v.findViewById(R.id.btnDelete);

        tvMessage.setText("Are you sure you want to delete " + s.provider + "?");

        AlertDialog dialog = builder.create();
        dialog.show();

        btnCancel.setOnClickListener(vv -> dialog.dismiss());

        btnDelete.setOnClickListener(vv -> {
            deleteSubscription(s);
            loadSubscriptions();
            dialog.dismiss();
        });
    }

    // ---------------------------------------------------------
    // TOGGLE ACTIVE
    // ---------------------------------------------------------
    @Override
    public void onToggleActive(Subscription s, boolean active) {
        s.active = active;
        saveSubscription(s);
    }

    // ---------------------------------------------------------
    // DATE PICKER
    // ---------------------------------------------------------
    private void pickDate(EditText edt) {
        Calendar c = Calendar.getInstance();
        DatePickerDialog dp = new DatePickerDialog(
                getContext(),
                (DatePicker view, int y, int m, int d) ->
                        edt.setText(y + "-" + (m + 1) + "-" + d),
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH)
        );
        dp.show();
    }
}
