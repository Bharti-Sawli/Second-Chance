package com.example.secondchance;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.MenuItem;
import android.view.View;
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

        // Load user data
        loadUserData();

        // Set click listeners for edit icons
        editName.setOnClickListener(v -> showEditDialog(nameValue, "name"));
        editPhone.setOnClickListener(v -> showEditDialog(phoneValue, "phone"));
        editAddress.setOnClickListener(v -> showEditDialog(addressValue, "address"));

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

                        profileName.setText(name != null ? name : "User Name");
                        profileEmail.setText(email != null ? email : "user@example.com");
                        nameValue.setText(name != null ? name : "their name");
                        emailValue.setText(email != null ? email : "their email");
                        phoneValue.setText(phone != null ? phone : "their phone number");
                        addressValue.setText(address != null ? address : "their address");
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
        builder.setTitle("Edit " + field.toUpperCase());

        final EditText input = new EditText(this);
        input.setText(textView.getText().toString());
        input.setSelection(input.getText().length()); // Move cursor to end

        // Set input type based on field
        if ("phone".equals(field)) {
            input.setInputType(InputType.TYPE_CLASS_PHONE);
        } else {
            input.setInputType(InputType.TYPE_CLASS_TEXT);
        }

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
                case "address":
                    userRef.child("address").setValue(value);
                    break;
            }

            Toast.makeText(this, field.toUpperCase() + " updated successfully", Toast.LENGTH_SHORT).show();
        }
    }
}