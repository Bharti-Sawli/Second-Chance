package com.example.secondchance;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    EditText inputEmail, inputPassword;
    Button btnLogin, btnRegister;
    ImageView togglePassword;
    ProgressBar progressBar; // Added for loading state
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize UI elements
        inputEmail = findViewById(R.id.inputEmail);
        inputPassword = findViewById(R.id.inputPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        togglePassword = findViewById(R.id.togglePassword);
        progressBar = findViewById(R.id.progressBar); // Ensure this ID exists in layout

        final boolean[] isPasswordVisible = {false};

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

        // Login button logic
        btnLogin.setOnClickListener(v -> {
            String email = inputEmail.getText().toString().trim();
            String password = inputPassword.getText().toString().trim();

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

            // Check network availability
            if (!isNetworkAvailable()) {
                Toast.makeText(LoginActivity.this, "No internet connection. Please check your network.", Toast.LENGTH_LONG).show();
                return;
            }

            // Show progress bar
            progressBar.setVisibility(View.VISIBLE);
            btnLogin.setEnabled(false);

            // Firebase Authentication: Sign in user
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        progressBar.setVisibility(View.GONE);
                        btnLogin.setEnabled(true);
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                // Check if email is verified
                                if (user.isEmailVerified()) {
                                    Toast.makeText(LoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                    finish();
                                } else {
                                    // Email not verified
                                    Toast.makeText(LoginActivity.this, "Please verify your email address", Toast.LENGTH_LONG).show();
                                    mAuth.signOut(); // Sign out the user
                                }
                            }
                        } else {
                            // Handle specific Firebase errors
                            String errorMessage;
                            try {
                                throw task.getException();
                            } catch (FirebaseAuthException e) {
                                switch (e.getErrorCode()) {
                                    case "ERROR_INVALID_EMAIL":
                                        errorMessage = "Invalid email format";
                                        inputEmail.setError(errorMessage);
                                        inputEmail.requestFocus();
                                        break;
                                    case "ERROR_WRONG_PASSWORD":
                                        errorMessage = "Incorrect password";
                                        inputPassword.setError(errorMessage);
                                        inputPassword.requestFocus();
                                        break;
                                    case "ERROR_USER_NOT_FOUND":
                                        errorMessage = "No account found with this email";
                                        inputEmail.setError(errorMessage);
                                        inputEmail.requestFocus();
                                        break;
                                    case "ERROR_USER_DISABLED":
                                        errorMessage = "This account has been disabled";
                                        Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                                        break;
                                    default:
                                        errorMessage = "Login failed: " + e.getMessage();
                                        Log.e("LoginActivity", "FirebaseAuthError: " + e.getErrorCode() + " - " + e.getMessage());
                                        Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                                }
                            } catch (Exception e) {
                                errorMessage = "Login failed: " + e.getMessage();
                                Log.e("LoginActivity", "Error: ", e);
                                Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        });

        // Register button logic
        btnRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }

    // Check network availability
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}