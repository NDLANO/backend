CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    title text,
    description text
);

CREATE TABLE topics (
    id BIGSERIAL PRIMARY KEY,
    title text,
    category_id BIGINT REFERENCES categories(id) ON DELETE CASCADE,
    owner_id BIGINT REFERENCES my_ndla_users(id) ON DELETE CASCADE,
    created timestamp NOT NULL DEFAULT now(),
    updated timestamp NOT NULL DEFAULT now()
);

CREATE INDEX topics_category_id_idx ON topics(category_id);
CREATE INDEX topics_owner_id_idx ON topics(owner_id);

CREATE TABLE posts (
    id BIGSERIAL PRIMARY KEY,
    content text,
    topic_id BIGINT REFERENCES topics(id) ON DELETE CASCADE,
    owner_id BIGINT REFERENCES my_ndla_users(id) ON DELETE CASCADE,
    created timestamp NOT NULL DEFAULT now(),
    updated timestamp NOT NULL DEFAULT now()
);

CREATE INDEX posts_topic_id_idx ON posts(topic_id);
CREATE INDEX posts_owner_id_idx ON posts(owner_id);

CREATE TABLE category_follows (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES my_ndla_users(id),
    category_id BIGINT REFERENCES categories(id) ON DELETE CASCADE
);

CREATE INDEX category_follows_user_id_idx ON category_follows(user_id);
CREATE INDEX category_follows_category_id_idx ON category_follows(category_id);

CREATE TABLE topic_follows (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES my_ndla_users(id),
    topic_id BIGINT REFERENCES topics(id) ON DELETE CASCADE
);

CREATE INDEX topic_follows_user_id_idx ON topic_follows(user_id);
CREATE INDEX topic_follows_topic_id_idx ON topic_follows(topic_id);

CREATE TABLE flags (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES my_ndla_users(id) ON DELETE CASCADE,
    post_id BIGINT REFERENCES posts(id) ON DELETE CASCADE,
    reason text,
    created timestamp NOT NULL DEFAULT now(),
    resolved timestamp NULL
);

CREATE INDEX flags_post_id_idx ON flags(post_id);
CREATE INDEX flags_user_id_idx ON flags(user_id);

CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES my_ndla_users(id) ON DELETE CASCADE,
    post_id BIGINT REFERENCES posts(id) ON DELETE CASCADE,
    topic_id BIGINT REFERENCES topics(id) ON DELETE CASCADE,
    is_read BOOLEAN DEFAULT FALSE,
    notification_time timestamp NOT NULL DEFAULT now()
);
