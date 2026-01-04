package com.example.loanlite;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ApplyLoanActivity extends AppCompatActivity {

    EditText etLoanAmount, etLoanReason;
    TextView txtSummaryInterest, txtSummaryFee, txtSummaryDisbursal, txtSummaryTotal;
    CheckBox checkboxAgree;
    Button btnSubmitLoan;

    FirebaseAuth mAuth;
    FirebaseFirestore db;

    long allowedLoanAmount = 0;
    long enteredAmount = 0;
    long interest = 0, fee = 0, disbursal = 0, totalRepayable = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_apply_loan);

        // Initialize Views
        etLoanAmount = findViewById(R.id.etLoanAmount);
        etLoanReason = findViewById(R.id.etLoanReason);
        checkboxAgree = findViewById(R.id.checkboxAgree);
        btnSubmitLoan = findViewById(R.id.btnSubmitLoan);
        txtSummaryInterest = findViewById(R.id.txtSummaryInterest);
        txtSummaryFee = findViewById(R.id.txtSummaryFee);
        txtSummaryDisbursal = findViewById(R.id.txtSummaryDisbursal);
        txtSummaryTotal = findViewById(R.id.txtSummaryTotal);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        allowedLoanAmount = getIntent().getLongExtra("allowedLoanAmount", 100);
        etLoanAmount.setHint("Enter amount up to ₹" + allowedLoanAmount);

        etLoanAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    enteredAmount = Long.parseLong(s.toString().replaceAll("[^\\d]", ""));
                    updateSummary(enteredAmount);
                } catch (Exception e) {
                    updateSummary(0);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnSubmitLoan.setOnClickListener(v -> checkAndSubmitLoan());
    }

    private void updateSummary(long amount) {
        interest = (long) (amount * 0.15);
        fee = (long) (amount * 0.05);
        disbursal = amount - (interest + fee);
        totalRepayable = amount;

        txtSummaryInterest.setText("Interest (15%): ₹" + interest);
        txtSummaryFee.setText("Processing Fee (5%): ₹" + fee);
        txtSummaryDisbursal.setText("Disbursal Amount: ₹" + disbursal);
        txtSummaryTotal.setText("Total Repayable: ₹" + totalRepayable);
    }

    private void checkAndSubmitLoan() {
        String amountStr = etLoanAmount.getText().toString().trim().replaceAll("[^\\d]", "");
        String reason = etLoanReason.getText().toString().trim();
        boolean agreed = checkboxAgree.isChecked();

        if (TextUtils.isEmpty(amountStr)) {
            Snackbar.make(btnSubmitLoan, "Please enter loan amount", Snackbar.LENGTH_SHORT).show();
            return;
        }

        long enteredAmt = Long.parseLong(amountStr);
        if (enteredAmt > allowedLoanAmount) {
            Snackbar.make(btnSubmitLoan, "You can only apply for up to ₹" + allowedLoanAmount, Snackbar.LENGTH_LONG).show();
            return;
        }

        if (TextUtils.isEmpty(reason)) {
            Snackbar.make(btnSubmitLoan, "Please enter loan reason", Snackbar.LENGTH_SHORT).show();
            return;
        }

        if (!agreed) {
            Snackbar.make(btnSubmitLoan, "Please accept Terms and Conditions", Snackbar.LENGTH_SHORT).show();
            return;
        }

        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (userId == null) {
            Snackbar.make(btnSubmitLoan, "User not logged in", Snackbar.LENGTH_SHORT).show();
            return;
        }

        // Recalculate loan summary
        updateSummary(enteredAmt);

        db.collection("loans")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    DocumentSnapshot latestLoan = null;
                    long latestTime = 0;

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Long appliedAt = doc.getLong("appliedAt");
                        if (appliedAt != null && appliedAt > latestTime) {
                            latestTime = appliedAt;
                            latestLoan = doc;
                        }
                    }

                    if (latestLoan != null) {
                        String loanStatus = latestLoan.getString("loanStatus");
                        String paymentStatus = latestLoan.getString("paymentStatus");

                        if ("approved".equalsIgnoreCase(loanStatus) &&
                                !"paid".equalsIgnoreCase(paymentStatus)) {
                            Snackbar.make(btnSubmitLoan, "You already have an active unpaid loan", Snackbar.LENGTH_LONG).show();
                            return;
                        }

                        if ("pending".equalsIgnoreCase(loanStatus)) {
                            Snackbar.make(btnSubmitLoan, "Your previous loan is under review", Snackbar.LENGTH_LONG).show();
                            return;
                        }

                        if ("rejected".equalsIgnoreCase(loanStatus)) {
                            Snackbar.make(btnSubmitLoan, "Your last loan was rejected. Please wait before re-applying.", Snackbar.LENGTH_LONG).show();
                            return;
                        }

                        if ("paid".equalsIgnoreCase(paymentStatus)) {
                            submitLoanApplication(userId, reason, enteredAmt);
                        } else {
                            Snackbar.make(btnSubmitLoan, "Cannot re-apply for loan at the moment", Snackbar.LENGTH_LONG).show();
                        }

                    } else {
                        // No previous loan found, proceed with application
                        submitLoanApplication(userId, reason, enteredAmt);
                    }
                })
                .addOnFailureListener(e -> {
                    Snackbar.make(btnSubmitLoan, "Error checking loan status", Snackbar.LENGTH_SHORT).show();
                    e.printStackTrace();
                });
    }

    private void submitLoanApplication(String userId, String reason, long finalAmount) {
        long timestamp = System.currentTimeMillis();
        String formattedDate = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(new Date(timestamp));

        Map<String, Object> loanData = new HashMap<>();
        loanData.put("userId", userId);
        loanData.put("loanAmount", finalAmount);
        loanData.put("interest", interest);
        loanData.put("processingFee", fee);
        loanData.put("expectedDisbursal", disbursal);
        loanData.put("expectedRepayment", totalRepayable);
        loanData.put("reason", reason);
        loanData.put("loanStatus", "pending");
        loanData.put("loanApplied", true);
        loanData.put("appliedAt", timestamp);
        loanData.put("appliedAtFormatted", formattedDate);
        loanData.put("finalDisbursal", 0);
        loanData.put("totalRepaid", 0);
        loanData.put("paymentStatus", "pending");
        loanData.put("dueDate", get30DaysFromNow());

        db.collection("loans")
                .add(loanData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Loan Application Submitted", Toast.LENGTH_LONG).show();
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("loanApplied", true);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error submitting loan", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                });
    }
    private String get30DaysFromNow() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, 30);
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        return sdf.format(calendar.getTime());
    }

}
