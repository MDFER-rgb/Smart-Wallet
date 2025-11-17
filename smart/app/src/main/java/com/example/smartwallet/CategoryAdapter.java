package com.example.smartwallet;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    private List<CategoryModel> list;
    private double totalSpent;

    public CategoryAdapter(List<CategoryModel> list, double totalSpent) {
        this.list = list;
        this.totalSpent = totalSpent;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_spending_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        CategoryModel data = list.get(position);

        holder.tvName.setText(data.name);
        holder.tvAmount.setText("$" + data.amount);
        holder.tvSubtitle.setText(data.transactions + " transactions");
        holder.imgIcon.setImageResource(data.icon);

        // progress_percent = (category_amount / totalSpent) * 100
        int percent = totalSpent == 0 ? 0 : (int)((data.amount / totalSpent) * 100);
        holder.progress.setProgress(percent);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {

        TextView tvName, tvSubtitle, tvAmount;
        ImageView imgIcon;
        ProgressBar progress;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvCategoryName);
            tvSubtitle = itemView.findViewById(R.id.tvCategorySubtitle);
            tvAmount = itemView.findViewById(R.id.tvCategoryAmount);
            imgIcon = itemView.findViewById(R.id.imgCategoryIcon);
            progress = itemView.findViewById(R.id.progCategory);
        }
    }
}
