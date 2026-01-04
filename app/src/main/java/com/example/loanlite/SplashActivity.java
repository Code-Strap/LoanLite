package com.example.loanlite;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 1000;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        new Handler().postDelayed(this::checkUserStatus, SPLASH_DELAY);
    }

    private void checkUserStatus() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            // Not logged in
            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            finish();
        } else {
            String uid = currentUser.getUid();

            db.collection("users").document(uid).get()
                    .addOnSuccessListener(document -> {
                        if (!document.exists()) {
                            // User not registered yet (no Firestore doc)
                            mAuth.signOut(); // optional
                            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                            finish();
                        } else {
                            boolean isVerified = document.getBoolean("isVerified") != null &&
                                    document.getBoolean("isVerified");
                            boolean isAdmin = document.getBoolean("isAdmin") != null &&
                                    document.getBoolean("isAdmin");

                            boolean hasUploadedDetails =
                                    document.contains("aadhaarNumber") &&
                                            document.contains("panNumber") &&
                                            document.contains("address") &&
                                            document.contains("pincode");

                            if (isAdmin) {
                                // Go to admin dashboard (to be implemented)
                                startActivity(new Intent(SplashActivity.this, AdminDashboardActivity.class));
                            } else if (!hasUploadedDetails) {
                                // Details not uploaded
                                startActivity(new Intent(SplashActivity.this, UploadDocumentsActivity.class));
                            } else if (!isVerified) {
                                // Uploaded but pending
                                startActivity(new Intent(SplashActivity.this, PendingVerificationActivity.class));
                            } else {
                                // Verified user
                                startActivity(new Intent(SplashActivity.this, MainActivity.class));
                            }
                            finish();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                        finish();
                    });
        }
    }
}
