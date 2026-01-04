package com.example.loanlite.userlist;

public class UserModel {
    private String fullName;
    private String email;
    private String uid;
    private boolean isVerified;
    private boolean isRejected;

    public UserModel() {
        // Required empty constructor for Firebase
    }

    public UserModel(String fullName, String email, String uid, boolean isVerified, boolean isRejected) {
        this.fullName = fullName;
        this.email = email;
        this.uid = uid;
        this.isVerified = isVerified;
        this.isRejected = isRejected;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getUid() {
        return uid;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public boolean isRejected() {
        return isRejected;
    }
}
