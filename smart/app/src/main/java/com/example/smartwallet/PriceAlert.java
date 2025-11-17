package com.example.smartwallet;

import com.google.firebase.Timestamp;

import java.util.HashMap;
import java.util.Map;

public class PriceAlert {

    public String id;
    public String coinId;
    public String operator;   // "above" or "below"
    public double target;
    public boolean active;
    public boolean triggered;
    public Timestamp createdAt;

    // REQUIRED empty constructor for Firestore
    public PriceAlert() {}

    // Your 3-parameter constructor (this FIXES your error)
    public PriceAlert(String coinId, String operator, double target) {
        this.coinId = coinId;
        this.operator = operator;
        this.target = target;
        this.active = true;
        this.triggered = false;
        this.createdAt = Timestamp.now();
    }

    // Firebase toMap() — REQUIRED (fixes the second error)
    public Map<String, Object> toMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("coinId", coinId);
        m.put("operator", operator);
        m.put("target", target);
        m.put("active", active);
        m.put("triggered", triggered);
        m.put("createdAt", createdAt);
        return m;
    }
}
