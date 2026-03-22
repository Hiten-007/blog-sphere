package org.hiten.blogspherebackend.repository;

import org.hiten.blogspherebackend.entity.User;
import org.hiten.blogspherebackend.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailAndIsDeletedFalse(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIsDeletedFalse(String email);

    List<User> findByRole(Role role);

    List<User> findByRoleAndIsDeletedFalse(Role role);

    List<User> findByIsDeletedFalse();

    long countByIsDeletedFalse();

    long countByRole(Role role);

    List<User> findByEmailVerifiedFalseAndIsDeletedFalse();

    @Query("SELECT u FROM User u WHERE u.isDeleted = false " +
            "AND (LOWER(u.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
            "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<User> searchActiveUsers(@Param("searchTerm") String searchTerm);
}