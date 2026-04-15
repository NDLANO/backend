create table draft_editors
(
    draft_id bigint not null,
    user_id  text   not null,
    primary key (draft_id, user_id)
);

insert into draft_editors (draft_id, user_id)
select note_users.article_id, note_users.user_id
from (select article_id,
             notes ->> 'user' as user_id
      from articledata ar
               cross join lateral jsonb_array_elements(ar.document -> 'notes') as notes
      union
      select article_id,
             prev_notes ->> 'user' as user_id
      from articledata ar
               cross join lateral jsonb_array_elements(ar.document -> 'previousVersionsNotes') as prev_notes) as note_users;

insert into draft_editors (draft_id, user_id)
select distinct article_id, document ->> 'updatedBy'
from articledata ar
where document is not null
on conflict do nothing;

drop materialized view if exists editor_view;
