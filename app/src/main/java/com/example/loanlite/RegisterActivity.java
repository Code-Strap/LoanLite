package com.example.loanlite;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.*;
import com.google.firebase.firestore.*;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    EditText etFullName, etPhoneNumber, etEmail, etPassword;
    RadioGroup rgGender;
    CheckBox cbAgeConfirm;
    Button btnRegister;
    ProgressBar progressBar;
    TextView tvLoginLink, textShowHide;

    FirebaseAuth mAuth;
    FirebaseFirestore db;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize views
        etFullName = findViewById(R.id.etFullName);
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        rgGender = findViewById(R.id.rgGender);
        cbAgeConfirm = findViewById(R.id.cbAgeConfirm);
        btnRegister = findViewById(R.id.btnRegister);
        progressBar = findViewById(R.id.progressBar);
        tvLoginLink = findViewById(R.id.tvLoginLink);
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

        // 🔘 Handle DONE key from password input
        etPassword.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                btnRegister.performClick();
                return true;
            }
            return false;
        });

        btnRegister.setOnClickListener(v -> {
            String fullName = etFullName.getText().toString().trim();
            String phone = etPhoneNumber.getText().toString().trim();
            String email = etEmail.getText().toString().trim().toLowerCase();
            String password = etPassword.getText().toString().trim();
            int selectedGenderId = rgGender.getCheckedRadioButtonId();

            if (!isConnected()) {
                Toast.makeText(this, "No internet connection", Toast.LENGTH_LONG).show();
                return;
            }

            // Input validation
            if (fullName.isEmpty() || fullName.trim().isEmpty()) {
                etFullName.setError("Enter your full name");
                etFullName.requestFocus();
                return;
            }

            if (!phone.matches("[6-9][0-9]{9}")) {
                etPhoneNumber.setError("Enter valid 10-digit Indian phone");
                etPhoneNumber.requestFocus();
                return;
            }

            if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() || email.contains("..")) {
                etEmail.setError("Enter valid email address");
                etEmail.requestFocus();
                return;
            }

            if (password.length() < 6 || !password.matches(".*[A-Za-z].*") || !password.matches(".*[0-9].*")) {
                etPassword.setError("Password must have letters and digits");
                etPassword.requestFocus();
                return;
            }

            if (selectedGenderId == -1) {
                Toast.makeText(this, "Please select gender", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!cbAgeConfirm.isChecked()) {
                Toast.makeText(this, "You must confirm you're 18+", Toast.LENGTH_SHORT).show();
                return;
            }

            progressBar.setVisibility(View.VISIBLE);
            btnRegister.setEnabled(false);

            // ✅ Create user in Firebase Auth
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(authTask -> {
                        if (authTask.isSuccessful()) {
                            FirebaseUser firebaseUser = authTask.getResult().getUser();
                            if (firebaseUser == null) {
                                Toast.makeText(this, "Unexpected error", Toast.LENGTH_LONG).show();
                                progressBar.setVisibility(View.GONE);
                                btnRegister.setEnabled(true);
                                return;
                            }

                            String uid = firebaseUser.getUid();
                            String gender = ((RadioButton) findViewById(selectedGenderId)).getText().toString();

                            Map<String, Object> userMap = new HashMap<>();
                            userMap.put("uid", uid);
                            userMap.put("fullName", fullName);
                            userMap.put("phone", phone);
                            userMap.put("email", email);
                            userMap.put("gender", gender);
                            userMap.put("isVerified", false);
                            userMap.put("isAdmin", false);

                            db.collection("users").document(uid)
                                    .set(userMap)
                                    .addOnSuccessListener(unused -> {
                                        Toast.makeText(this, "Registered successfully!", Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(this, LoginActivity.class));
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this, "Failed to save user data", Toast.LENGTH_LONG).show();
                                        progressBar.setVisibility(View.GONE);
                                        btnRegister.setEnabled(true);
                                    });

                        } else {
                            Exception e = authTask.getException();
                            if (e instanceof FirebaseAuthUserCollisionException) {
                                Toast.makeText(this, "Email already registered", Toast.LENGTH_LONG).show();
                            } else if (e instanceof FirebaseAuthWeakPasswordException) {
                                etPassword.setError("Weak password");
                                etPassword.requestFocus();
                            } else {
                                Toast.makeText(this, "Registration failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                            progressBar.setVisibility(View.GONE);
                            btnRegister.setEnabled(true);
                        }
                    });
        });

        tvLoginLink.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private boolean isConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return (netInfo != null && netInfo.isConnected());
    }
}
