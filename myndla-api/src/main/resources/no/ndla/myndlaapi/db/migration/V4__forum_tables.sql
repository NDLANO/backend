CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    title text,
    description text
);

CREATE TABLE topics (
    id BIGSERIAL PRIMARY KEY,
    title text,
    content text,
    category_id BIGINT REFERENCES categories(id),
    created timestamp NOT NULL DEFAULT now(),
    updated timestamp NOT NULL DEFAULT now()
);

CREATE TABLE posts (
    id BIGSERIAL PRIMARY KEY,
    title text,
    content text,
    topic_id BIGINT REFERENCES topics(id),
    created timestamp NOT NULL DEFAULT now(),
    updated timestamp NOT NULL DEFAULT now()
);

CREATE TABLE topic_follows (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES my_ndla_users(id),
    topic_id BIGINT REFERENCES topics(id)
);

CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES my_ndla_users(id),
    post_id BIGINT REFERENCES posts(id),
    topic_id BIGINT REFERENCES topics(id),
    is_read BOOLEAN DEFAULT FALSE
);
