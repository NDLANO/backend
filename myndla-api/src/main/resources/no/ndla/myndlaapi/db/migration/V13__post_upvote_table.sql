CREATE TABLE post_upvote (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES my_ndla_users(id) ON DELETE CASCADE,
    post_id BIGINT REFERENCES posts(id) ON DELETE CASCADE
);