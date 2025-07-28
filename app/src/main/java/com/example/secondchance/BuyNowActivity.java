package com.example.secondchance;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BuyNowActivity extends AppCompatActivity {

    private TextView itemTitle, itemPrice, sellerName, deliveryInfo, deliveryLocation, contactNumber,
            itemTotal, deliveryCharges, totalAmount, bottomTotalAmount, changeAddress, changeContactNumber;
    private ImageView itemImage, backIcon;
    private Button btnProceedToPay;
    private RadioButton cashOnDelivery, upi, creditCard; // Changed from CheckBox to RadioButton
    private DatabaseReference itemsRef, usersRef, ordersRef, orderedProductsRef;
    private String itemId;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buy_now);

        // Initialize views
        itemTitle = findViewById(R.id.itemTitle);
        itemPrice = findViewById(R.id.itemPrice);
        sellerName = findViewById(R.id.sellerName);
        deliveryInfo = findViewById(R.id.deliveryInfo);
        deliveryLocation = findViewById(R.id.deliveryLocation);
        contactNumber = findViewById(R.id.contactNumber);
        itemImage = findViewById(R.id.itemImage);
        backIcon = findViewById(R.id.backIcon);
        btnProceedToPay = findViewById(R.id.btnProceedToPay);
        changeAddress = findViewById(R.id.changeAddress); // Changed from Button to TextView
        changeContactNumber = findViewById(R.id.changeContactNumber); // Changed from Button to TextView
        cashOnDelivery = findViewById(R.id.cashOnDelivery); // Changed from CheckBox to RadioButton
        upi = findViewById(R.id.upi); // Changed from CheckBox to RadioButton
        creditCard = findViewById(R.id.creditCard); // Changed from CheckBox to RadioButton
        itemTotal = findViewById(R.id.itemTotal);
        deliveryCharges = findViewById(R.id.deliveryCharges);
        totalAmount = findViewById(R.id.totalAmount);
        bottomTotalAmount = findViewById(R.id.bottomTotalAmount);

        // Ensure only one payment method can be selected at a time (using RadioButton behavior)
        View.OnClickListener paymentListener = v -> {
            if (v instanceof RadioButton) {
                RadioButton clicked = (RadioButton) v;
                if (clicked.isChecked()) {
                    cashOnDelivery.setChecked(clicked == cashOnDelivery);
                    upi.setChecked(clicked == upi);
                    creditCard.setChecked(clicked == creditCard);
                    updateProceedButtonText();
                }
            }
        };
        cashOnDelivery.setOnClickListener(paymentListener);
        upi.setOnClickListener(paymentListener);
        creditCard.setOnClickListener(paymentListener);

        // Initialize Firebase
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        itemsRef = database.getReference("items");
        usersRef = database.getReference("users");
        ordersRef = database.getReference("orders");
        orderedProductsRef = database.getReference("orderedProducts");
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // Set up back icon click listener
        backIcon.setOnClickListener(v -> onBackPressed());



        // Get itemId from intent
        itemId = getIntent().getStringExtra("itemId");
        if (itemId == null) {
            Toast.makeText(this, "Item not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Fetch item details
        itemsRef.child(itemId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String title = snapshot.child("title").getValue(String.class);
                    String price = snapshot.child("price").getValue(String.class);
                    String sellerUserId = snapshot.child("userId").getValue(String.class);
                    String imageUrl1 = snapshot.child("imageUrl1").getValue(String.class);

                    itemTitle.setText(title != null ? title : "N/A");
                    itemPrice.setText(price != null ? "₹" + price : "₹0");
                    itemTotal.setText(price != null ? "₹" + price : "₹0");
                    deliveryCharges.setText("FREE");
                    totalAmount.setText(price != null ? "₹" + price : "₹0");
                    bottomTotalAmount.setText(price != null ? "₹" + price : "₹0");
                    deliveryInfo.setText("Expected delivery: 3-5 business days");

                    Glide.with(BuyNowActivity.this)
                            .load(imageUrl1 != null ? imageUrl1 : R.drawable.placeholder_image)
                            .placeholder(R.drawable.placeholder_image)
                            .into(itemImage);

                    // Fetch seller name and contact
                    if (sellerUserId != null) {
                        usersRef.child(sellerUserId).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                                if (userSnapshot.exists()) {
                                    String name = userSnapshot.child("name").getValue(String.class);
                                    String sellerPhone = userSnapshot.child("phone").getValue(String.class);
                                    sellerName.setText(name != null ? "Sold by: " + name : "Sold by: Unknown");
                                } else {
                                    sellerName.setText("Sold by: Unknown");
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                sellerName.setText("Sold by: Unknown");
                                Log.e("BuyNowActivity", "Failed to fetch seller: ", error.toException());
                            }
                        });
                    } else {
                        sellerName.setText("Sold by: Unknown");
                    }
                } else {
                    Toast.makeText(BuyNowActivity.this, "Item not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(BuyNowActivity.this, "Failed to load item", Toast.LENGTH_SHORT).show();
                Log.e("BuyNowActivity", "Failed to load item: ", error.toException());
            }
        });

        // Fetch and display user's address and contact number
        if (currentUser != null) {
            usersRef.child(currentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String pinCode = snapshot.child("pinCode").getValue(String.class);
                        String city = snapshot.child("city").getValue(String.class);
                        String state = snapshot.child("state").getValue(String.class);
                        String address = snapshot.child("address").getValue(String.class);
                        String phone = snapshot.child("phone").getValue(String.class);
                        String deliveryText = (address != null ? address + ", " : "") +
                                (city != null ? city + ", " : "") +
                                (state != null ? state + ", " : "") +
                                (pinCode != null ? pinCode : "");
                        deliveryLocation.setText(deliveryText.isEmpty() ? "[Not set]" : deliveryText);
                        contactNumber.setText(phone != null ? phone : "[Not set]");
                    } else {
                        deliveryLocation.setText("[Not set]");
                        contactNumber.setText("[Not set]");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    deliveryLocation.setText("[Not set]");
                    contactNumber.setText("[Not set]");
                    Log.e("BuyNowActivity", "Failed to fetch user data: ", error.toException());
                }
            });
        } else {
            deliveryLocation.setText("[Not logged in]");
            contactNumber.setText("[Not set]");
        }

        // Change Address button click (now TextView)
        changeAddress.setOnClickListener(v -> showAddressChangeDialog());

        // Change Contact Number button click (now TextView)
        changeContactNumber.setOnClickListener(v -> showContactNumberDialog());

        // Proceed to Pay button click
        btnProceedToPay.setOnClickListener(v -> {
            String selectedPayment = getSelectedPaymentMethod();
            String phone = contactNumber.getText().toString().trim();
            if (selectedPayment == null) {
                Toast.makeText(this, "Please select a payment method", Toast.LENGTH_SHORT).show();
            } else if (phone.equals("[Not set]")) {
                showContactNumberDialog();
            } else if (selectedPayment.equals("Cash on Delivery")) {
                itemsRef.child(itemId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String sellerUserId = snapshot.child("userId").getValue(String.class);
                            if (sellerUserId != null) {
                                usersRef.child(sellerUserId).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                                        if (userSnapshot.exists()) {
                                            String sellerPhone = userSnapshot.child("phone").getValue(String.class);
                                            String buyerUserId = currentUser != null ? currentUser.getUid() : "anonymous";
                                            String buyerPhone = contactNumber.getText().toString().trim();
                                            String deliveryLoc = deliveryLocation.getText().toString().trim();

                                            String orderId = UUID.randomUUID().toString();
                                            long orderTime = System.currentTimeMillis();

                                            OrderData orderData = new OrderData(
                                                    sellerUserId,
                                                    buyerUserId,
                                                    orderTime,
                                                    itemId,
                                                    orderId,
                                                    deliveryLoc,
                                                    sellerPhone != null ? sellerPhone : "Not set",
                                                    buyerPhone
                                            );

                                            ordersRef.child(orderId).setValue(orderData)
                                                    .addOnSuccessListener(aVoid -> {
                                                        Map<String, Object> productData = new HashMap<>();
                                                        productData.put("author", snapshot.child("author").getValue(String.class));
                                                        productData.put("category", snapshot.child("category").getValue(String.class));
                                                        productData.put("condition", snapshot.child("condition").getValue(String.class));
                                                        productData.put("description", snapshot.child("description").getValue(String.class));
                                                        productData.put("id", snapshot.child("id").getValue(String.class));
                                                        productData.put("price", snapshot.child("price").getValue(String.class));
                                                        productData.put("title", snapshot.child("title").getValue(String.class));
                                                        productData.put("userId", snapshot.child("userId").getValue(String.class));
                                                        productData.put("imageUrl1", snapshot.child("imageUrl1").getValue(String.class));
                                                        productData.put("imageUrl2", snapshot.child("imageUrl2").getValue(String.class));

                                                        orderedProductsRef.child(itemId).setValue(productData)
                                                                .addOnSuccessListener(aVoid2 -> {
                                                                    itemsRef.child(itemId).removeValue()
                                                                            .addOnSuccessListener(aVoid3 -> {
                                                                                Toast.makeText(BuyNowActivity.this, "Order placed successfully!", Toast.LENGTH_SHORT).show();
                                                                                Intent intent = new Intent(BuyNowActivity.this, OrderPlacedActivity.class);
                                                                                intent.putExtra("itemId", itemId);
                                                                                intent.putExtra("orderId",orderId);
                                                                                intent.putExtra("paymentMethod", selectedPayment);
                                                                                intent.putExtra("phone", buyerPhone);
                                                                                startActivity(intent);
                                                                                finish();
                                                                            })
                                                                            .addOnFailureListener(e3 -> {
                                                                                Toast.makeText(BuyNowActivity.this, "Failed to delete product", Toast.LENGTH_SHORT).show();
                                                                                Log.e("BuyNowActivity", "Product deletion failed: ", e3);
                                                                            });
                                                                })
                                                                .addOnFailureListener(e2 -> {
                                                                    Toast.makeText(BuyNowActivity.this, "Failed to copy product data", Toast.LENGTH_SHORT).show();
                                                                    Log.e("BuyNowActivity", "Product copy failed: ", e2);
                                                                });
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        Toast.makeText(BuyNowActivity.this, "Failed to place order", Toast.LENGTH_SHORT).show();
                                                        Log.e("BuyNowActivity", "Order placement failed: ", e);
                                                    });
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        Toast.makeText(BuyNowActivity.this, "Failed to fetch seller details", Toast.LENGTH_SHORT).show();
                                        Log.e("BuyNowActivity", "Seller fetch cancelled: ", error.toException());
                                    }
                                });
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(BuyNowActivity.this, "Failed to load item", Toast.LENGTH_SHORT).show();
                        Log.e("BuyNowActivity", "Item fetch cancelled: ", error.toException());
                    }
                });
            } else {
                Toast.makeText(this, "Proceeding with " + selectedPayment + ". Payment integration pending.", Toast.LENGTH_SHORT).show();
                // TODO: Integrate with a payment gateway (e.g., Razorpay, Stripe)
            }
        });
    }

    private void updateProceedButtonText() {
        if (cashOnDelivery.isChecked()) {
            btnProceedToPay.setText("Place Order");
        } else {
            btnProceedToPay.setText("Proceed to Pay");
        }
    }

    private String getSelectedPaymentMethod() {
        if (cashOnDelivery.isChecked()) return "Cash on Delivery";
        if (upi.isChecked()) return "UPI";
        if (creditCard.isChecked()) return "Credit/Debit Card";
        return null;
    }

    private void showAddressChangeDialog() {
        if (currentUser == null) {
            Toast.makeText(this, "Please log in to change address", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_address, null);
        builder.setView(dialogView);

        EditText editPinCode = dialogView.findViewById(R.id.editPinCode);
        EditText editCity = dialogView.findViewById(R.id.editCity);
        EditText editState = dialogView.findViewById(R.id.editState);
        EditText editAddress = dialogView.findViewById(R.id.editAddress);

        usersRef.child(currentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String pinCode = snapshot.child("pinCode").getValue(String.class);
                    String city = snapshot.child("city").getValue(String.class);
                    String state = snapshot.child("state").getValue(String.class);
                    String address = snapshot.child("address").getValue(String.class);
                    if (pinCode != null) editPinCode.setText(pinCode);
                    if (city != null) editCity.setText(city);
                    if (state != null) editState.setText(state);
                    if (address != null) editAddress.setText(address);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("BuyNowActivity", "Failed to pre-fill address: ", error.toException());
            }
        });

        builder.setTitle("Change Delivery Address")
                .setPositiveButton("Save", (dialog, which) -> {
                    String newPinCode = editPinCode.getText().toString().trim();
                    String newCity = editCity.getText().toString().trim();
                    String newState = editState.getText().toString().trim();
                    String newAddress = editAddress.getText().toString().trim();

                    if (newPinCode.isEmpty() || newCity.isEmpty() || newState.isEmpty() || newAddress.isEmpty()) {
                        Toast.makeText(BuyNowActivity.this, "All fields are required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String userId = currentUser.getUid();
                    usersRef.child(userId).child("pinCode").setValue(newPinCode);
                    usersRef.child(userId).child("city").setValue(newCity);
                    usersRef.child(userId).child("state").setValue(newState);
                    usersRef.child(userId).child("address").setValue(newAddress)
                            .addOnSuccessListener(aVoid -> {
                                String deliveryText = newAddress + ", " + newCity + ", " + newState + ", " + newPinCode;
                                deliveryLocation.setText(deliveryText);
                                Toast.makeText(BuyNowActivity.this, "Address updated", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(BuyNowActivity.this, "Failed to update address", Toast.LENGTH_SHORT).show();
                                Log.e("BuyNowActivity", "Address update failed: ", e);
                            });
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showContactNumberDialog() {
        if (currentUser == null) {
            Toast.makeText(this, "Please log in to add contact number", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_contact_number, null);
        builder.setView(dialogView);

        EditText editPhone = dialogView.findViewById(R.id.editPhone);
        usersRef.child(currentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String phone = snapshot.child("phone").getValue(String.class);
                    if (phone != null) editPhone.setText(phone);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("BuyNowActivity", "Failed to pre-fill phone: ", error.toException());
            }
        });

        builder.setTitle("Change Contact Number")
                .setPositiveButton("Save", (dialog, which) -> {
                    String newPhone = editPhone.getText().toString().trim();
                    if (newPhone.isEmpty() || !newPhone.matches("\\d{10}")) {
                        Toast.makeText(BuyNowActivity.this, "Please enter a valid 10-digit phone number", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String userId = currentUser.getUid();
                    usersRef.child(userId).child("phone").setValue(newPhone)
                            .addOnSuccessListener(aVoid -> {
                                contactNumber.setText(newPhone);
                                Toast.makeText(BuyNowActivity.this, "Contact number updated", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(BuyNowActivity.this, "Failed to update contact number", Toast.LENGTH_SHORT).show();
                                Log.e("BuyNowActivity", "Contact update failed: ", e);
                            });
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // Order data class to structure the order details
    private static class OrderData {
        public String sellerUserId;
        public String buyerUserId;
        public long orderTime;
        public String productId;
        public String orderId;
        public String deliveryLocation;
        public String sellerPhone;
        public String buyerPhone;

        public OrderData(String sellerUserId, String buyerUserId, long orderTime, String productId, String orderId, String deliveryLocation, String sellerPhone, String buyerPhone) {
            this.sellerUserId = sellerUserId;
            this.buyerUserId = buyerUserId;
            this.orderTime = orderTime;
            this.productId = productId;
            this.orderId = orderId;
            this.deliveryLocation = deliveryLocation;
            this.sellerPhone = sellerPhone;
            this.buyerPhone = buyerPhone;
        }
    }
}