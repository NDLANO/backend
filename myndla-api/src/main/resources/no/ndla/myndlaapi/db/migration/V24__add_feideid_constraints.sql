-- Remove orphaned folders for non-existing feide_ids
delete from folders f
where f.feide_id is not null
  and not exists (
    select 1
    from my_ndla_users u
    where u.feide_id = f.feide_id
);

-- Constraints for cascade delete from my_ndla_users
alter table folders add constraint fk_feide_id foreign key (feide_id) references my_ndla_users(feide_id) on delete cascade;
alter table resources add constraint fk_feide_id foreign key (feide_id) references my_ndla_users(feide_id) on delete cascade;
alter table saved_shared_folder add constraint fk_feide_id foreign key (feide_id) references my_ndla_users(feide_id) on delete cascade;
