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
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.secondchance.databinding.ActivityLoginBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;

/**
 * LoginActivity - Handles user login with Firebase authentication.
 * Includes email verification, password toggle, error handling, and network check.
 */
public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private FirebaseAuth mAuth;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();

        // Toggle password visibility
        binding.togglePassword.setOnClickListener(v -> {
            isPasswordVisible = !isPasswordVisible;
            if (isPasswordVisible) {
                binding.inputPassword.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                binding.togglePassword.setImageResource(R.drawable.ic_eye);
            } else {
                binding.inputPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                binding.togglePassword.setImageResource(R.drawable.ic_eye_off);
            }
            binding.inputPassword.setSelection(binding.inputPassword.length());
        });

        // Login button logic
        binding.btnLogin.setOnClickListener(v -> {
            String email = binding.inputEmail.getText().toString().trim();
            String password = binding.inputPassword.getText().toString().trim();

            if (!validateInput(email, password)) return;

            if (!isNetworkAvailable()) {
                showToast(getString(R.string.no_internet));
                return;
            }

            showLoading(true);

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        showLoading(false);
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null && user.isEmailVerified()) {
                                showToast(getString(R.string.login_success));
                                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                finish();
                            } else {
                                showToast(getString(R.string.email_not_verified));
                                mAuth.signOut();
                            }
                        } else {
                            handleFirebaseError(task.getException());
                        }
                    });
        });

        // Register button logic
        binding.btnRegister.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));
    }

    private boolean validateInput(String email, String password) {
        if (email.isEmpty()) {
            binding.inputEmail.setError(getString(R.string.error_email_required));
            binding.inputEmail.requestFocus();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.inputEmail.setError(getString(R.string.error_email_invalid));
            binding.inputEmail.requestFocus();
            return false;
        }

        if (password.isEmpty()) {
            binding.inputPassword.setError(getString(R.string.error_password_required));
            binding.inputPassword.requestFocus();
            return false;
        }

        if (password.length() < 6) {
            binding.inputPassword.setError(getString(R.string.error_password_length));
            binding.inputPassword.requestFocus();
            return false;
        }

        return true;
    }

    private void handleFirebaseError(Exception exception) {
        String errorMessage;
        try {
            throw exception;
        } catch (FirebaseAuthException e) {
            switch (e.getErrorCode()) {
                case "ERROR_INVALID_EMAIL":
                    errorMessage = getString(R.string.error_email_invalid);
                    binding.inputEmail.setError(errorMessage);
                    binding.inputEmail.requestFocus();
                    break;
                case "ERROR_WRONG_PASSWORD":
                    errorMessage = getString(R.string.error_wrong_password);
                    binding.inputPassword.setError(errorMessage);
                    binding.inputPassword.requestFocus();
                    break;
                case "ERROR_USER_NOT_FOUND":
                    errorMessage = getString(R.string.error_user_not_found);
                    binding.inputEmail.setError(errorMessage);
                    binding.inputEmail.requestFocus();
                    break;
                case "ERROR_USER_DISABLED":
                    errorMessage = getString(R.string.error_user_disabled);
                    break;
                default:
                    errorMessage = "Login failed: " + e.getMessage();
                    Log.e("LoginActivity", "FirebaseAuthError: " + e.getErrorCode(), e);
            }
            showToast(errorMessage);
        } catch (Exception e) {
            errorMessage = "Login failed: " + e.getMessage();
            Log.e("LoginActivity", "Unknown error: ", e);
            showToast(errorMessage);
        }
    }

    private void showLoading(boolean isLoading) {
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.btnLogin.setEnabled(!isLoading);
    }

    private void showToast(String message) {
        Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnected();
    }
}
