package com.example.smartwallet;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class EditBalanceDialog extends BottomSheetDialogFragment {

    private OnBalanceUpdatedListener listener;

    public interface OnBalanceUpdatedListener {
        void onBalanceUpdated(double newBalance);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        listener = (OnBalanceUpdatedListener) context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.dialog_edit_balance, container, false);

        TextView tvCurrentBalance = view.findViewById(R.id.tvCurrentBalance);
        EditText etAmount = view.findViewById(R.id.etAmount);
        Button btnAdd = view.findViewById(R.id.btnAddMoney);
        Button btnRemove = view.findViewById(R.id.btnRemoveMoney);
        Button btnReset = view.findViewById(R.id.btnResetBalance);

        SharedPreferences prefs = requireActivity().getSharedPreferences("wallet", 0);
        double balance = Double.parseDouble(prefs.getString("totalBalance", "0"));

        tvCurrentBalance.setText("Current: $" + balance);

        btnAdd.setOnClickListener(v -> {
            String amt = etAmount.getText().toString();
            if (!amt.isEmpty()) {
                double newBalance = balance + Double.parseDouble(amt);
                prefs.edit().putString("totalBalance", String.valueOf(newBalance)).apply();
                listener.onBalanceUpdated(newBalance);
                dismiss();
            }
        });

        btnRemove.setOnClickListener(v -> {
            String amt = etAmount.getText().toString();
            if (!amt.isEmpty()) {
                double newBalance = balance - Double.parseDouble(amt);
                if (newBalance < 0) newBalance = 0;
                prefs.edit().putString("totalBalance", String.valueOf(newBalance)).apply();
                listener.onBalanceUpdated(newBalance);
                dismiss();
            }
        });

        btnReset.setOnClickListener(v -> {
            prefs.edit().putString("totalBalance", "0").apply();
            listener.onBalanceUpdated(0);
            dismiss();
        });

        return view;
    }
}
