package org.hiten.blogspherebackend.service.impl;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hiten.blogspherebackend.entity.Comment;
import org.hiten.blogspherebackend.entity.Post;
import org.hiten.blogspherebackend.entity.User;
import org.hiten.blogspherebackend.enums.EmailType;
import org.hiten.blogspherebackend.service.EmailService;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {
    private static final String APP_BASE_URL = "http://localhost:8080";
    private static final String FROM_ADDRESS = "noreply@blog-cms.local";

    private final JavaMailSender mailSender;


    private String htmlHeader() {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Blog CMS</title>
                </head>
                <body style="margin:0;padding:0;background-color:#f1f5f9;font-family:'Segoe UI',Arial,sans-serif;">
                  <table role="presentation" cellpadding="0" cellspacing="0" width="100%"
                         style="background-color:#f1f5f9;padding:32px 0;">
                    <tr><td align="center">
                      <table role="presentation" cellpadding="0" cellspacing="0" width="600"
                             style="background-color:#ffffff;border-radius:12px;
                                    box-shadow:0 4px 24px rgba(0,0,0,0.08);overflow:hidden;">

                        <!-- HEADER -->
                        <tr>
                          <td style="background-color:#1e293b;padding:28px 40px;text-align:center;">
                            <span style="color:#ffffff;font-size:22px;font-weight:700;
                                         letter-spacing:0.5px;">Blog CMS</span>
                          </td>
                        </tr>

                        <!-- BODY OPEN -->
                        <tr><td style="padding:40px 40px 32px;">
                """;
    }

    private String htmlFooter() {
        return """
                        </td></tr>

                        <!-- FOOTER -->
                        <tr>
                          <td style="background-color:#f8fafc;padding:20px 40px;text-align:center;
                                     border-top:1px solid #e2e8f0;">
                            <p style="margin:0;font-size:12px;color:#94a3b8;">
                              You are receiving this email because you have an account on Blog CMS.<br>
                              &copy; 2026 Blog CMS. All rights reserved.
                            </p>
                          </td>
                        </tr>

                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """;
    }

    private String buildWelcomeHtml(User user) {
        String body = """
                <h2 style="margin:0 0 16px;font-size:24px;color:#1e293b;font-weight:700;">
                  Welcome to Blog CMS, %s! &#127881;
                </h2>
                <p style="margin:0 0 12px;font-size:15px;line-height:1.7;color:#475569;">
                  We're thrilled to have you here. Your account is all set up and ready to go.
                </p>
                <p style="margin:0 0 12px;font-size:15px;line-height:1.7;color:#475569;">
                  You can browse and read all published articles on the platform straight away.
                  If you've been granted the <strong style="color:#6366f1;">AUTHOR</strong> role,
                  you can also create and publish your own articles — share your ideas with
                  the world!
                </p>
                <p style="margin:0;font-size:15px;line-height:1.7;color:#475569;">
                  Happy reading, and welcome aboard.
                </p>
                %s
                """.formatted(escapeHtml(user.getName()), ctaButton("Start Exploring", APP_BASE_URL));

        return htmlHeader() + body + htmlFooter();
    }

    private String buildOtpHtml(String email, String otp) {
        String body = """
                <h2 style="margin:0 0 16px;font-size:24px;color:#1e293b;font-weight:700;">
                  Verify your email address
                </h2>
                <p style="margin:0 0 20px;font-size:15px;line-height:1.7;color:#475569;">
                  Use the one-time password below to complete your registration.
                  It expires in <strong>15 minutes</strong>.
                </p>

                <table role="presentation" cellpadding="0" cellspacing="0"
                       style="margin:0 auto 24px;">
                  <tr>
                    <td style="background-color:#f1f5f9;border:2px dashed #6366f1;
                                border-radius:12px;padding:20px 48px;text-align:center;">
                      <span style="font-size:36px;font-weight:800;letter-spacing:12px;
                                   color:#1e293b;">%s</span>
                    </td>
                  </tr>
                </table>

                <p style="margin:0;font-size:13px;line-height:1.6;color:#94a3b8;text-align:center;">
                  If you did not request this, please ignore this email. Your account is safe.
                </p>
                """.formatted(escapeHtml(otp));

        return htmlHeader() + body + htmlFooter();
    }

    private String buildPasswordResetHtml(User user, String resetToken) {
        String resetUrl = APP_BASE_URL + "/reset-password?token=" + resetToken;
        String body = """
                <h2 style="margin:0 0 16px;font-size:24px;color:#1e293b;font-weight:700;">
                  Reset your password
                </h2>
                <p style="margin:0 0 12px;font-size:15px;line-height:1.7;color:#475569;">
                  Hi %s, we received a request to reset the password for your account.
                </p>
                <p style="margin:0 0 24px;font-size:15px;line-height:1.7;color:#475569;">
                  Click the button below to choose a new password. This link is valid for
                  <strong>30 minutes</strong>.
                </p>
                %s
                <p style="margin:32px 0 0;font-size:13px;line-height:1.6;color:#94a3b8;">
                  If you did not request a password reset, you can safely ignore this email.
                  Your password will not be changed.
                </p>
                """.formatted(escapeHtml(user.getName()), ctaButton("Reset Password", resetUrl));

        return htmlHeader() + body + htmlFooter();
    }

    private String buildCommentNotificationHtml(User postAuthor, Comment comment) {
        String postUrl = APP_BASE_URL + "/posts/" + comment.getPost().getSlug();
        String body = """
                <h2 style="margin:0 0 16px;font-size:24px;color:#1e293b;font-weight:700;">
                  New comment on your post &#128172;
                </h2>
                <p style="margin:0 0 12px;font-size:15px;line-height:1.7;color:#475569;">
                  Hi %s, <strong style="color:#1e293b;">%s</strong> left a comment on your post
                  <em>&ldquo;%s&rdquo;</em>:
                </p>

                <table role="presentation" cellpadding="0" cellspacing="0" width="100%%"
                       style="margin:16px 0 24px;">
                  <tr>
                    <td style="background-color:#f8fafc;border-left:4px solid #6366f1;
                                border-radius:0 8px 8px 0;padding:16px 20px;">
                      <p style="margin:0;font-size:15px;line-height:1.7;color:#334155;
                                 font-style:italic;">%s</p>
                    </td>
                  </tr>
                </table>
                %s
                """.formatted(
                escapeHtml(postAuthor.getName()),
                escapeHtml(comment.getAuthor().getName()),
                escapeHtml(comment.getPost().getTitle()),
                escapeHtml(comment.getBody().length() > 300
                        ? comment.getBody().substring(0, 300) + "…"
                        : comment.getBody()),
                ctaButton("View Comment", postUrl)
        );

        return htmlHeader() + body + htmlFooter();
    }

    private String buildPostPublishedHtml(User author, Post post) {
        String postUrl = APP_BASE_URL + "/posts/" + post.getSlug();
        String body = """
            <h2 style="margin:0 0 16px;font-size:24px;color:#1e293b;font-weight:700;">
              Your post is live! &#127881;
            </h2>
            <p style="margin:0 0 12px;font-size:15px;line-height:1.7;color:#475569;">
              Hi %s, your scheduled post has just been published:
            </p>
            <table role="presentation" cellpadding="0" cellspacing="0" width="100%%"
                   style="margin:0 0 24px;">
              <tr>
                <td style="background-color:#f8fafc;border-left:4px solid #6366f1;
                            border-radius:0 8px 8px 0;padding:16px 20px;">
                  <p style="margin:0;font-size:17px;font-weight:600;color:#1e293b;">%s</p>
                </td>
              </tr>
            </table>
            <p style="margin:0 0 24px;font-size:15px;line-height:1.7;color:#475569;">
              It is now live and visible to all readers. Share it with your audience!
            </p>
            %s
            """.formatted(
                escapeHtml(author.getName()),
                escapeHtml(post.getTitle()),
                ctaButton("View Your Post", postUrl)
        );

        return htmlHeader() + body + htmlFooter();
    }

    private String ctaButton(String label, String url) {
        return """
                <table role="presentation" cellpadding="0" cellspacing="0"
                       style="margin:32px auto 0;">
                  <tr>
                    <td style="background-color:#6366f1;border-radius:8px;">
                      <a href="%s"
                         style="display:inline-block;padding:14px 32px;color:#ffffff;
                                font-size:15px;font-weight:600;text-decoration:none;
                                letter-spacing:0.3px;">%s</a>
                    </td>
                  </tr>
                </table>
                """.formatted(url, label);
    }
    private String escapeHtml(String input) {
        if (input == null) return "";
        return input
                .replace("&",  "&amp;")
                .replace("<",  "&lt;")
                .replace(">",  "&gt;")
                .replace("\"", "&quot;")
                .replace("'",  "&#x27;");
    }

    // private helper

    private void doSend(String to, String subject, String htmlBody, EmailType type){
        log.info("[EMAIL] Sending type={} to={} subject=\"{}\" thread={}",
                type,
                to,
                subject,
                Thread.currentThread().getName());
        try{
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
            helper.setFrom(FROM_ADDRESS);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(mimeMessage);
            log.info("[EMAIL] Sent Successfully: type={} to={}", type, to);;
        }   catch(MessagingException e){
            log.error("[EMAIL] SMTP failure: type={} to={} error={}", type, to, e.getMessage(), e);
        } catch(Exception e){
            log.error("[EMAIL] Unexpected failure: type={} to={} error={}", type, to, e.getMessage(), e);
        }
    }

    // EmailService methods

    @Override
    @Async("taskExecutor")
    public void sendWelcomeEmail(User user) {
        try {
            log.info("[EMAIL] Preparing WELCOME email for user id={} name={}", user.getId(), user.getName());
            String html = buildWelcomeHtml(user);
            doSend(user.getEmail(), "Welcome to Daisy-Blog", html, EmailType.WELCOME);
        } catch (Exception e) {
            log.error("[EMAIL] Failed to send WELCOME email to {}: {}", user.getEmail(), e.getMessage() ,e);
        }
    }

    @Override
    @Async("taskExecutor")
    public void sendPasswordResetEmail(User user, String resetToken) {
        try {
            log.info("[EMAIL] Preparing PASSWORD_RESET email for user id={}", user.getId());
            doSend(user.getEmail(), "Reset your Blog CMS password",
                    buildPasswordResetHtml(user, resetToken), EmailType.PASSWORD_RESET);
        } catch (Exception e) {
            log.error("[EMAIL] Failed to prepare PASSWORD_RESET email for {}: {}", user.getEmail(), e.getMessage(), e);
        }
    }

    @Override
    @Async("taskExecutor")
    public void sendCommentNotification(User postAuthor, Comment comment) {
        try {
            log.info("[EMAIL] Preparing COMMENT_NOTIFICATION email for post author id={}", postAuthor.getId());
            doSend(postAuthor.getEmail(), "New comment on your post — Blog CMS",
                    buildCommentNotificationHtml(postAuthor, comment), EmailType.COMMENT_NOTIFICATION);
        } catch (Exception e) {
            log.error("[EMAIL] Failed to prepare COMMENT_NOTIFICATION email for {}: {}", postAuthor.getEmail(), e.getMessage(), e);
        }
    }

    @Override
    public void sendWeeklyDigest(User user, List<Post> popularPosts) {

    }

    @Override
    @Async("taskExecutor")
    public void sendOtpEmail(String email, String otp) {
        try {
            log.info("[EMAIL] Preparing OTP_VERIFICATION email for {}", email);
            doSend(email, "Your Blog CMS verification code", buildOtpHtml(email, otp), EmailType.OTP_VERIFICATION);
        } catch (Exception e) {
            log.error("[EMAIL] Failed to prepare OTP email for {}: {}", email, e.getMessage(), e);
        }
    }

    @Override
    @Async("taskExecutor")
    public void sendPostPublishedEmail(User author, Post post) {
        try {
            log.info("[EMAIL] Preparing POST_PUBLISHED email for user id={} postId={}", author.getId(), post.getId());
            String html = buildPostPublishedHtml(author, post);
            doSend(author.getEmail(), "Your post is now live — Blog CMS", html, EmailType.POST_PUBLISHED);
        } catch (Exception e) {
            log.error("[EMAIL] Failed to send POST_PUBLISHED email for postId={}: {}", post.getId(), e.getMessage(), e);
        }
    }
}
