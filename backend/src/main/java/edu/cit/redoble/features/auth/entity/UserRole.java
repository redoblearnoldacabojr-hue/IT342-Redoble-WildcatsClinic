package edu.cit.redoble.features.auth.entity;

public final class UserRole {

    public static final int USER = 1;
    public static final int STAFF = 2;
    public static final int ADMIN = 3;

    private UserRole() {
    }

    public static boolean isPrivileged(int role) {
        return role >= STAFF;
    }

    public static boolean isAdmin(int role) {
        return role >= ADMIN;
    }
}