package org.hiten.blogspherebackend.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hiten.blogspherebackend.entity.PasswordResetToken;
import org.hiten.blogspherebackend.entity.User;
import org.hiten.blogspherebackend.exception.BadRequestException;
import org.hiten.blogspherebackend.exception.ResourceNotFoundException;
import org.hiten.blogspherebackend.repository.PasswordResetTokenRepository;
import org.hiten.blogspherebackend.repository.UserRepository;
import org.hiten.blogspherebackend.service.EmailService;
import org.hiten.blogspherebackend.service.PasswordResetService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final String TOKEN_KEY_PREFIX = "pwd-reset::";
    private static final Duration TOKEN_TTL = Duration.ofMinutes(30);

    private final StringRedisTemplate redisTemplate;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void initiatePasswordReset(String email) {
        User user = userRepository.findByEmailAndIsDeletedFalse(email).orElse(null);
        if (user == null) {
            log.warn("[PWD-RESET] Reset requested for unknown email={}", email);
            return;
        }

        String token = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plus(TOKEN_TTL);

        try {
            redisTemplate.opsForValue().set(TOKEN_KEY_PREFIX + token, email, TOKEN_TTL);
            log.info("[PWD-RESET] Token stored in Redis for user id={} ttl={}min",
                    user.getId(), TOKEN_TTL.toMinutes());
        } catch (Exception e) {
            log.warn("[PWD-RESET] Redis write failed for user id={}, DB backup will be used. error={}",
                    user.getId(), e.getMessage());
        }

        PasswordResetToken record = PasswordResetToken.builder()
                .email(email)
                .token(token)
                .expiresAt(expiresAt)
                .build();
        passwordResetTokenRepository.save(record);
        log.info("[PWD-RESET] Token stored in DB backup for user id={} expiresAt={}",
                user.getId(), expiresAt);

        emailService.sendPasswordResetEmail(user, token);
    }

    @Override
    @Transactional
    public void confirmPasswordReset(String token, String newPassword) {

        String email = null;
        boolean redisHit = false;

        try {
            email = redisTemplate.opsForValue().get(TOKEN_KEY_PREFIX + token);
            if (email != null) {
                redisHit = true;
                log.info("[PWD-RESET] Token resolved via Redis");
            } else {
                log.warn("[PWD-RESET] Redis miss for token — falling back to DB");
            }
        } catch (Exception e) {
            log.warn("[PWD-RESET] Redis read failed, falling back to DB. error={}", e.getMessage());
        }

        PasswordResetToken dbRecord = null;

        if (email == null) {
            dbRecord = passwordResetTokenRepository
                    .findValidToken(token, LocalDateTime.now())
                    .orElse(null);

            if (dbRecord == null) {
                throw new BadRequestException("Password reset token is invalid or has expired");
            }

            email = dbRecord.getEmail();
            log.info("[PWD-RESET] Token resolved via DB fallback");
        }

        String finalEmail = email;
        User user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", finalEmail));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("[PWD-RESET] Password updated for user id={}", user.getId());

        if (redisHit) {
            try {
                redisTemplate.delete(TOKEN_KEY_PREFIX + token);
            } catch (Exception e) {
                log.warn("[PWD-RESET] Failed to delete token from Redis: {}", e.getMessage());
            }
            passwordResetTokenRepository
                    .findValidToken(token, LocalDateTime.now())
                    .ifPresent(r -> {
                        r.setUsed(true);
                        passwordResetTokenRepository.save(r);
                    });
        } else {
            dbRecord.setUsed(true);
            passwordResetTokenRepository.save(dbRecord);
        }

        log.info("[PWD-RESET] Token invalidated in both stores for user id={}", user.getId());
    }
}
