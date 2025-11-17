package com.example.smartwallet;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CryptoChartFragment extends Fragment {

    private LineChart lineChart;
    private Button btn7d, btn30d, btn90d, btnSetAlert;
    private Spinner spnCoin;
    private TextView tvLivePrice;

    private final Handler handler = new Handler();
    private Runnable livePriceRunnable;
    private String currentCoin = "bitcoin";
    private int currentDays = 30;

    public CryptoChartFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_crypto_chart, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        lineChart = view.findViewById(R.id.lineChart);
        btn7d = view.findViewById(R.id.btn7d);
        btn30d = view.findViewById(R.id.btn30d);
        btn90d = view.findViewById(R.id.btn90d);
        spnCoin = view.findViewById(R.id.spnCoin);
        tvLivePrice = view.findViewById(R.id.tvLivePrice);
        btnSetAlert = view.findViewById(R.id.btnSetAlert);

        setupChart();

        // coin spinner
        ArrayAdapter<String> coinAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item,
                new String[]{"bitcoin", "ethereum", "solana", "dogecoin", "cardano"});
        coinAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnCoin.setAdapter(coinAdapter);

        spnCoin.setSelection(0);
        spnCoin.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                currentCoin = (String) parent.getItemAtPosition(position);
                loadChartData(currentCoin, currentDays);
                fetchLivePrice(currentCoin);
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        btn7d.setOnClickListener(v -> { currentDays = 7; loadChartData(currentCoin, currentDays); });
        btn30d.setOnClickListener(v -> { currentDays = 30; loadChartData(currentCoin, currentDays); });
        btn90d.setOnClickListener(v -> { currentDays = 90; loadChartData(currentCoin, currentDays); });

        btnSetAlert.setOnClickListener(v -> {

            if (!isAdded()) return;

            double curPrice;

            try {
                String txt = tvLivePrice.getText()
                        .toString()
                        .replace("$", "")
                        .replace(",", "")
                        .trim();

                if (txt.isEmpty() || txt.equals("0") || txt.equals("0.00")) {
                    Toast.makeText(requireContext(),
                            "Live price not loaded yet!",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                curPrice = Double.parseDouble(txt);

            } catch (Exception e) {
                Toast.makeText(requireContext(),
                        "Error reading price",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            Bundle args = new Bundle();
            args.putString("prefill_coin", currentCoin);
            args.putString("prefill_target",
                    String.format(Locale.getDefault(), "%.2f", curPrice));

            AlertsFragment frag = new AlertsFragment();
            frag.setArguments(args);

            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, frag)   // FIXED!
                    .addToBackStack(null)
                    .commit();
        });


        // default
        loadChartData(currentCoin, currentDays);
        fetchLivePrice(currentCoin);

        // periodic live price updater every 30s
        livePriceRunnable = () -> {
            fetchLivePrice(currentCoin);
            handler.postDelayed(livePriceRunnable, 30_000);
        };
        handler.postDelayed(livePriceRunnable, 30_000);
    }

    private void setupChart() {
        lineChart.setNoDataText("Loading chart...");
        lineChart.setTouchEnabled(true);
        lineChart.setPinchZoom(true);
        lineChart.setBackgroundColor(Color.parseColor("#0B0610"));
        lineChart.getDescription().setEnabled(false);
        lineChart.getLegend().setEnabled(false);

        XAxis x = lineChart.getXAxis();
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setTextColor(Color.WHITE);
        x.setDrawGridLines(true);

        YAxis left = lineChart.getAxisLeft();
        left.setTextColor(Color.WHITE);
        left.setDrawGridLines(true);

        lineChart.getAxisRight().setEnabled(false);
    }

    private void loadChartData(String coinId, int days) {
        CoinGeckoService service = RetrofitClient.getClient().create(CoinGeckoService.class);
        Call<CoinGeckoService.MarketChartResponse> call = service.getMarketChart(coinId, "usd", days);

        call.enqueue(new Callback<CoinGeckoService.MarketChartResponse>() {
            @Override
            public void onResponse(Call<CoinGeckoService.MarketChartResponse> call, Response<CoinGeckoService.MarketChartResponse> response) {
                if (!response.isSuccessful() || response.body() == null) return;

                List<List<Double>> prices = response.body().prices;
                List<Entry> entries = new ArrayList<>();

                for (int i = 0; i < prices.size(); i++) {
                    List<Double> pair = prices.get(i);
                    if (pair.size() >= 2) {
                        double price = pair.get(1);
                        entries.add(new Entry(i, (float) price));
                    }
                }

                LineDataSet dataSet = new LineDataSet(entries, coinId + " Price");
                dataSet.setColor(Color.parseColor("#A06BFF"));
                dataSet.setLineWidth(2.2f);
                dataSet.setDrawCircles(false);
                dataSet.setDrawValues(false);

                LineData lineData = new LineData(dataSet);
                lineChart.setData(lineData);
                lineChart.invalidate();
            }

            @Override
            public void onFailure(Call<CoinGeckoService.MarketChartResponse> call, Throwable t) {
                Log.e("CryptoChart", "chart load failure: " + t.getMessage());
            }
        });
    }

    private void fetchLivePrice(String coinId) {
        CoinGeckoService service = RetrofitClient.getClient().create(CoinGeckoService.class);
        Call<Map<String, Map<String, Double>>> call = service.getSimplePrice(coinId, "usd");

        call.enqueue(new Callback<Map<String, Map<String, Double>>>() {
            @Override
            public void onResponse(Call<Map<String, Map<String, Double>>> call, Response<Map<String, Map<String, Double>>> response) {
                if (!response.isSuccessful() || response.body() == null) return;
                Map<String, Map<String, Double>> body = response.body();
                Map<String, Double> coinMap = body.get(coinId);
                if (coinMap == null) return;
                Double price = coinMap.get("usd");
                if (price == null) return;
                tvLivePrice.setText(String.format(Locale.getDefault(), "$%.2f", price));
            }

            @Override
            public void onFailure(Call<Map<String, Map<String, Double>>> call, Throwable t) {
                Log.e("CryptoChart", "live price fail: " + t.getMessage());
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacks(livePriceRunnable);
    }
}
