package org.hiten.blogspherebackend.service;

public interface DigestService {

    // Send weekly digest emails to all active users
    void sendWeeklyDigestEmails();
}
