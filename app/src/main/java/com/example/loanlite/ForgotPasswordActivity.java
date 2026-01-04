package com.example.loanlite;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    EditText etEmail;
    Button btnResetPassword;
    ProgressBar progressBar;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        etEmail = findViewById(R.id.etEmail);
        btnResetPassword = findViewById(R.id.btnResetPassword);
        progressBar = findViewById(R.id.progressBar);

        mAuth = FirebaseAuth.getInstance();

        btnResetPassword.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();

            // Validate email
            if (TextUtils.isEmpty(email)) {
                etEmail.setError("Enter your registered email");
                etEmail.requestFocus();
                return;
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.setError("Enter a valid email");
                etEmail.requestFocus();
                return;
            }

            // Show progress
            progressBar.setVisibility(View.VISIBLE);
            btnResetPassword.setEnabled(false);

            // Send reset email
            mAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        progressBar.setVisibility(View.GONE);
                        btnResetPassword.setEnabled(true);

                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Password reset email sent!", Toast.LENGTH_LONG).show();

                            // Navigate to LoginActivity after short delay
                            new android.os.Handler().postDelayed(() -> {
                                Intent intent = new Intent(ForgotPasswordActivity.this, LoginActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                finish();
                            }, 1000);

                        } else {
                            String error = task.getException() != null ? task.getException().getMessage() : "Something went wrong!";
                            Toast.makeText(this, "Error: " + error, Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }
}
