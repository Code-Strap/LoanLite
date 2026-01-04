package com.example.loanlite;

import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private Button applyButton, btnRetry;
    private TextView loanStatusText, txtDueDateText, txtUserName, txtLoanUpto, txtDaysLeft;
    private CardView cardActiveLoan, cardLoanHistory, bottomNavBar;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressDaysLeft;
    private View noInternetView;
    private ImageView openUserProfile;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private String userId;
    private String loanId = "";

    private double expectedRepaymentAmount = 0.0;
    private String dueDate = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ✅ Initialize Views with correct IDs
        applyButton = findViewById(R.id.btnApplyLoan);
        loanStatusText = findViewById(R.id.txtLoanOfferText);
        txtDueDateText = findViewById(R.id.txtDueDateText);
        txtUserName = findViewById(R.id.txtUserName);
        txtLoanUpto = findViewById(R.id.txtLoanUpto);
        txtDaysLeft = findViewById(R.id.txtDaysLeft);
        cardActiveLoan = findViewById(R.id.cardActiveLoan);
        cardLoanHistory = findViewById(R.id.cardLoanHistory);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        progressDaysLeft = findViewById(R.id.progressDaysLeft);
        bottomNavBar = findViewById(R.id.bottomNavBar);
        openUserProfile = findViewById(R.id.imgProfileNav);
        noInternetView = findViewById(R.id.noInternetView);
        btnRetry = findViewById(R.id.btnRetry);

        // ✅ Retry button
        btnRetry.setOnClickListener(v -> {
            if (isInternetAvailable()) {
                noInternetView.setVisibility(View.GONE);
                swipeRefreshLayout.setVisibility(View.VISIBLE);
                checkLoanStatusAndUpdateUI();
            } else {
                Toast.makeText(MainActivity.this, "Still no internet connection", Toast.LENGTH_SHORT).show();
            }
        });

        // ✅ Firebase Init
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (auth.getCurrentUser() != null) {
            userId = auth.getCurrentUser().getUid();
            loadUserName();
            checkLoanStatusAndUpdateUI();
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
        }

        // ✅ Swipe Refresh
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadUserName();
            checkLoanStatusAndUpdateUI();
        });

        // ✅ Card Clicks
        cardActiveLoan.setOnClickListener(v -> openActiveLoan());
        cardLoanHistory.setOnClickListener(v -> openLoanHistory());
        openUserProfile.setOnClickListener(v -> openUserProfile());
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkLoanStatusAndUpdateUI();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 100 && resultCode == RESULT_OK) {
            boolean loanApplied = data != null && data.getBooleanExtra("loanApplied", false);
            if (loanApplied) {
                swipeRefreshLayout.setRefreshing(true);

                // ✅ Delay 1s before refreshing
                new Handler().postDelayed(this::checkLoanStatusAndUpdateUI, 1000);
            }
        }
    }

    private void loadUserName() {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String name = documentSnapshot.getString("fullName");
                    txtUserName.setText(name != null ? " " + name : "Welcome!");
                })
                .addOnFailureListener(e -> txtUserName.setText("Welcome!"));
    }

    private void checkLoanStatusAndUpdateUI() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            swipeRefreshLayout.setRefreshing(false);
            return;
        }

        if (!isInternetAvailable()) {
            swipeRefreshLayout.setRefreshing(false);
            noInternetView.setVisibility(View.VISIBLE);
            swipeRefreshLayout.setVisibility(View.GONE);
            bottomNavBar.setVisibility(View.GONE);
            return;
        } else {
            noInternetView.setVisibility(View.GONE);
            swipeRefreshLayout.setVisibility(View.VISIBLE);
            bottomNavBar.setVisibility(View.VISIBLE);
        }

        db.collection("loans")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot latestLoanDoc = null;
                        long latestTime = -1;

                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            Long appliedAt = doc.getLong("appliedAt");
                            if (appliedAt != null && appliedAt > latestTime) {
                                latestTime = appliedAt;
                                latestLoanDoc = doc;
                            }
                        }

                        if (latestLoanDoc != null) {
                            processLoanDocument(latestLoanDoc);
                        } else {
                            showApplyNowState();
                        }
                    } else {
                        showApplyNowState();
                    }
                })
                .addOnFailureListener(e -> {
                    swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(MainActivity.this, "Error checking loan status", Toast.LENGTH_SHORT).show();
                });
    }

    private void processLoanDocument(DocumentSnapshot loanDoc) {
        loanId = loanDoc.getId();
        String loanStatus = loanDoc.getString("loanStatus");

        // ✅ Agar overdueAmount hai to use karo, warna totalRepayment
        if (loanDoc.contains("overdueAmount")) {
            expectedRepaymentAmount = loanDoc.getDouble("overdueAmount");
        } else if (loanDoc.contains("totalRepayment")) {
            expectedRepaymentAmount = loanDoc.getDouble("totalRepayment");
        } else {
            expectedRepaymentAmount = 0;
        }

        dueDate = loanDoc.getString("dueDate");

        if ("active".equalsIgnoreCase(loanStatus)) {
            showPayNowState();
        } else {
            showAppliedState(loanStatus);
        }

        swipeRefreshLayout.setRefreshing(false);
    }

    private void showApplyNowState() {
        applyButton.setText("Apply Now");
        applyButton.setBackgroundColor(getResources().getColor(R.color.purple_500));
        applyButton.setEnabled(true);

        applyButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ApplyLoanActivity.class);
            startActivityForResult(intent, 100);
        });

        loanStatusText.setText("You're eligible for a loan offer!");
        txtDueDateText.setText("Due Date: N/A");
        txtLoanUpto.setText("");
        txtDaysLeft.setText("");
        progressDaysLeft.setVisibility(View.GONE);

        swipeRefreshLayout.setRefreshing(false);
    }

    private void showAppliedState(String loanStatus) {
        applyButton.setText("Applied");
        applyButton.setBackgroundColor(Color.GRAY);
        applyButton.setEnabled(false);

        loanStatusText.setText("Loan Status: " + (loanStatus != null ? loanStatus : "Pending"));
        txtDueDateText.setText("Due Date: Awaiting approval");
        txtLoanUpto.setText("");
        txtDaysLeft.setText("");
        progressDaysLeft.setVisibility(View.GONE);
    }

    private void showPayNowState() {
        applyButton.setText("Pay Now");
        applyButton.setBackgroundColor(Color.parseColor("#4CAF50"));
        applyButton.setEnabled(true);
        applyButton.setOnClickListener(v -> openPaymentActivity());

        loanStatusText.setText("Active Loan Credit");
        txtDueDateText.setText("Due Date: " + dueDate);

        int daysLeft = getDaysLeft(dueDate);
        progressDaysLeft.setMax(30);

        if (daysLeft < 0) {
            txtDaysLeft.setText("Overdue: " + Math.abs(daysLeft));
            txtDaysLeft.setTextColor(Color.parseColor("#D32F2F"));
            progressDaysLeft.setProgress(progressDaysLeft.getMax());

            // ✅ Overdue calculate karo
            db.collection("loans").document(loanId).get().addOnSuccessListener(doc -> {
                String paymentStatus = doc.getString("paymentStatus");

                if ("pending".equalsIgnoreCase(paymentStatus)) {
                    int daysOverdue = Math.abs(daysLeft);
                    double displayAmount = doc.contains("overdueAmount")
                            ? doc.getDouble("overdueAmount")
                            : expectedRepaymentAmount;

                    // 🔁 Agar overdueAmount nahi tha to calculate karke save karo
                    if (!doc.contains("overdueAmount")) {
                        for (int i = 0; i < daysOverdue; i++) {
                            displayAmount += (displayAmount * 0.02);
                        }
                        displayAmount = Math.round(displayAmount * 100.0) / 100.0;

                        Map<String, Object> update = new HashMap<>();
                        update.put("overdueAmount", displayAmount);
                        db.collection("loans").document(loanId).update(update);
                    }

                    // ✅ Local + UI update
                    expectedRepaymentAmount = displayAmount;
                    txtLoanUpto.setText("₹ " + displayAmount + " (Overdue)");
                    txtLoanUpto.setTextColor(Color.parseColor("#D32F2F"));
                } else {
                    txtLoanUpto.setText("₹ " + expectedRepaymentAmount);
                    txtLoanUpto.setTextColor(Color.parseColor("#2196F3"));
                }
            });

        } else {
            txtDaysLeft.setText("Days Left: " + daysLeft);

            if (daysLeft < 5) {
                txtDaysLeft.setTextColor(Color.parseColor("#D32F2F"));
            } else if (daysLeft <= 10) {
                txtDaysLeft.setTextColor(Color.parseColor("#F57C00"));
            } else {
                txtDaysLeft.setTextColor(Color.parseColor("#388E3C"));
            }

            progressDaysLeft.setProgress(30 - daysLeft);

            // ✅ Normal case me directly
            txtLoanUpto.setText("₹ " + expectedRepaymentAmount);
            txtLoanUpto.setTextColor(Color.parseColor("#2196F3"));
        }

        progressDaysLeft.setVisibility(View.VISIBLE);
    }

    private int getDaysLeft(String dueDateStr) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        try {
            Date dueDate = sdf.parse(dueDateStr);
            Date today = new Date();
            long diff = dueDate.getTime() - today.getTime();
            return (int) (diff / (1000 * 60 * 60 * 24));
        } catch (ParseException e) {
            e.printStackTrace();
            return 0;
        }
    }

    private void openPaymentActivity() {
        Intent intent = new Intent(MainActivity.this, PaymentActivity.class);
        intent.putExtra("loanId", loanId);
        intent.putExtra("amount", expectedRepaymentAmount); // ✅ ab updated overdue amount jayega
        startActivity(intent);
    }

    private void openActiveLoan() {
        Intent intent = new Intent(MainActivity.this, ActiveLoanActivity.class);
        intent.putExtra("loanId", loanId);
        intent.putExtra("dueDate", dueDate);
        intent.putExtra("amount", expectedRepaymentAmount);
        startActivity(intent);
    }

    private void openLoanHistory() {
        Intent intent = new Intent(MainActivity.this, LoanHistoryActivity.class);
        intent.putExtra("userId", userId);
        startActivity(intent);
    }

    private void openUserProfile() {
        Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
        startActivity(intent);
    }

    private boolean isInternetAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo netInfo = cm.getActiveNetworkInfo();
            return netInfo != null && netInfo.isConnected();
        }
        return false;
    }
}
