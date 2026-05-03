package org.hiten.blogspherebackend.service;

import org.hiten.blogspherebackend.entity.Comment;
import org.hiten.blogspherebackend.entity.Post;
import org.hiten.blogspherebackend.entity.User;

import java.util.List;

public interface EmailService {

        // Send a welcome email to a newly registered user
        void sendWelcomeEmail(User user);

        // Send password-reset link to the user
        void sendPasswordResetEmail(User user, String resetToken);

        // Notify a post author that someone lft a comment on their post
        void sendCommentNotification(User postAuthor, Comment comment);

        // Send a weekly digest containing the most popular posts
        void sendWeeklyDigest(User user, List<Post> popularPosts);

        // Send OTP for email verification during registration
        void sendOtpEmail(String email, String otp);
}
