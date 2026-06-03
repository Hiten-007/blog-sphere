package org.hiten.blogspherebackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LikeResponse {

    private Long postId;
    private long likeCount;
    private boolean likedByCurrentUser;
}