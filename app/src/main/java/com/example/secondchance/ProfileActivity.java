package com.example.secondchance;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ProfileActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference db;
    private TextView profileName, profileEmail, nameValue, emailValue, phoneValue, addressValue;
    private ImageView editName, editPhone, editAddress;
    private BottomNavigationView bottomNavigation;
    private TextView textView2; // Reference to "SecondChance" TextView
    private ImageView logo, profileIcon; // References to logo and profile icon
    private Button btnPutBookOnSale;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance().getReference("users");

        // Initialize UI elements
        profileName = findViewById(R.id.profileName);
        profileEmail = findViewById(R.id.profileEmail);
        nameValue = findViewById(R.id.nameValue);
        emailValue = findViewById(R.id.emailValue);
        phoneValue = findViewById(R.id.phoneValue);
        addressValue = findViewById(R.id.addressValue);
        editName = findViewById(R.id.editName);
        editPhone = findViewById(R.id.editPhone);
        editAddress = findViewById(R.id.editAddress);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        textView2 = findViewById(R.id.textView2); // "SecondChance" text
        logo = findViewById(R.id.logo); // App logo
        profileIcon = findViewById(R.id.profileIcon); // Profile icon
        btnPutBookOnSale = findViewById(R.id.btnPutBookOnSale); // Put a Book on Sale button

        // Load user data
        loadUserData();

        // Set click listeners for edit icons
        editName.setOnClickListener(v -> showEditDialog(nameValue, "name"));
        editPhone.setOnClickListener(v -> showEditDialog(phoneValue, "phone"));
        editAddress.setOnClickListener(v -> showEditDialog(addressValue, "address"));

        // Set up header navigation
        textView2.setOnClickListener(v -> {
            startActivity(new Intent(ProfileActivity.this, MainActivity.class));
            finish();
        });

        logo.setOnClickListener(v -> {
            startActivity(new Intent(ProfileActivity.this, MainActivity.class));
            finish();
        });

        profileIcon.setOnClickListener(v -> {
            // Refresh or do nothing since already on ProfileActivity
            recreate(); // Optional: Recreates the activity to refresh data
        });

        // Set up "Put a Book on Sale" navigation
        btnPutBookOnSale.setOnClickListener(v -> {
            startActivity(new Intent(ProfileActivity.this, SaleBookActivity.class));
            finish();
        });

        // Set up bottom navigation
        bottomNavigation.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(ProfileActivity.this, MainActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_listings) {
                startActivity(new Intent(ProfileActivity.this, ListingsActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_profile) {
                // Already on profile, do nothing or refresh
                return true;
            }
            return false;
        });

        // Set default selected item to Profile
        bottomNavigation.setSelectedItemId(R.id.nav_profile);
    }

    private void loadUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String uid = user.getUid();

            // Fetch user details from Realtime Database
            db.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        String name = dataSnapshot.child("name").getValue(String.class);
                        String email = dataSnapshot.child("email").getValue(String.class);
                        String phone = dataSnapshot.child("phone").getValue(String.class);
                        String address = dataSnapshot.child("address").getValue(String.class);
                        String city = dataSnapshot.child("city").getValue(String.class);
                        String state = dataSnapshot.child("state").getValue(String.class);
                        String pincode = dataSnapshot.child("pincode").getValue(String.class);

                        profileName.setText(name != null ? name : "User Name");
                        profileEmail.setText(email != null ? email : "user@example.com");
                        nameValue.setText(name != null ? name : "their name");
                        emailValue.setText(email != null ? email : "their email");
                        phoneValue.setText(phone != null ? phone : "their phone number");
                        // Combine address, city, state, and pincode for display
                        String fullAddress = (address != null ? address : "") + ", " +
                                (city != null ? city : "") + ", " +
                                (state != null ? state : "") + ", " +
                                (pincode != null ? pincode : "");
                        addressValue.setText(fullAddress.isEmpty() ? "upload address" : fullAddress);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Toast.makeText(ProfileActivity.this, "Failed to load user data", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void showEditDialog(final TextView textView, final String field) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = null;

        if ("address".equals(field)) {
            builder.setTitle("Edit Address");
            dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_change_address, null);
            final EditText editAddress = dialogView.findViewById(R.id.editAddress);
            final EditText editCity = dialogView.findViewById(R.id.editCity);
            final EditText editState = dialogView.findViewById(R.id.editState);
            final EditText editPinCode = dialogView.findViewById(R.id.editPinCode);

            // Pre-fill existing values from the combined address string
            String[] addressParts = textView.getText().toString().split(", ");
            if (addressParts.length > 0) editAddress.setText(addressParts[0].isEmpty() ? "" : addressParts[0]);
            if (addressParts.length > 1) editCity.setText(addressParts[1].isEmpty() ? "" : addressParts[1]);
            if (addressParts.length > 2) editState.setText(addressParts[2].isEmpty() ? "" : addressParts[2]);
            if (addressParts.length > 3) editPinCode.setText(addressParts[3].isEmpty() ? "" : addressParts[3].replaceAll("[^0-9]", ""));

            builder.setView(dialogView);

            builder.setPositiveButton("Save", (dialog, which) -> {
                String newAddress = editAddress.getText().toString().trim();
                String newCity = editCity.getText().toString().trim();
                String newState = editState.getText().toString().trim();
                String newPinCode = editPinCode.getText().toString().trim();

                if (!newAddress.isEmpty() && !newCity.isEmpty() && !newState.isEmpty() && !newPinCode.isEmpty()) {
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null) {
                        String uid = user.getUid();
                        DatabaseReference userRef = db.child(uid);
                        userRef.child("address").setValue(newAddress);
                        userRef.child("city").setValue(newCity);
                        userRef.child("state").setValue(newState);
                        userRef.child("pincode").setValue(newPinCode);

                        // Update the display with the combined string
                        String fullAddress = newAddress + ", " + newCity + ", " + newState + ", " + newPinCode;
                        textView.setText(fullAddress);
                        addressValue.setText(fullAddress); // Ensure consistency
                        Toast.makeText(ProfileActivity.this, "Address updated successfully", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(ProfileActivity.this, "All fields are required", Toast.LENGTH_SHORT).show();
                }
            });

        } else if ("phone".equals(field)) {
            builder.setTitle("Edit Phone Number");
            dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_change_contact_number, null);
            final EditText editPhone = dialogView.findViewById(R.id.editPhone);
            editPhone.setText(textView.getText().toString());

            builder.setView(dialogView);

            builder.setPositiveButton("Save", (dialog, which) -> {
                String newValue = editPhone.getText().toString().trim();
                if (!newValue.isEmpty() && newValue.length() == 10 && newValue.matches("\\d+")) {
                    updateUserData(field, newValue);
                    textView.setText(newValue);
                    phoneValue.setText(newValue); // Ensure consistency
                } else {
                    Toast.makeText(ProfileActivity.this, "Enter a valid 10-digit phone number", Toast.LENGTH_SHORT).show();
                }
            });

        } else { // Default case for "name"
            builder.setTitle("Edit " + field.toUpperCase());
            final EditText input = new EditText(this);
            input.setText(textView.getText().toString());
            input.setSelection(input.getText().length()); // Move cursor to end
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            builder.setView(input);

            builder.setPositiveButton("Save", (dialog, which) -> {
                String newValue = input.getText().toString().trim();
                if (!newValue.isEmpty()) {
                    updateUserData(field, newValue);
                    textView.setText(newValue);
                    if ("name".equals(field)) {
                        profileName.setText(newValue); // Update profile header name
                        nameValue.setText(newValue);   // Ensure consistency
                    }
                } else {
                    Toast.makeText(ProfileActivity.this, "Field cannot be empty", Toast.LENGTH_SHORT).show();
                }
            });
        }

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void updateUserData(String field, String value) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            DatabaseReference userRef = db.child(uid);

            // Only update the specified field
            switch (field) {
                case "name":
                    userRef.child("name").setValue(value);
                    break;
                case "phone":
                    userRef.child("phone").setValue(value);
                    break;
            }

            Toast.makeText(this, field.toUpperCase() + " updated successfully", Toast.LENGTH_SHORT).show();
        }
    }
}