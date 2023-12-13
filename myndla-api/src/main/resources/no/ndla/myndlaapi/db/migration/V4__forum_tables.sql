CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    title text,
    description text
);

CREATE TABLE topics (
    id BIGSERIAL PRIMARY KEY,
    title text,
    category_id BIGINT REFERENCES categories(id),
    owner_id BIGINT REFERENCES my_ndla_users(id),
    created timestamp NOT NULL DEFAULT now(),
    updated timestamp NOT NULL DEFAULT now()
);

CREATE INDEX topics_category_id_idx ON topics(category_id);
CREATE INDEX topics_owner_id_idx ON topics(owner_id);

CREATE TABLE posts (
    id BIGSERIAL PRIMARY KEY,
    content text,
    topic_id BIGINT REFERENCES topics(id),
    owner_id BIGINT REFERENCES my_ndla_users(id),
    created timestamp NOT NULL DEFAULT now(),
    updated timestamp NOT NULL DEFAULT now()
);

CREATE INDEX posts_topic_id_idx ON posts(topic_id);
CREATE INDEX posts_owner_id_idx ON posts(owner_id);

CREATE TABLE topic_follows (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES my_ndla_users(id),
    topic_id BIGINT REFERENCES topics(id)
);

CREATE TABLE flags (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES my_ndla_users(id),
    post_id BIGINT REFERENCES posts(id),
    reason text,
    created timestamp NOT NULL DEFAULT now(),
    resolved timestamp NULL
);

CREATE INDEX flags_post_id_idx ON flags(post_id);
CREATE INDEX flags_user_id_idx ON flags(user_id);

CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES my_ndla_users(id),
    post_id BIGINT REFERENCES posts(id),
    topic_id BIGINT REFERENCES topics(id),
    is_read BOOLEAN DEFAULT FALSE
);
