package com.example.smartwallet;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.smartwallet.models.Subscription;

import java.util.List;

public class SubscriptionAdapter extends RecyclerView.Adapter<SubscriptionAdapter.Holder> {

    public interface Listener {
        void onEdit(Subscription s);
        void onDelete(Subscription s);
        void onToggleActive(Subscription s, boolean active);
    }

    private final List<Subscription> list;
    private final Listener listener;

    public SubscriptionAdapter(List<Subscription> list, Listener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_subscription, parent, false);

        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {

        Subscription s = list.get(position);

        h.tvProvider.setText(s.provider);
        h.tvPlan.setText(s.plan);
        h.tvAmount.setText(s.currency + " " + s.amount);

        // Load provider logo
        Glide.with(h.itemView.getContext())
                .load(s.logoUrl)
                .placeholder(R.drawable.ic_placeholder)
                .into(h.imgLogo);

        // Tap → Edit
        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(s);
        });

        // Long press → Delete
        h.itemView.setOnLongClickListener(v -> {
            if (listener != null) listener.onDelete(s);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class Holder extends RecyclerView.ViewHolder {

        ImageView imgLogo;
        TextView tvProvider, tvPlan, tvAmount;

        public Holder(@NonNull View v) {
            super(v);

            imgLogo = v.findViewById(R.id.imgLogo);
            tvProvider = v.findViewById(R.id.tvProvider);
            tvPlan = v.findViewById(R.id.tvPlan);
            tvAmount = v.findViewById(R.id.tvAmount);
        }
    }
}
