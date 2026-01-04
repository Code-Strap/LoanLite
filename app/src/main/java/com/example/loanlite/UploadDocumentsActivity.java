package com.example.loanlite;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class UploadDocumentsActivity extends AppCompatActivity {

    private EditText etAadhaar, etPan, etAddress, etPincode;
    private Button btnUploadAadhaar, btnUploadPan, btnSaveDetails;
    private TextView textAadhaarStatus, textPanStatus;
    private TextView textUserName, textUserStatus, textVerifiedLabel;
    private ImageView imageUserProfile;

    private FirebaseFirestore db;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_documents);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        etAadhaar = findViewById(R.id.etAadhaar);
        etPan = findViewById(R.id.etPan);
        etAddress = findViewById(R.id.etAddress);
        etPincode = findViewById(R.id.etPincode);

        btnUploadAadhaar = findViewById(R.id.btnUploadAadhaar);
        btnUploadPan = findViewById(R.id.btnUploadPan);
        btnSaveDetails = findViewById(R.id.btnSaveDetails);

        textAadhaarStatus = findViewById(R.id.textAadhaarStatus);
        textPanStatus = findViewById(R.id.textPanStatus);

        textUserName = findViewById(R.id.textUserName);
        textUserStatus = findViewById(R.id.textUserStatus);
        textVerifiedLabel = findViewById(R.id.textVerifiedLabel);
        imageUserProfile = findViewById(R.id.imageUserProfile);

        // Set default status red
        textAadhaarStatus.setText("Status: Not uploaded");
        textAadhaarStatus.setTextColor(Color.parseColor("#D32F2F"));
        textPanStatus.setText("Status: Not uploaded");
        textPanStatus.setTextColor(Color.parseColor("#D32F2F"));

        btnUploadAadhaar.setOnClickListener(v -> {
            String aadhaar = etAadhaar.getText().toString().trim();

            if (!aadhaar.matches("\\d{12}")) {
                Toast.makeText(this, "Enter valid 12-digit Aadhaar number", Toast.LENGTH_SHORT).show();
                return;
            }

            db.collection("users")
                    .whereEqualTo("aadhaarNumber", aadhaar)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            Toast.makeText(this, "This Aadhaar is already used", Toast.LENGTH_SHORT).show();
                        } else {
                            Map<String, Object> aadhaarUpdate = new HashMap<>();
                            aadhaarUpdate.put("aadhaarNumber", aadhaar);

                            db.collection("users").document(uid).update(aadhaarUpdate)
                                    .addOnSuccessListener(unused -> {
                                        textAadhaarStatus.setText("Status: Uploaded");
                                        textAadhaarStatus.setTextColor(Color.parseColor("#388E3C"));
                                        btnUploadAadhaar.setText("Re-upload");
                                        etAadhaar.setEnabled(false);
                                        btnUploadAadhaar.setEnabled(false);
                                        Toast.makeText(this, "Aadhaar uploaded", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e ->
                                            Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        }
                    });
        });

        btnUploadPan.setOnClickListener(v -> {
            String pan = etPan.getText().toString().trim().toUpperCase();

            if (!pan.matches("[A-Z]{5}[0-9]{4}[A-Z]")) {
                Toast.makeText(this, "Enter valid PAN (e.g., ABCDE1234F)", Toast.LENGTH_SHORT).show();
                return;
            }

            db.collection("users")
                    .whereEqualTo("panNumber", pan)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            Toast.makeText(this, "This PAN is already used", Toast.LENGTH_SHORT).show();
                        } else {
                            Map<String, Object> panUpdate = new HashMap<>();
                            panUpdate.put("panNumber", pan);

                            db.collection("users").document(uid).update(panUpdate)
                                    .addOnSuccessListener(unused -> {
                                        textPanStatus.setText("Status: Uploaded");
                                        textPanStatus.setTextColor(Color.parseColor("#388E3C"));
                                        btnUploadPan.setText("Re-upload");
                                        etPan.setEnabled(false);
                                        btnUploadPan.setEnabled(false);
                                        Toast.makeText(this, "PAN uploaded", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e ->
                                            Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        }
                    });
        });

        btnSaveDetails.setOnClickListener(v -> {
            String aadhaar = etAadhaar.getText().toString().trim();
            String pan = etPan.getText().toString().trim();
            String address = etAddress.getText().toString().trim();
            String pincode = etPincode.getText().toString().trim();

            if (aadhaar.isEmpty() || pan.isEmpty() || address.isEmpty() || pincode.isEmpty()) {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!aadhaar.matches("\\d{12}")) {
                Toast.makeText(this, "Invalid Aadhaar number", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!pan.matches("[A-Z]{5}[0-9]{4}[A-Z]")) {
                Toast.makeText(this, "Invalid PAN format", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!pincode.matches("\\d{6}")) {
                Toast.makeText(this, "Invalid Pincode", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("aadhaarNumber", aadhaar);
            updates.put("panNumber", pan);
            updates.put("address", address);
            updates.put("pincode", pincode);
            updates.put("isVerified", false);

            db.collection("users").document(uid)
                    .update(updates)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "Details saved. Verification pending.", Toast.LENGTH_LONG).show();

                        etAadhaar.setEnabled(false);
                        etPan.setEnabled(false);
                        etAddress.setEnabled(false);
                        etPincode.setEnabled(false);
                        btnUploadAadhaar.setEnabled(false);
                        btnUploadPan.setEnabled(false);
                        btnSaveDetails.setEnabled(false);

                        textUserStatus.setText("Status: Pending");

                        startActivity(new Intent(this, PendingVerificationActivity.class));
                        finish();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to save: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });
    }
}
