package com.example.secondchance;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BookOrderAdapter extends RecyclerView.Adapter<BookOrderAdapter.OrderViewHolder> {
    private List<BuyNowActivity.OrderData> orderList;
    private Context context;

    public BookOrderAdapter(List<BuyNowActivity.OrderData> orderList, Context context) {
        this.orderList = orderList;
        this.context = context;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order_book, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        BuyNowActivity.OrderData order = orderList.get(position);

        // Fetch book title from items node using productId
        DatabaseReference itemsRef = FirebaseDatabase.getInstance().getReference("orderedProducts");
        itemsRef.child(order.productId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String title = snapshot.child("title").getValue(String.class);
                holder.bookTitle.setText(title != null ? title : "N/A");
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                holder.bookTitle.setText("N/A");
            }
        });

        holder.orderId.setText("Order ID: " + order.orderId);
        holder.orderDate.setText("Date: " + new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date(order.orderTime)));

        holder.btnCancelOrder.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Cancel Order")
                    .setMessage("Are you sure you want to cancel this order?")
                    .setPositiveButton("Yes", (dialog, which) -> cancelOrder(order, holder))
                    .setNegativeButton("No", null)
                    .show();
        });
    }

    private void cancelOrder(BuyNowActivity.OrderData order, OrderViewHolder holder) {
        int position = holder.getAdapterPosition();
        if (position == RecyclerView.NO_POSITION) {
            Toast.makeText(context, "Item position not found.", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference ordersRef = FirebaseDatabase.getInstance().getReference("orders");
        DatabaseReference itemsRef = FirebaseDatabase.getInstance().getReference("items");
        DatabaseReference orderedProductsRef = FirebaseDatabase.getInstance().getReference("orderedProducts");

        // 1. Get the product data from ordered_products
        orderedProductsRef.child(order.productId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // 2. Move product data back to items node
                    itemsRef.child(order.productId).setValue(snapshot.getValue())
                            .addOnSuccessListener(aVoid -> {
                                // 3. Delete the order
                                ordersRef.child(order.orderId).removeValue();
                                // 4. Remove from ordered_products
                                orderedProductsRef.child(order.productId).removeValue();
                                // 5. Remove from adapter list and notify
                                orderList.remove(position);
                                notifyItemRemoved(position);
                                Toast.makeText(context, "Order cancelled and item put back on sale.", Toast.LENGTH_SHORT).show();
                            });
                } else {
                    Toast.makeText(context, "Product data not found for restore.", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(context, "Failed to restore product.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView bookTitle, orderId, orderDate;
        Button btnCancelOrder;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            bookTitle = itemView.findViewById(R.id.textBookTitle);
            orderId = itemView.findViewById(R.id.textOrderId);
            orderDate = itemView.findViewById(R.id.textOrderDate);
            btnCancelOrder = itemView.findViewById(R.id.btnCancelOrder);
        }
    }
}