package com.exam.system.service;

import com.exam.system.model.Role;
import com.exam.system.model.User;

import java.util.Optional;

/**
 * Service contract for User Authentication, Registration, and Role Management.
 */
public interface UserService {

    User registerUser(String username, String rawPassword, String fullName, String email, Role role);

    User authenticate(String username, String rawPassword);

    Optional<User> getUserById(int id);

    void validateAdminAccess(User user);

    void validateStudentAccess(User user);
}
