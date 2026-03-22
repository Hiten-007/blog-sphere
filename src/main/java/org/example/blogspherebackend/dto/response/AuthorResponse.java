package org.example.blogspherebackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.blogspherebackend.entity.User;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorResponse {

        private Long id;
        private String name;
        private String avatarUrl;

        public static AuthorResponse fromEntity(User user) {
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
