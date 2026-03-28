package org.hiten.blogspherebackend.security;

import lombok.RequiredArgsConstructor;
import org.hiten.blogspherebackend.entity.User;
import org.hiten.blogspherebackend.repository.UserRepository;
import org.hiten.blogspherebackend.service.UserService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Custom UserDsetailsService implementation for Spring Security.
 * Loads user details from database for authentication.
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserService userService;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userService.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }

    /**
     * Load user details by user ID.
     * Used by JWT filter when we have user ID from token.
     *
     * @param userId the user ID
     * @return UserDetails for the user
     */
    public UserDetails loadUserById(Long userId) throws UsernameNotFoundException {
        return userService.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));
    }
}
