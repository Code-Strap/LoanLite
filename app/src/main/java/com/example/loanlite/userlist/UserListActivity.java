package com.example.loanlite.userlist;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.loanlite.R;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.*;

public class UserListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private UserAdapter adapter;
    private List<UserModel> userList;
    private FirebaseFirestore db;
    private String filterStatus;
    private View rootView;
    private String adminName = "Admin";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_list);
        rootView = findViewById(android.R.id.content);

        recyclerView = findViewById(R.id.userRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        userList = new ArrayList<>();
        db = FirebaseFirestore.getInstance();

        adapter = new UserAdapter(this, userList, new UserAdapter.OnActionClickListener() {
            @Override
            public void onApproveClicked(UserModel user) {
                showConfirmationDialog(user, true);
            }

            @Override
            public void onRejectClicked(UserModel user) {
                showConfirmationDialog(user, false);
            }

            @Override
            public void onViewDetails(UserModel user) {
                Toast.makeText(UserListActivity.this, "Showing details of " + user.getFullName(), Toast.LENGTH_SHORT).show();
            }
        });

        recyclerView.setAdapter(adapter);

        filterStatus = getIntent().getStringExtra("filter");
        adminName = getIntent().getStringExtra("adminName");
        if (adminName == null) adminName = "Admin";
        if (filterStatus == null) filterStatus = "all";

        fetchUsersByStatus(filterStatus);
    }

    private void fetchUsersByStatus(String status) {
        db.collection("users").get().addOnCompleteListener(task -> {
            userList.clear();
            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot doc : task.getResult()) {
                    Boolean isAdmin = doc.getBoolean("isAdmin");
                    if (isAdmin != null && isAdmin) continue;

                    boolean isVerified = Boolean.TRUE.equals(doc.getBoolean("isVerified"));
                    boolean isRejected = Boolean.TRUE.equals(doc.getBoolean("isRejected"));

                    String fullName = doc.getString("fullName");
                    String email = doc.getString("email");
                    String uid = doc.getId();

                    if (status.equalsIgnoreCase("Verified") && !isVerified) continue;
                    if (status.equalsIgnoreCase("Rejected") && !isRejected) continue;
                    if (status.equalsIgnoreCase("Pending") && (isVerified || isRejected)) continue;

                    userList.add(new UserModel(fullName, email, uid, isVerified, isRejected));
                }

                adapter.notifyDataSetChanged();

                if (userList.isEmpty()) {
                    Snackbar.make(rootView, "No users found for: " + filterStatus, Snackbar.LENGTH_LONG).show();
                }
            } else {
                Snackbar.make(rootView, "❌ Failed to fetch users", Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void showConfirmationDialog(UserModel user, boolean approve) {
        String title = approve ? "✅ Approve KYC" : "❌ Reject KYC";
        String message = "Are you sure you want to mark " + user.getFullName() + " as " + (approve ? "Verified" : "Rejected") + "?";

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Yes", (dialog, which) -> updateVerification(user, approve))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateVerification(UserModel user, boolean isVerified) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isVerified", isVerified);
        updates.put("isRejected", !isVerified);

        String adminUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("users").document(user.getUid())
                .update(updates)
                .addOnSuccessListener(unused -> {
                    Snackbar.make(rootView, user.getFullName() + " marked as " + (isVerified ? "Verified" : "Rejected"), Snackbar.LENGTH_SHORT).show();
                    logAdminAction(adminUid, adminName, user, isVerified);
                    fetchUsersByStatus(filterStatus);
                })
                .addOnFailureListener(e -> {
                    Log.e("UPDATE_USER", "Error", e);
                    Snackbar.make(rootView, "❌ Failed to update: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                });
    }

    private void logAdminAction(String adminUid, String adminName, UserModel user, boolean isVerified) {
        Map<String, Object> log = new HashMap<>();
        log.put("adminUid", adminUid);
        log.put("adminName", adminName);
        log.put("kyc_action", isVerified ? "Approved" : "Rejected");
        log.put("userName", user.getFullName());
        log.put("userId", user.getUid());
        log.put("timestamp", FieldValue.serverTimestamp());

        db.collection("logs")
                .add(log)
                .addOnSuccessListener(doc -> Log.d("LOG_ACTION", "✅ Log added"))
                .addOnFailureListener(e -> Log.e("LOG_ACTION", "❌ Log failed", e));
    }
}
