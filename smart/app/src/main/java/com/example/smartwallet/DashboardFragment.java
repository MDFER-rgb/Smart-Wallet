package com.example.smartwallet;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.EditText;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;


import java.util.Locale;

public class DashboardFragment extends Fragment {

    TextView tvGreeting, tvTotalBalance, tvCirclePercent, tvMonthlyBudget, tvTotalSpentValue;
    TextView tvSub1, tvSub2, btnViewAllSubs, tvSavings;
    ProgressBar circularProgress, progressSavings;

    double maxBalance = 1;

    FirebaseFirestore db;
    FirebaseAuth auth;

    public DashboardFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // UI refs
        tvGreeting = view.findViewById(R.id.tvGreeting);
        tvTotalBalance = view.findViewById(R.id.tvTotalBalance);
        tvCirclePercent = view.findViewById(R.id.tvCirclePercent);
        tvMonthlyBudget = view.findViewById(R.id.tvMonthlyBudget);
        tvTotalSpentValue = view.findViewById(R.id.tvTotalSpentValue);

        tvSub1 = view.findViewById(R.id.tvSub1);
        tvSub2 = view.findViewById(R.id.tvSub2);
        btnViewAllSubs = view.findViewById(R.id.btnViewAllSubs);

        tvSavings = view.findViewById(R.id.tvSavings);
        progressSavings = view.findViewById(R.id.progressSavings);
        circularProgress = view.findViewById(R.id.circularProgress);

        ImageView btnUser = view.findViewById(R.id.btnUserProfile);
        Button btnLogout = view.findViewById(R.id.btnLogout);
        Button btnDeleteAccount = view.findViewById(R.id.btnDeleteAccount);

