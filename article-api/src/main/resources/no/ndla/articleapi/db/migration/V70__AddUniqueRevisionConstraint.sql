-- delete the oldest row of the singular duplicate
delete
from contentdata
where id = 50237;

alter table contentdata
    add constraint contentdata_article_id_revision_key unique (article_id, revision);

drop index if exists contentdata_article_id_idx;
