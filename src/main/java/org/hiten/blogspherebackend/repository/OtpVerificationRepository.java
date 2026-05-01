package org.hiten.blogspherebackend.repository;

import org.hiten.blogspherebackend.entity.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

    @Query("SELECT o FROM OtpVerification o " +
            "WHERE o.email = :email " +
            "AND o.used = false " +
            "AND o.expiresAt > :now " +
            "ORDER BY o.createdAt DESC")
    Optional<OtpVerification> findLatestValidOtp(@Param("email") String email, @Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM OtpVerification o WHERE o.expiresAt < :cutoff OR o.used = true")
    int deleteExpiredAndUsed(@Param("cutoff") LocalDateTime cutoff);
}