        btnUser.setOnClickListener(v -> {
            if (getActivity() == null) return;
            getActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new LoginFragment())
                    .addToBackStack(null)
                    .commit();
        });

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            // Not logged in - go to login screen
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new LoginFragment())
                        .commitAllowingStateLoss();
            }
            return;
        }

        String uid = user.getUid();
        loadAllData(uid);

        btnLogout.setOnClickListener(v -> {
            auth.signOut();
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new LoginFragment())
                        .commitAllowingStateLoss();
            }
        });

        btnDeleteAccount.setOnClickListener(v -> showDeleteConfirm());

        view.findViewById(R.id.cardMonthlyBudget).setOnClickListener(v -> {
            if (getActivity() == null) return;
            getActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new BudgetManagerFragment())
                    .addToBackStack(null)
                    .commit();
        });

        view.findViewById(R.id.cardAddExpense).setOnClickListener(v -> {
            if (getActivity() == null) return;
            getActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new ExpensesFragment())
                    .addToBackStack(null)
                    .commit();
        });

        view.findViewById(R.id.cardSubscriptions).setOnClickListener(v -> {
            if (getActivity() == null) return;
            getActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new SubscriptionsFragment())
                    .addToBackStack(null)
                    .commit();
        });

        btnViewAllSubs.setOnClickListener(v -> {
            if (getActivity() == null) return;
            getActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new SubscriptionsFragment())
                    .addToBackStack(null)
                    .commit();
        });
    }

    // ---------------- LOAD ALL ----------------
    private void loadAllData(String uid) {
        loadUserName(uid);
        loadWallet(uid);
        loadBudget(uid);
        loadTotalSpent(uid);
        loadSubscriptions(uid);
        calculateSavings(uid);
    }

    private void loadUserName(String uid) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(d -> {
                    String f = d.getString("firstName");
                    String l = d.getString("lastName");

                    if (f == null) f = "";
                    if (l == null) l = "";

                    if (f.isEmpty() && l.isEmpty()) {
                        tvGreeting.setText("Good afternoon!");
                    } else {
                        tvGreeting.setText("Good afternoon, " + f + " " + l);
                    }
                })
                .addOnFailureListener(e -> tvGreeting.setText("Good afternoon!"));
    }

    private void loadWallet(String uid) {
        db.collection("wallet").document(uid).get()
                .addOnSuccessListener(doc -> {
                    Double bal = doc.getDouble("totalBalance");
                    Double max = doc.getDouble("maxBalance");

                    if (bal == null) bal = 0.0;
                    if (max == null) max = Math.max(1.0, bal);

                    maxBalance = max;
                    updateBalance(bal);
                })
                .addOnFailureListener(e -> updateBalance(0));
    }

    private void loadBudget(String uid) {
        db.collection("budget").document(uid).get()
                .addOnSuccessListener(d -> {
                    Double monthly = d.getDouble("monthlyBudget");
                    if (monthly == null) monthly = 0.0;
                    tvMonthlyBudget.setText("$" + String.format(Locale.getDefault(), "%.2f", monthly));
                })
                .addOnFailureListener(e -> tvMonthlyBudget.setText("$0.00"));
    }

    private void loadTotalSpent(String uid) {
        db.collection("expenses")
                .document(uid)
                .collection("categories")
                .get()
                .addOnSuccessListener(q -> {
                    double totalSpent = 0;
                    for (QueryDocumentSnapshot d : q) {
                        Double spent = d.getDouble("spent");
                        if (spent != null) totalSpent += spent;
                    }
                    tvTotalSpentValue.setText("$" + String.format(Locale.getDefault(), "%.2f", totalSpent));
                })
                .addOnFailureListener(e -> tvTotalSpentValue.setText("$0.00"));
    }

    private void loadSubscriptions(String uid) {
        db.collection("subscriptions")
                .document(uid)
                .collection("items")
                .limit(2)
                .get()
                .addOnSuccessListener(q -> {
                    int i = 0;
                    for (QueryDocumentSnapshot d : q) {
                        if (d.getId().equals("init")) continue;
                        String provider = d.getString("provider");
                        Double amount = d.getDouble("amount");
                        String currency = d.getString("currency");

                        if (provider == null) provider = "—";
                        if (currency == null) currency = "LKR";
                        if (amount == null) amount = 0.0;

                        String text = provider + " • " + currency + " " +
                                String.format(Locale.getDefault(), "%.2f", amount);

                        if (i == 0) tvSub1.setText(text);
                        else if (i == 1) tvSub2.setText(text);

                        i++;
                    }
                    if (i == 0) {
                        tvSub1.setText("No subscriptions");
                        tvSub2.setText("");
                    }
                })
                .addOnFailureListener(e -> {
                    tvSub1.setText("No subscriptions");
                    tvSub2.setText("");
                });
    }

    // ---------------- UI updates ----------------
    public void updateBalance(double bal) {
        if (tvTotalBalance != null)
            tvTotalBalance.setText("$" + String.format(Locale.getDefault(), "%.2f", bal));

        int percent = 0;
        if (maxBalance > 0) {
            percent = (int) ((bal / maxBalance) * 100);
        }
        if (percent > 100) percent = 100;
        if (circularProgress != null) circularProgress.setProgress(percent);
        if (tvCirclePercent != null) tvCirclePercent.setText(percent + "%");
    }

    // ---------------- Savings ----------------
    public void calculateSavings(String uid) {
        db.collection("wallet").document(uid).get()
                .addOnSuccessListener(walletDoc -> {
                    Double balanceValue = walletDoc.getDouble("totalBalance");
                    final double totalBalance = balanceValue != null ? balanceValue : 0.0;

                    db.collection("budget").document(uid).get()
                            .addOnSuccessListener(budgetDoc -> {
                                Double mb = budgetDoc.getDouble("monthlyBudget");
                                final double monthlyBudget = mb != null ? mb : 0.0;

                                double savings = totalBalance - monthlyBudget;
                                if (savings < 0) savings = 0;

                                if (tvSavings != null) {
                                    tvSavings.setText("$" + String.format(Locale.getDefault(), "%.2f", savings)
                                            + " / $" + String.format(Locale.getDefault(), "%.2f", totalBalance));
                                }

                                int percent = 0;
                                if (totalBalance > 0) percent = (int) ((savings / totalBalance) * 100);
                                if (percent < 0) percent = 0;
                                if (percent > 100) percent = 100;
                                if (progressSavings != null) progressSavings.setProgress(percent);

                                // Save savings doc (simple map)
                                db.collection("savings").document(uid)
                                        .set(new SavingsModel(savings, percent))
                                        .addOnFailureListener(e ->
                                                Toast.makeText(requireContext(), "Failed to update savings", Toast.LENGTH_SHORT).show());
                            })
                            .addOnFailureListener(e -> {
                                // budget read failed -> still show something
                                if (tvSavings != null)
                                    tvSavings.setText("$0.00 / $" + String.format(Locale.getDefault(), "%.2f", totalBalance));
                            });
                })
                .addOnFailureListener(e -> {
                    if (tvSavings != null) tvSavings.setText("$0.00 / $0.00");
                });
    }

    // ---------------- Delete account ----------------
    private void showDeleteConfirm() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to permanently delete your account? This cannot be undone.")
                .setPositiveButton("Delete", (d, w) -> deleteAccount())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteAccount() {

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        // Ask password first
        showPasswordPopup(password -> {

            AuthCredential credential =
                    EmailAuthProvider.getCredential(user.getEmail(), password);

            // Re-authenticate first
            user.reauthenticate(credential)
                    .addOnSuccessListener(a -> {

                        String uid = user.getUid();

                        // Delete Firestore data
                        db.collection("users").document(uid).delete();
                        db.collection("wallet").document(uid).delete();
                        db.collection("budget").document(uid).delete();

                        db.collection("expenses").document(uid)
                                .collection("categories")
                                .get()
                                .addOnSuccessListener(q -> {
                                    for (var d : q) d.getReference().delete();
                                });

                        db.collection("subscriptions").document(uid)
                                .collection("items")
                                .get()
                                .addOnSuccessListener(q -> {
                                    for (var d : q) d.getReference().delete();
                                });

                        // Delete Auth user
                        user.delete()
                                .addOnSuccessListener(done -> {

                                    Toast.makeText(getContext(),
                                            "Account deleted", Toast.LENGTH_SHORT).show();

                                    auth.signOut();
                                    requireActivity().getSupportFragmentManager()
                                            .beginTransaction()
                                            .replace(R.id.fragment_container, new LoginFragment())
                                            .commit();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(getContext(),
                                            "Delete failed: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(),
                                "Password incorrect", Toast.LENGTH_LONG).show();
                    });
        });
    }

    // ============================
// iOS STYLE PASSWORD POPUP
// ============================
    private interface PasswordCallback {
        void onPasswordEntered(String password);
    }

    private void showPasswordPopup(PasswordCallback callback) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View v = LayoutInflater.from(getContext()).inflate(R.layout.dialog_password, null);
        builder.setView(v);

        EditText edtPass = v.findViewById(R.id.edtPassword);
        TextView btnCancel = v.findViewById(R.id.btnCancelPass);
        TextView btnOk = v.findViewById(R.id.btnOkPass);

        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        btnCancel.setOnClickListener(x -> dialog.dismiss());

        btnOk.setOnClickListener(x -> {
            String pass = edtPass.getText().toString().trim();
            if (!pass.isEmpty()) {
                dialog.dismiss();
                callback.onPasswordEntered(pass);  // return password safely
            }
        });

        dialog.show();
    }



}
