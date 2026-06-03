create table image_editors
(
    image_id bigint not null,
    user_id  text   not null,
    primary key (image_id, user_id)
);

insert into image_editors (image_id, user_id)
select distinct note_users.image_id, note_users.user_id
from (select id                                       as image_id,
             notes ->> 'updatedBy'                    as user_id
      from imagemetadata
               cross join lateral jsonb_array_elements(metadata -> 'editorNotes') as notes) as note_users
where user_id is not null
on conflict do nothing;

insert into image_editors (image_id, user_id)
select id, metadata ->> 'createdBy'
from imagemetadata
where metadata is not null
  and metadata ->> 'createdBy' is not null
on conflict do nothing;

insert into image_editors (image_id, user_id)
select id, metadata ->> 'updatedBy'
from imagemetadata
where metadata is not null
  and metadata ->> 'updatedBy' is not null
on conflict do nothing;

