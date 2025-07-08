package com.example.secondchance;

import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import android.util.Patterns;


import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {

    EditText inputEmail, inputPassword, inputConfirmPassword;
    Button btnCreateAccount, btnAlreadyHaveAccount;
    ImageView togglePassword, toggleConfirmPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        inputEmail = findViewById(R.id.inputEmail);
        inputPassword = findViewById(R.id.inputPassword);
        inputConfirmPassword = findViewById(R.id.inputConfirmPassword);
        btnCreateAccount = findViewById(R.id.btnCreateAccount);
        btnAlreadyHaveAccount = findViewById(R.id.btnAlreadyHaveAccount);
        togglePassword = findViewById(R.id.togglePassword);
        toggleConfirmPassword = findViewById(R.id.toggleConfirmPassword);

        final boolean[] isPasswordVisible = {false};
        final boolean[] isConfirmVisible = {false};

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

        btnCreateAccount.setOnClickListener(v -> {
            String email = inputEmail.getText().toString().trim();
            String password = inputPassword.getText().toString().trim();
            String confirm = inputConfirmPassword.getText().toString().trim();

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

            // TODO: Firebase register logic here
            Toast.makeText(RegisterActivity.this, "Validation Passed!", Toast.LENGTH_SHORT).show();
        });


        btnAlreadyHaveAccount.setOnClickListener(v -> {
            finish(); // Go back to login screen
        });
    }
}