package com.example.smartwallet;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface CoinGeckoService {

    @GET("coins/{id}/market_chart")
    Call<MarketChartResponse> getMarketChart(
            @Path("id") String id,
            @Query("vs_currency") String vsCurrency,
            @Query("days") int days
    );

    @GET("simple/price")
    Call<Map<String, Map<String, Double>>> getSimplePrice(
            @Query("ids") String ids,
            @Query("vs_currencies") String vsCurrencies
    );

    class MarketChartResponse {
        @SerializedName("prices")
        public List<List<Double>> prices;
    }
}
