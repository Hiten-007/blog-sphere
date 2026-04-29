package org.hiten.blogspherebackend.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hiten.blogspherebackend.dto.request.EmailRequest;
import org.hiten.blogspherebackend.entity.Comment;
import org.hiten.blogspherebackend.entity.Post;
import org.hiten.blogspherebackend.entity.User;
import org.hiten.blogspherebackend.enums.EmailType;
import org.hiten.blogspherebackend.service.EmailService;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {
    private static final String FROM_ADDRESS = "noreply@blog-cms.local";

    private final JavaMailSender mailSender;

//     private helper

    private void doSend(EmailRequest request) {
        log.info("[EMAIL] Sending type={} to={} subject=\"{}\" thread={}",
                request.getType(),
                request.getTo(),
                request.getSubject(),
                Thread.currentThread().getName());

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(FROM_ADDRESS);
        message.setTo(request.getTo());
        message.setSubject(request.getSubject());
        message.setText(request.getBody());

        mailSender.send(message);

        log.info("[EMAIL] Sent Successfully: type={} to={}", request.getType(), request.getTo());
    }

    // EmailService methods


    @Override
    @Async("taskExecutor")
    public void sendWelcomeEmail(User user) {
        try {
            String body = "Hi " + user.getName() + ".\n\n"
                    + "Welcome to Blog CMS! Your account has been created successfully.\n\n"
                    + "You can now log in, explore published posts, and - if you have the AUTHOR role - "
                    + "start writing your own articles.\n\n"
                    + "Happy reading!\n"
                    + "The Blog CMS Team";

            EmailRequest request = EmailRequest.builder()
                    .to(user.getEmail())
                    .subject("Welcome to Blog CMS!")
                    .body(body)
                    .type(EmailType.WELCOME)
                    .build();

            log.info("[EMAIL] Preparing WELCOME email for user id={} name={}", user.getId(), user.getName());
            doSend(request);
        } catch (Exception e) {
            log.error("[EMAIL] Failed to send WELCOME email to {}: {}", user.getEmail(), e.getMessage() ,e);
        }
    }

    @Override
    @Async("taskExecutor")
    public void sendPasswordResetEmail(User user, String resetToken) {

    }

    @Override
    public void sendCommentNotification(User postAuthor, Comment comment) {

    }

    @Override
    public void sendWeeklyDigest(User user, List<Post> popularPosts) {

    }
}
