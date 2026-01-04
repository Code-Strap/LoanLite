package com.example.loanlite;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.loanlite.userlist.UserListActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class AdminDashboardActivity extends AppCompatActivity {

    private TextView txtAdminName;
    private TextView totalUsersText, verifiedUsersText, pendingKycText, rejectedUsersText;
    private TextView totalLoanGivenText, recoveredAmountText;
    private SwipeRefreshLayout swipeRefreshLayout;

    private FirebaseFirestore db;
    private String adminName = "Admin";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        db = FirebaseFirestore.getInstance();

        // Initialize views
        txtAdminName = findViewById(R.id.txtAdmin_name);
        totalUsersText = findViewById(R.id.totalUsers);
        verifiedUsersText = findViewById(R.id.verifiedUsers);
        pendingKycText = findViewById(R.id.pendingKyc);
        rejectedUsersText = findViewById(R.id.rejectedUsers);
        totalLoanGivenText = findViewById(R.id.totalLoanGiven);
        recoveredAmountText = findViewById(R.id.recoveredAmount);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        loadAdminName();
        setupUserCardClicks();
        loadUserSummaryCounts();

        swipeRefreshLayout.setOnRefreshListener(this::loadUserSummaryCounts);
    }

    private void loadAdminName() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        String name = snapshot.getString("fullName");
                        adminName = name != null ? name : "Admin";
                        txtAdminName.setText(adminName);
                    } else {
                        txtAdminName.setText("Admin");
                    }
                })
                .addOnFailureListener(e -> {
                    txtAdminName.setText("Admin");
                    Log.e("ADMIN_NAME", "Failed to fetch admin name", e);
                });
    }

    private void setupUserCardClicks() {
        findViewById(R.id.cardTotalUsers).setOnClickListener(v -> openUserList("all"));
        findViewById(R.id.cardVerifiedUsers).setOnClickListener(v -> openUserList("Verified"));
        findViewById(R.id.cardPendingKyc).setOnClickListener(v -> openUserList("Pending"));
        findViewById(R.id.cardRejectedUsers).setOnClickListener(v -> openUserList("Rejected"));
    }

    private void openUserList(String filter) {
        Intent intent = new Intent(this, UserListActivity.class);
        intent.putExtra("filter", filter);
        intent.putExtra("adminName", adminName);
        startActivity(intent);
    }

    private void loadUserSummaryCounts() {
        swipeRefreshLayout.setRefreshing(true);

        db.collection("users").get().addOnSuccessListener(querySnapshot -> {
            int total = 0, verified = 0, pending = 0, rejected = 0;

            for (QueryDocumentSnapshot doc : querySnapshot) {
                Boolean isAdmin = doc.getBoolean("isAdmin");
                if (isAdmin != null && isAdmin) continue;

                total++;
                boolean isVerified = Boolean.TRUE.equals(doc.getBoolean("isVerified"));
                boolean isRejected = Boolean.TRUE.equals(doc.getBoolean("isRejected"));

                if (isVerified) verified++;
                else if (isRejected) rejected++;
                else pending++;
            }

            totalUsersText.setText(String.valueOf(total));
            verifiedUsersText.setText(String.valueOf(verified));
            pendingKycText.setText(String.valueOf(pending));
            rejectedUsersText.setText(String.valueOf(rejected));

            loadLoanSummary();
        }).addOnFailureListener(e -> {
            swipeRefreshLayout.setRefreshing(false);
            Log.e("DASHBOARD", "Error loading user summary", e);
        });
    }

    private void loadLoanSummary() {
        db.collection("loans").get().addOnSuccessListener(querySnapshot -> {
            double totalDisbursed = 0;
            double totalRecovered = 0;

            for (QueryDocumentSnapshot doc : querySnapshot) {
                Object disbursedObj = doc.get("finalDisbursal"); // Rename if your Firestore key is different
                Object repaidObj = doc.get("totalRepaid");       // Rename if your Firestore key is different

                if (disbursedObj instanceof Number) {
                    totalDisbursed += ((Number) disbursedObj).doubleValue();
                }

                if (repaidObj instanceof Number) {
                    totalRecovered += ((Number) repaidObj).doubleValue();
                }

                Log.d("LOAN_SUMMARY", "Doc: " + doc.getId() + " → Disbursed=" + disbursedObj + ", Repaid=" + repaidObj);
            }

            totalLoanGivenText.setText("₹" + formatCurrency(totalDisbursed));
            recoveredAmountText.setText("₹" + formatCurrency(totalRecovered));

            swipeRefreshLayout.setRefreshing(false);
        }).addOnFailureListener(e -> {
            swipeRefreshLayout.setRefreshing(false);
            Log.e("DASHBOARD", "Error loading loan summary", e);
        });
    }

    private String formatCurrency(double amount) {
        return String.format("%,d", (int) amount);
    }
}
