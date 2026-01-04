package com.example.loanlite;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class PaymentActivity extends AppCompatActivity {

    private TextView tvUpiId, tvAmount;
    private ImageView btnCopyUpi;
    private EditText etUtr;
    private Button btnConfirmPayment;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        // Init UI
        tvUpiId = findViewById(R.id.tvUpiId);
        tvAmount = findViewById(R.id.tvAmount);
        btnCopyUpi = findViewById(R.id.btnCopyUpi);
        etUtr = findViewById(R.id.etUtr);
        btnConfirmPayment = findViewById(R.id.btnConfirmPayment);

        // Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // UPI ID Copy
        btnCopyUpi.setOnClickListener(v -> {
            String upiId = tvUpiId.getText().toString().replace("UPI ID: ", "");
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("UPI ID", upiId);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "UPI ID copied!", Toast.LENGTH_SHORT).show();
        });

        // Confirm Payment
        btnConfirmPayment.setOnClickListener(v -> {
            String utr = etUtr.getText().toString().trim();

            if (utr.isEmpty()) {
                Toast.makeText(this, "Please enter transaction ID", Toast.LENGTH_SHORT).show();
                return;
            }

            String userId = mAuth.getCurrentUser().getUid();

            db.collection("loans")
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("loanStatus", "approved")
                    .limit(1)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            DocumentSnapshot loanDoc = querySnapshot.getDocuments().get(0);
                            String loanId = loanDoc.getId();

                            db.collection("loans").document(loanId)
                                    .update("utr", utr, "paymentStatus", "pending")
                                    .addOnSuccessListener(unused -> {
                                        Toast.makeText(this, "Transaction submitted!", Toast.LENGTH_SHORT).show();
                                        btnConfirmPayment.setEnabled(false);
                                        etUtr.setEnabled(false);

                                        showConfirmationDialog();
                                    })
                                    .addOnFailureListener(e ->
                                            Toast.makeText(this, "Failed to submit. Try again.", Toast.LENGTH_SHORT).show()
                                    );
                        } else {
                            Toast.makeText(this, "No active loan found", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Error accessing loan data", Toast.LENGTH_SHORT).show()
                    );
        });
    }

    private void showConfirmationDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_thank_you);
        dialog.setCancelable(false);

        // Optional: Make background rounded or transparent
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        Button btnOk = dialog.findViewById(R.id.btnOk);
        btnOk.setOnClickListener(v -> {
            dialog.dismiss();
            finish(); // or go back to MainActivity if needed
        });

        dialog.show();
    }
}
