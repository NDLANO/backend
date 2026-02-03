alter table my_ndla_users
    add column last_seen timestamp not null default now();

update my_ndla_users
    set last_seen = (document->>'lastUpdated')::timestamp;
