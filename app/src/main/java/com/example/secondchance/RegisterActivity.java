package com.example.secondchance;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.util.Patterns;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    EditText inputEmail, inputPassword, inputConfirmPassword, inputName;
    Button btnCreateAccount, btnAlreadyHaveAccount;
    ImageView togglePassword, toggleConfirmPassword;
    ProgressBar progressBar;
    FirebaseAuth mAuth;
    DatabaseReference db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_register);

        // Initialize Firebase Auth and Realtime Database
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance().getReference("users");

        // Initialize UI elements
        inputEmail = findViewById(R.id.inputEmail);
        inputPassword = findViewById(R.id.inputPassword);
        inputConfirmPassword = findViewById(R.id.inputConfirmPassword);
        inputName = findViewById(R.id.inputName); // Optional: Add name field if in layout
        btnCreateAccount = findViewById(R.id.btnCreateAccount);
        btnAlreadyHaveAccount = findViewById(R.id.btnAlreadyHaveAccount);
        togglePassword = findViewById(R.id.togglePassword);
        toggleConfirmPassword = findViewById(R.id.toggleConfirmPassword);
        progressBar = findViewById(R.id.progressBar); // Added for loading state

        final boolean[] isPasswordVisible = {false};
        final boolean[] isConfirmVisible = {false};

        // Toggle password visibility
        togglePassword.setOnClickListener(v -> {
            if (isPasswordVisible[0]) {
                inputPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                togglePassword.setImageResource(R.drawable.ic_eye_off);
            } else {
                inputPassword.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                togglePassword.setImageResource(R.drawable.ic_eye);
            }
            isPasswordVisible[0] = !isPasswordVisible[0];
            inputPassword.setSelection(inputPassword.length());
        });

        // Toggle confirm password visibility
        toggleConfirmPassword.setOnClickListener(v -> {
            if (isConfirmVisible[0]) {
                inputConfirmPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                toggleConfirmPassword.setImageResource(R.drawable.ic_eye_off);
            } else {
                inputConfirmPassword.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                toggleConfirmPassword.setImageResource(R.drawable.ic_eye);
            }
            isConfirmVisible[0] = !isConfirmVisible[0];
            inputConfirmPassword.setSelection(inputConfirmPassword.length());
        });

        // Create account button logic
        btnCreateAccount.setOnClickListener(v -> {
            String email = inputEmail.getText().toString().trim();
            String password = inputPassword.getText().toString().trim();
            String confirm = inputConfirmPassword.getText().toString().trim();
            String name = inputName != null ? inputName.getText().toString().trim() : "";

            // Input validation
            if (email.isEmpty()) {
                inputEmail.setError("Email is required");
                inputEmail.requestFocus();
                return;
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                inputEmail.setError("Invalid email format");
                inputEmail.requestFocus();
                return;
            }

            if (password.isEmpty()) {
                inputPassword.setError("Password is required");
                inputPassword.requestFocus();
                return;
            }

            if (password.length() < 6) {
                inputPassword.setError("Password must be at least 6 characters");
                inputPassword.requestFocus();
                return;
            }

            if (confirm.isEmpty()) {
                inputConfirmPassword.setError("Please confirm your password");
                inputConfirmPassword.requestFocus();
                return;
            }

            if (!password.equals(confirm)) {
                inputConfirmPassword.setError("Passwords do not match");
                inputConfirmPassword.requestFocus();
                return;
            }

            if (name.isEmpty() && inputName != null) {
                inputName.setError("Name is required");
                inputName.requestFocus();
                return;
            }

            // Show progress bar
            progressBar.setVisibility(View.VISIBLE);
            btnCreateAccount.setEnabled(false);

            // Firebase Authentication: Create user
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            // Get user ID
                            String userId = mAuth.getCurrentUser().getUid();

                            // Prepare user data for Realtime Database
                            Map<String, Object> userData = new HashMap<>();
                            userData.put("userId", userId);
                            userData.put("email", email);
                            userData.put("name", name.isEmpty() ? "User" : name);
                            userData.put("role", "user"); // Default role
                            userData.put("createdAt", System.currentTimeMillis());
                            userData.put("status", "active");

                            // Store user data in Realtime Database under /users/{userId}
                            db.child(userId).setValue(userData)
                                    .addOnSuccessListener(aVoid -> {
                                        // Send email verification
                                        mAuth.getCurrentUser().sendEmailVerification()
                                                .addOnSuccessListener(aVoid1 -> {
                                                    Toast.makeText(RegisterActivity.this, "Registration successful! Verification email sent.", Toast.LENGTH_LONG).show();
                                                    // Navigate to main activity
                                                    Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                                                    startActivity(intent);
                                                    finish();
                                                })
                                                .addOnFailureListener(e -> {
                                                    Toast.makeText(RegisterActivity.this, "Failed to send verification email: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                                    // Still navigate to main activity
                                                    Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                                                    startActivity(intent);
                                                    finish();
                                                });
                                    })
                                    .addOnFailureListener(e -> {
                                        progressBar.setVisibility(View.GONE);
                                        btnCreateAccount.setEnabled(true);
                                        Toast.makeText(RegisterActivity.this, "Failed to save user data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    });
                        } else {
                            progressBar.setVisibility(View.GONE);
                            btnCreateAccount.setEnabled(true);
                            // Handle specific Firebase errors
                            String errorMessage;
                            try {
                                throw task.getException();
                            } catch (FirebaseAuthException e) {
                                switch (e.getErrorCode()) {
                                    case "ERROR_EMAIL_ALREADY_IN_USE":
                                        errorMessage = "Email is already registered";
                                        inputEmail.setError(errorMessage);
                                        inputEmail.requestFocus();
                                        break;
                                    case "ERROR_WEAK_PASSWORD":
                                        errorMessage = "Password is too weak";
                                        inputPassword.setError(errorMessage);
                                        inputPassword.requestFocus();
                                        break;
                                    default:
                                        errorMessage = "Registration failed: " + e.getMessage();
                                        Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                                }
                            } catch (Exception e) {
                                errorMessage = "Registration failed: " + e.getMessage();
                                Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        });

        // Already have account button logic
        btnAlreadyHaveAccount.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }
}