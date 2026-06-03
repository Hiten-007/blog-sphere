CREATE TABLE post_likes (
                            id         BIGSERIAL PRIMARY KEY,
                            post_id    BIGINT      NOT NULL REFERENCES posts(id)  ON DELETE CASCADE,
                            user_id    BIGINT      NOT NULL REFERENCES users(id)  ON DELETE CASCADE,
                            liked_at   TIMESTAMP   NOT NULL DEFAULT NOW(),
                            CONSTRAINT uq_post_likes_post_user UNIQUE (post_id, user_id)
);

CREATE INDEX idx_post_likes_post_id ON post_likes(post_id);
CREATE INDEX idx_post_likes_user_id ON post_likes(user_id);