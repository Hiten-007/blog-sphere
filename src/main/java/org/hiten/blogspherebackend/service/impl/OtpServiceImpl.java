package org.hiten.blogspherebackend.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hiten.blogspherebackend.entity.OtpVerification;
import org.hiten.blogspherebackend.repository.OtpVerificationRepository;
import org.hiten.blogspherebackend.service.EmailService;
import org.hiten.blogspherebackend.service.OtpService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

    private static final String OTP_KEY_PREFIX = "otp::";
    private static final String EMAIL_VERIFIED_KEY_PREFIX = "otp:email-verified::";
    private static final Duration OTP_TTL = Duration.ofMinutes(15);
    private static final Duration EMAIL_VERIFIED_TTL = Duration.ofMinutes(30);
    private static final int OTP_LENGTH = 6;

    private final StringRedisTemplate redisTemplate;
    private final OtpVerificationRepository otpVerificationRepository;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public void generateAndSendOtp(String email) {
        String otp = String.format("%0" + OTP_LENGTH + "d", secureRandom.nextInt((int) Math.pow(10, OTP_LENGTH)));

        LocalDateTime expiresAt = LocalDateTime.now().plus(OTP_TTL);

        try {
            redisTemplate.opsForValue().set(OTP_KEY_PREFIX + email, otp, OTP_TTL);
            log.info("[OTP] Stored OTP in Redis for email={} ttl={}min", email, OTP_TTL.toMinutes());
        } catch (Exception e) {
            log.warn("[OTP] Redis write failed for email={}, DB backup will be used. error={}",
                    email, e.getMessage());
        }

        otpVerificationRepository.deleteExpiredAndUsed(LocalDateTime.now());

        OtpVerification record = OtpVerification.builder()
                .email(email)
                .otp(otp)
                .expiresAt(expiresAt)
                .build();

        otpVerificationRepository.save(record);
        log.info("[OTP] Stored OTP in DB backup for email={} expiresAt={}", email, expiresAt);

        emailService.sendOtpEmail(email, otp);
    }

    @Override
    @Transactional
    public boolean verifyOtp(String email, String otp) {
        try {
            String storedOtp = redisTemplate.opsForValue().get(OTP_KEY_PREFIX + email);

            if (storedOtp != null) {
                if (!storedOtp.equals(otp)) {
                    log.warn("[OTP] Mismatch (Redis path) for email={}", email);
                    return false;
                }

                redisTemplate.delete(OTP_KEY_PREFIX + email);
                markDbOtpUsed(email, otp);
                markEmailRecentlyVerified(email);
                log.info("[OTP] Verified via Redis and invalidated for email={}", email);
                return true;
            }

            log.warn("[OTP] Redis miss for email={} — falling back to DB", email);

        } catch (Exception e) {
            log.warn("[OTP] Redis read failed for email={}, falling back to DB. error={}",
                    email, e.getMessage());
        }

        return verifyOtpFromDb(email, otp);
    }

    private boolean verifyOtpFromDb(String email, String otp) {
        OtpVerification record = otpVerificationRepository
                .findLatestValidOtp(email, LocalDateTime.now())
                .orElse(null);

        if (record == null) {
            log.warn("[OTP] No valid OTP in DB for email={} (expired or never generated)", email);
            return false;
        }

        if (!record.getOtp().equals(otp)) {
            log.warn("[OTP] Mismatch (DB fallback path) for email={}", email);
            return false;
        }

        record.setUsed(true);
        otpVerificationRepository.save(record);
        markEmailRecentlyVerified(email);
        log.info("[OTP] Verified via DB fallback and marked used for email={}", email);
        return true;
    }

    private void markEmailRecentlyVerified(String email) {
        try {
            redisTemplate.opsForValue().set(EMAIL_VERIFIED_KEY_PREFIX + email, "true", EMAIL_VERIFIED_TTL);
        } catch (Exception e) {
            log.warn("[OTP] Could not persist email-verified flag for email={}, error={}", email, e.getMessage());
        }
    }

    @Override
    public boolean consumeRecentlyVerifiedFlag(String email) {
        try {
            Boolean deleted = redisTemplate.delete(EMAIL_VERIFIED_KEY_PREFIX + email);
            return Boolean.TRUE.equals(deleted);
        } catch (Exception e) {
            log.warn("[OTP] Could not read email-verified flag for email={}, error={}", email, e.getMessage());
            return false;
        }
    }

    private void markDbOtpUsed(String email, String otp) {
        otpVerificationRepository
                .findLatestValidOtp(email, LocalDateTime.now())
                .filter(r -> r.getOtp().equals(otp))
                .ifPresent(r -> {
                    r.setUsed(true);
                    otpVerificationRepository.save(r);
                });
    }
}