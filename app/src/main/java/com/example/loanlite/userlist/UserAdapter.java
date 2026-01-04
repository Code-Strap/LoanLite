package com.example.loanlite.userlist;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.loanlite.R;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private final Context context;
    private final List<UserModel> userList;
    private final OnActionClickListener listener;

    public interface OnActionClickListener {
        void onApproveClicked(UserModel user);
        void onRejectClicked(UserModel user);
        void onViewDetails(UserModel user);
    }

    public UserAdapter(Context context, List<UserModel> userList, OnActionClickListener listener) {
        this.context = context;
        this.userList = userList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user_row, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        UserModel user = userList.get(position);

        holder.userName.setText("👤 " + user.getFullName());
        holder.userEmail.setText("📧 " + user.getEmail());
        holder.userUid.setText("🆔 UID: " + user.getUid());

        // Show badge and button states
        if (user.isVerified()) {
            holder.userStatus.setText("✅ Verified");
            holder.userStatus.setBackgroundResource(R.drawable.status_verified_bg);
            holder.pendingActionsLayout.setVisibility(View.GONE);
        } else if (user.isRejected()) {
            holder.userStatus.setText("❌ Rejected");
            holder.userStatus.setBackgroundResource(R.drawable.status_rejected_bg);
            holder.pendingActionsLayout.setVisibility(View.GONE);
        } else {
            holder.userStatus.setText("⏳ Pending");
            holder.userStatus.setBackgroundResource(R.drawable.status_pending_bg);
            holder.pendingActionsLayout.setVisibility(View.VISIBLE);
        }

        holder.btnViewDetails.setOnClickListener(v -> listener.onViewDetails(user));
        holder.btnApprove.setOnClickListener(v -> listener.onApproveClicked(user));
        holder.btnReject.setOnClickListener(v -> listener.onRejectClicked(user));
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView userName, userEmail, userUid, userStatus;
        Button btnViewDetails, btnApprove, btnReject;
        LinearLayout pendingActionsLayout;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.userName);
            userEmail = itemView.findViewById(R.id.userEmail);
            userUid = itemView.findViewById(R.id.userUid);
            userStatus = itemView.findViewById(R.id.userStatus);
            btnViewDetails = itemView.findViewById(R.id.btnViewDetails);
            btnApprove = itemView.findViewById(R.id.btnApprove);
            btnReject = itemView.findViewById(R.id.btnReject);
            pendingActionsLayout = itemView.findViewById(R.id.pendingActionsLayout);
        }
    }
}
