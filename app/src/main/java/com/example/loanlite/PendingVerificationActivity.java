package com.example.loanlite;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class PendingVerificationActivity extends AppCompatActivity {

    private TextView textAadhaar, textPan, textAddress, textPincode;
    private Button btnEditDetails;
    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_verification);

        textAadhaar = findViewById(R.id.textAadhaarDisplay);
        textPan = findViewById(R.id.textPanDisplay);
        textAddress = findViewById(R.id.textAddressDisplay);
        textPincode = findViewById(R.id.textPincodeDisplay);
        btnEditDetails = findViewById(R.id.btnEditDetails);

        firestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String uid = currentUser.getUid();

        firestore.collection("users").document(uid)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        Boolean isVerified = document.getBoolean("isVerified");
                        if (isVerified != null && isVerified) {
                            Intent mainIntent = new Intent(this, MainActivity.class);
                            startActivity(mainIntent);
                            finish();
                            return;
                        }

                        // Aadhaar
                        String aadhaarRaw = document.getString("aadhaarNumber");
                        String aadhaarMasked = "Not available";
                        if (aadhaarRaw != null && aadhaarRaw.length() == 12) {
                            String display = "Aadhaar: XXXX-XXXX-" + aadhaarRaw.substring(8);
                            SpannableString span = new SpannableString(display);
                            span.setSpan(new ForegroundColorSpan(Color.parseColor("#4CAF50")),
                                    display.indexOf("XXXX-XXXX-"),
                                    display.length(),
                                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            textAadhaar.setText(span);
                        } else {
                            textAadhaar.setText("Aadhaar: Not available");
                        }

                        // PAN
                        String panRaw = document.getString("panNumber");
                        if (panRaw != null && panRaw.length() == 10) {
                            String display = "PAN: XXXXX" + panRaw.substring(5).toUpperCase();
                            SpannableString span = new SpannableString(display);
                            span.setSpan(new ForegroundColorSpan(Color.parseColor("#4CAF50")),
                                    display.indexOf("XXXXX"),
                                    display.length(),
                                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            textPan.setText(span);
                        } else {
                            textPan.setText("PAN: Not available");
                        }

                        // Address
                        String address = document.getString("address");
                        if (address != null && !address.isEmpty()) {
                            String display = "Address: " + address;
                            SpannableString span = new SpannableString(display);
                            span.setSpan(new ForegroundColorSpan(Color.parseColor("#4CAF50")),
                                    display.indexOf(address),
                                    display.length(),
                                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            textAddress.setText(span);
                        } else {
                            textAddress.setText("Address: Not available");
                        }

                        // Pincode
                        String pincode = document.getString("pincode");
                        if (pincode != null && !pincode.isEmpty()) {
                            String display = "Pincode: " + pincode;
                            SpannableString span = new SpannableString(display);
                            span.setSpan(new ForegroundColorSpan(Color.parseColor("#4CAF50")),
                                    display.indexOf(pincode),
                                    display.length(),
                                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            textPincode.setText(span);
                        } else {
                            textPincode.setText("Pincode: Not available");
                        }

                    } else {
                        Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error loading data: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );

        btnEditDetails.setOnClickListener(v -> {
            Intent editIntent = new Intent(PendingVerificationActivity.this, UploadDocumentsActivity.class);
            startActivity(editIntent);
            finish();
        });
    }
}
