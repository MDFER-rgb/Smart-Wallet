package com.example.smartwallet.models;

import java.util.HashMap;
import java.util.Map;

public class Subscription {

    public String id;
    public String provider;
    public String plan;
    public double amount;
    public String currency;
    public String renewalDate;
    public String frequency;
    public boolean active;
    public String notes;

    public String logoUrl = "";   // Auto logo URL

    public Subscription() {}

    public Subscription(String id,
                        String provider,
                        String plan,
                        double amount,
                        String currency,
                        String renewalDate,
                        String frequency,
                        boolean active,
                        String notes) {

        this.id = id;
        this.provider = provider;
        this.plan = plan;
        this.amount = amount;
        this.currency = currency;
        this.renewalDate = renewalDate;
        this.frequency = frequency;
        this.active = active;
        this.notes = notes;

        // AUTO-SET LOGO URL BASED ON PROVIDER NAME
        this.logoUrl = "https://logo.clearbit.com/" +
                provider.toLowerCase().replace(" ", "") + ".com";
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();

        map.put("id", id);
        map.put("provider", provider);
        map.put("plan", plan);
        map.put("amount", amount);
        map.put("currency", currency);
        map.put("renewalDate", renewalDate);
        map.put("frequency", frequency);
        map.put("active", active);
        map.put("notes", notes);
        map.put("logoUrl", logoUrl);

        return map;
    }

    public static Subscription fromMap(Map<String, Object> map) {
        Subscription s = new Subscription();

        s.id = map.get("id").toString();
        s.provider = (String) map.get("provider");
        s.plan = (String) map.get("plan");
        s.amount = Double.parseDouble(map.get("amount").toString());
        s.currency = (String) map.get("currency");
        s.renewalDate = (String) map.get("renewalDate");
        s.frequency = (String) map.get("frequency");
        s.active = (boolean) map.get("active");
        s.notes = (String) map.get("notes");

        s.logoUrl = map.get("logoUrl") != null
                ? map.get("logoUrl").toString()
                : "";

        return s;
    }
}
