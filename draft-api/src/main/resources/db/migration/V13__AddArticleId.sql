ALTER TABLE articledata ADD column article_id bigint not null default 0;
UPDATE articledata SET article_id = id;