-- delete the oldest row of the singular duplicate
delete
from articledata
where id = 72537;

alter table articledata
    add constraint articledata_article_id_revision_key unique (article_id, revision);

drop index if exists articledata_article_id_idx;
