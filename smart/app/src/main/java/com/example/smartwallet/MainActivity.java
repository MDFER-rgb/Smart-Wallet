package com.example.smartwallet;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

// Firebase
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import androidx.preference.PreferenceManager;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements EditBalanceDialog.OnBalanceUpdatedListener {

    BottomNavigationView bottomNav;
    FirebaseAuth auth;
    private static final int REQ_NOTIF = 101; // Android 13+

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottomNav);
        auth = FirebaseAuth.getInstance();

        // 🔥 Ask for Android 13+ Notification Permission
        requestNotificationPermission();

        FirebaseUser user = auth.getCurrentUser();

        // ----------------------------
        // Default Start Screen
        // ----------------------------
        if (savedInstanceState == null) {
            if (user != null) {
                // Save uid for Worker (alerts)
                PreferenceManager.getDefaultSharedPreferences(this)
                        .edit()
                        .putString("last_signed_in_uid", user.getUid())
                        .apply();

                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new DashboardFragment())
                        .commit();
            } else {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new LoginFragment())
                        .commit();
            }
        }

        // ----------------------------
        // Bottom Navigation Handler
        // ----------------------------
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selected = null;

            if (item.getItemId() == R.id.navigation_dashboard)
                selected = new DashboardFragment();

            else if (item.getItemId() == R.id.navigation_expenses)
                selected = new ExpensesFragment();

            else if (item.getItemId() == R.id.navigation_crypto)   // 🔥 Crypto Screen
                selected = new CryptoChartFragment();

            else if (item.getItemId() == R.id.navigation_subscriptions)
                selected = new SubscriptionsFragment();

            else if (item.getItemId() == R.id.navigation_alerts)   // 🔥 Alerts Screen
                selected = new AlertsFragment();

            if (selected != null) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, selected)
                        .commit();
            }

            return true;
        });

        testFirestoreConnection();
    }

    // -----------------------------------------------------------
    // 🔥 Android 13+ Notification Permission
    // -----------------------------------------------------------
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQ_NOTIF
                );
            }
        }
    }

    // -----------------------------------------------------------
    // Firebase Test
    // -----------------------------------------------------------
    private void testFirestoreConnection() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> testData = new HashMap<>();
        testData.put("message", "Hello Firebase from SmartWallet!");
        testData.put("status", "working");

        db.collection("testCollection")
                .add(testData)
                .addOnSuccessListener(ref ->
                        Log.d("FIREBASE_TEST", "Success! Doc ID: " + ref.getId()))
                .addOnFailureListener(e ->
                        Log.e("FIREBASE_TEST", "Error writing document", e));
    }

    // -----------------------------------------------------------
    // Update Dashboard Balance
    // -----------------------------------------------------------
    @Override
    public void onBalanceUpdated(double newBalance) {
        Fragment current = getSupportFragmentManager()
                .findFragmentById(R.id.fragment_container);

        if (current instanceof DashboardFragment) {
            ((DashboardFragment) current).updateBalance(newBalance);
        }
    }

    // -----------------------------------------------------------
    // Permission Result Callback
    // -----------------------------------------------------------
    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQ_NOTIF) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("NOTIF", "Notification Permission GRANTED");
            } else {
                Log.w("NOTIF", "Notification Permission DENIED");
            }
        }
    }
}
