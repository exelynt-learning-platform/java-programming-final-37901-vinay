package com.exam.system.model;

import java.util.Objects;

/**
 * Domain model representing a system user with role-based access.
 */
public class User {

    private int id;
    private final String username;
    private final String passwordHash;
    private final String fullName;
    private final String email;
    private final Role role;

    public User(int id, String username, String passwordHash, String fullName, String email, Role role) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
    }

    public User(String username, String passwordHash, String fullName, String email, Role role) {
        this(0, username, passwordHash, fullName, email, role);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public Role getRole() {
        return role;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public boolean isStudent() {
        return role == Role.STUDENT;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User user)) return false;
        return Objects.equals(username, user.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username);
    }

    @Override
    public String toString() {
        return String.format("User[ID=%d, User='%s', Name='%s', Role=%s]", id, username, fullName, role);
    }
}
