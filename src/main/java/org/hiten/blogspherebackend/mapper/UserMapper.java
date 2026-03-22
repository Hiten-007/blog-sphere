package org.hiten.blogspherebackend.mapper;

import org.hiten.blogspherebackend.dto.response.AuthorResponse;
import org.hiten.blogspherebackend.dto.response.UserResponse;
import org.hiten.blogspherebackend.entity.User;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) { return toUserResponse(user); }

    public AuthorResponse toAuthorResponse(User user) { return toAuthor(user); }

    public List<UserResponse> toResponseList(List<User> users) {
        return users.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public static UserResponse toUserResponse(User user) {
        if(user == null) {
            return null;
        }
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole())
                .emailVerified(user.getEmailVerified())
                .bio(user.getBio())
                .avatarUrl(user.getAvatarUrl())
                .createdAt(user.getCreatedAt())
                .build();
    }

    public static AuthorResponse toAuthor(User user) {
        if(user == null) {
            return null;
        }
        return AuthorResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }
}
