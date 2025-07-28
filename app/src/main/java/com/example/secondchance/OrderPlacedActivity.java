package com.example.secondchance;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class OrderPlacedActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_placed);

        // Initialize views
        ImageView successIcon = findViewById(R.id.successIcon); // Replaced VideoView with ImageView
        TextView orderMessage = findViewById(R.id.orderMessage);
        TextView orderId = findViewById(R.id.orderId);
        TextView deliveryDate = findViewById(R.id.deliveryDate);
        TextView paymentMethod = findViewById(R.id.paymentMethod);
        Button backButton = findViewById(R.id.backButton);
        ImageView backIcon = findViewById(R.id.backIcon);

        // Get data from intent
        String itemId = getIntent().getStringExtra("itemId");
        String paymentMethodText = getIntent().getStringExtra("paymentMethod");
        String phone = getIntent().getStringExtra("phone");

        // Set order message with payment method and phone number if available
        if (paymentMethodText != null && phone != null) {
            orderMessage.setText("🎉 Order Successfully Placed!\nwith " + paymentMethodText + "!");
        } else if (paymentMethodText != null) {
            orderMessage.setText("🎉 Order Successfully Placed with " + paymentMethodText + "!");
        } else {
            orderMessage.setText("🎉 Order Successfully Placed!");
        }

        // Set order details
        orderId.setText(getIntent().getStringExtra("orderId")); // Generate a unique order ID based on timestamp
        deliveryDate.setText("3-5 Business Days"); // Static value as per layout
        paymentMethod.setText(paymentMethodText != null ? paymentMethodText : "Cash on Delivery"); // Default to COD if not provided

        // Back icon click listener
        backIcon.setOnClickListener(v -> {
            Intent intent = new Intent(OrderPlacedActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        // Back button click listener
        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(OrderPlacedActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }
}