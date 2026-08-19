package org.hiten.blogspherebackend.service;

public interface OtpService {

    void generateAndSendOtp(String email);

    boolean verifyOtp(String email, String otp);

    boolean consumeRecentlyVerifiedFlag(String email);
}