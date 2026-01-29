alter table my_ndla_users
    add column last_seen timestamp not null default now();
