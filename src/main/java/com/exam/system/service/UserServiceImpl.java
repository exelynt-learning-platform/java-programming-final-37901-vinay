package com.exam.system.service;

import com.exam.system.config.DatabaseConnectionManager;
import com.exam.system.exception.AuthenticationException;
import com.exam.system.model.Role;
import com.exam.system.model.User;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Implementation of UserService with JDBC database interaction and SHA-256 password hashing.
 */
public class UserServiceImpl implements UserService {

    private static final Logger logger = Logger.getLogger(UserServiceImpl.class.getName());
    private final DatabaseConnectionManager dbManager = DatabaseConnectionManager.getInstance();

    @Override
    public User registerUser(String username, String rawPassword, String fullName, String email, Role role) {
        if (username == null || rawPassword == null || role == null) {
            throw new AuthenticationException("Registration fields cannot be null.");
        }

        String passwordHash = hashPassword(rawPassword);
        String sql = "INSERT INTO users (username, password_hash, full_name, email, role) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, username.trim());
            ps.setString(2, passwordHash);
            ps.setString(3, fullName.trim());
            ps.setString(4, email.trim());
            ps.setString(5, role.name());

            ps.executeUpdate();

            int generatedId = 0;
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    generatedId = rs.getInt(1);
                }
            }

            User user = new User(generatedId, username, passwordHash, fullName, email, role);
            logger.info("Registered user: " + username + " with role: " + role);
            return user;

        } catch (SQLException e) {
            logger.warning("Failed to register user " + username + ": " + e.getMessage());
            throw new AuthenticationException("Registration failed (username/email may already exist): " + e.getMessage());
        }
    }

    @Override
    public User authenticate(String username, String rawPassword) {
        if (username == null || rawPassword == null) {
            throw new AuthenticationException("Username and password must not be null.");
        }

        String inputHash = hashPassword(rawPassword);
        String sql = "SELECT id, username, password_hash, full_name, email, role FROM users WHERE username = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");
                    if (storedHash.equals(inputHash)) {
                        User user = new User(
                                rs.getInt("id"),
                                rs.getString("username"),
                                storedHash,
                                rs.getString("full_name"),
                                rs.getString("email"),
                                Role.valueOf(rs.getString("role"))
                        );
                        logger.info("User authenticated successfully: " + username + " [" + user.getRole() + "]");
                        return user;
                    }
                }
            }
        } catch (SQLException e) {
            logger.severe("Database error during authentication: " + e.getMessage());
            throw new AuthenticationException("Database error during login: " + e.getMessage());
        }

        throw new AuthenticationException("Invalid username or password for user: " + username);
    }

    @Override
    public Optional<User> getUserById(int id) {
        String sql = "SELECT id, username, password_hash, full_name, email, role FROM users WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new User(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("password_hash"),
                            rs.getString("full_name"),
                            rs.getString("email"),
                            Role.valueOf(rs.getString("role"))
                    ));
                }
            }
        } catch (SQLException e) {
            logger.warning("Error fetching user by ID " + id + ": " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public void validateAdminAccess(User user) {
        if (user == null || !user.isAdmin()) {
            throw new AuthenticationException("Access Denied: Administrative privileges required.");
        }
    }

    @Override
    public void validateStudentAccess(User user) {
        if (user == null || !user.isStudent()) {
            throw new AuthenticationException("Access Denied: Student account required.");
        }
    }

    private String hashPassword(String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(rawPassword.hashCode());
        }
    }
}
