alter table resources
    alter column created type timestamptz using created at time zone 'UTC';

alter table folders
    alter column created type timestamptz using created at time zone 'UTC',
    alter column updated type timestamptz using updated at time zone 'UTC',
    alter column shared  type timestamptz using shared  at time zone 'UTC';

alter table resource_connections
    alter column favorited_date type timestamptz using favorited_date at time zone 'UTC';

alter table my_ndla_users
    alter column last_seen type timestamptz using last_seen at time zone 'UTC';

alter table robot_definitions
    alter column created type timestamptz using created at time zone 'UTC',
    alter column updated type timestamptz using updated at time zone 'UTC',
    alter column shared  type timestamptz using shared  at time zone 'UTC';

alter table user_cleanup_audit
    alter column last_cleanup_date type timestamptz using last_cleanup_date at time zone 'UTC';
