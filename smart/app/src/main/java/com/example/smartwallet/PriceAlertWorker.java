package com.example.smartwallet;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Response;

public class PriceAlertWorker extends Worker {

    private FirebaseFirestore db;
    private Context ctx;

    public PriceAlertWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        db = FirebaseFirestore.getInstance();
        ctx = context;
    }

    @NonNull
    @Override
    public Result doWork() {

        try {

            // -------------------------------
            // 1️⃣ Get signed-in UID
            // -------------------------------
            String uid = PreferenceManager
                    .getDefaultSharedPreferences(ctx)
                    .getString("last_signed_in_uid", null);

            if (uid == null) {
                if (FirebaseAuth.getInstance().getCurrentUser() != null)
                    uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            }

            if (uid == null) {
                return Result.success(); // no user
            }

            // -------------------------------
            // 2️⃣ Load all active alerts
            // -------------------------------
            var task = db.collection("price_alerts")
                    .document(uid)
                    .collection("items")
                    .whereEqualTo("active", true)
                    .whereEqualTo("triggered", false)
                    .get()
                    .getResult();

            List<PriceAlert> alerts = new ArrayList<>();
            List<String> coinIds = new ArrayList<>();

            for (QueryDocumentSnapshot d : task) {
                PriceAlert p = d.toObject(PriceAlert.class);
                p.id = d.getId();
                alerts.add(p);

                if (!coinIds.contains(p.coinId))
                    coinIds.add(p.coinId);
            }

            if (alerts.isEmpty()) return Result.success();

            // -------------------------------
            // 3️⃣ Fetch prices for all coins
            // -------------------------------
            String ids = String.join(",", coinIds);

            CoinGeckoService service = RetrofitClient.getClient()
                    .create(CoinGeckoService.class);

            Call<Map<String, Map<String, Double>>> call =
                    service.getSimplePrice(ids, "usd");

            Response<Map<String, Map<String, Double>>> resp = call.execute();

            if (!resp.isSuccessful() || resp.body() == null) {
                Log.e("PriceAlertWorker", "Price API failed");
                return Result.retry();
            }

            Map<String, Map<String, Double>> prices = resp.body();

            // -------------------------------
            // 4️⃣ Check alert conditions
            // -------------------------------
            int notifId = 1000;

            for (PriceAlert a : alerts) {

                Map<String, Double> coin = prices.get(a.coinId);
                if (coin == null) continue;

                Double price = coin.get("usd");
                if (price == null) continue;

                boolean hit = false;

                if ("above".equals(a.operator) && price > a.target) hit = true;
                if ("below".equals(a.operator) && price < a.target) hit = true;

                if (hit) {

                    // Create notification
                    NotificationHelper.createNotificationChannel(ctx);

                    String title = a.coinId.toUpperCase() + " price alert!";
                    String body = a.coinId.toUpperCase() + " is now $" + price;

                    NotificationHelper.sendNotification(
                            ctx, notifId++, title, body
                    );

                    // Mark triggered
                    db.collection("price_alerts")
                            .document(uid)
                            .collection("items")
                            .document(a.id)
                            .update(
                                    "triggered", true,
                                    "triggeredAt", Timestamp.now()
                            );
                }
            }

            return Result.success();

        } catch (Exception e) {
            Log.e("PriceAlertWorker", "ERROR", e);
            return Result.failure();
        }
    }
}
