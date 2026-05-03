package org.hiten.blogspherebackend.service;

public interface PasswordResetService {

    // Generate a secure random token, store it in Redis, send email
    void initiatePasswordReset(String email);

    // Validate the token: if valid, update password and invalidate token
    void confirmPasswordReset(String token, String newPassword);
}