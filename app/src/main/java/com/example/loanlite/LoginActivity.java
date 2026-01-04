package com.example.loanlite;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;

public class LoginActivity extends AppCompatActivity {

    EditText etEmail, etPassword, etPhoneNumber;
    Button btnContinue;
    ProgressBar progressBar;
    TextView tvForgotPassword, tvRegister, textShowHide;

    FirebaseAuth mAuth;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 🔐 Auto-login check
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            checkUserVerification(currentUser.getUid());
            return;
        }

        setContentView(R.layout.activity_login);

        // Views
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        btnContinue = findViewById(R.id.btnContinue);
        progressBar = findViewById(R.id.progressBar);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvRegister = findViewById(R.id.tvRegister);
        textShowHide = findViewById(R.id.textShowHide);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Toggle password visibility
        textShowHide.setOnClickListener(v -> {
            if (etPassword.getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                textShowHide.setText("Hide");
            } else {
                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                textShowHide.setText("Show");
            }
            etPassword.setSelection(etPassword.getText().length());
        });

        // Login Button Click
        btnContinue.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String phone = etPhoneNumber.getText().toString().trim();

            if (email.isEmpty()) {
                etEmail.setError("Enter email");
                etEmail.requestFocus();
                return;
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.setError("Invalid email format");
                etEmail.requestFocus();
                return;
            }
            if (password.isEmpty()) {
                etPassword.setError("Enter password");
                etPassword.requestFocus();
                return;
            }
            if (phone.length() != 10) {
                etPhoneNumber.setError("Enter valid 10-digit phone");
                etPhoneNumber.requestFocus();
                return;
            }

            progressBar.setVisibility(View.VISIBLE);
            btnContinue.setEnabled(false);

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            String uid = mAuth.getCurrentUser().getUid();

                            db.collection("users").document(uid)
                                    .get()
                                    .addOnSuccessListener(document -> {
                                        progressBar.setVisibility(View.GONE);
                                        btnContinue.setEnabled(true);

                                        if (document.exists()) {
                                            String savedPhone = document.getString("phone");
                                            if (savedPhone != null && savedPhone.equals(phone)) {
                                                Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();
                                                checkUserVerification(uid);
                                            } else {
                                                Toast.makeText(this, "Phone number doesn't match our records", Toast.LENGTH_LONG).show();
                                                mAuth.signOut();
                                            }
                                        } else {
                                            Toast.makeText(this, "User data not found", Toast.LENGTH_LONG).show();
                                            mAuth.signOut();
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        progressBar.setVisibility(View.GONE);
                                        btnContinue.setEnabled(true);
                                        Toast.makeText(this, "Failed to fetch user data", Toast.LENGTH_SHORT).show();
                                    });

                        } else {
                            progressBar.setVisibility(View.GONE);
                            btnContinue.setEnabled(true);
                            Toast.makeText(this, "Login failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });

        // Forgot Password
        tvForgotPassword.setOnClickListener(v -> {
            startActivity(new Intent(this, ForgotPasswordActivity.class));
        });

        // Register New User
        tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });
    }

    private void checkUserVerification(String uid) {
        FirebaseFirestore.getInstance().collection("users").document(uid)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        Boolean isVerified = document.getBoolean("isVerified");

                        if (isVerified != null && isVerified) {
                            // ✅ Verified → go to dashboard
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        } else {
                            // 🔍 Check if user has uploaded details
                            String aadhaarNumber = document.getString("aadhaarNumber");
                            String panNumber = document.getString("panNumber");
                            String address = document.getString("address");
                            String pincode = document.getString("pincode");

                            boolean isUploaded = aadhaarNumber != null && !aadhaarNumber.isEmpty()
                                    && panNumber != null && !panNumber.isEmpty()
                                    && address != null && !address.isEmpty()
                                    && pincode != null && !pincode.isEmpty();

                            if (isUploaded) {
                                // 🚧 Uploaded but not verified → Pending screen
                                startActivity(new Intent(LoginActivity.this, PendingVerificationActivity.class));
                            } else {
                                // 📝 Details not uploaded → Upload screen
                                startActivity(new Intent(LoginActivity.this, UploadDocumentsActivity.class));
                            }
                        }
                        finish();
                    } else {
                        FirebaseAuth.getInstance().signOut();
                        startActivity(new Intent(LoginActivity.this, LoginActivity.class));
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Something went wrong!", Toast.LENGTH_SHORT).show();
                    FirebaseAuth.getInstance().signOut();
                    startActivity(new Intent(LoginActivity.this, LoginActivity.class));
                    finish();
                });
    }
}
