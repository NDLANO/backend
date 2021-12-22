-- Add article_id so id is still unique while versioning
ALTER TABLE contentdata ADD column article_id bigint not null default 0;
UPDATE contentdata SET article_id = id;

-- Update contentdata id sequence since inserts have been using determined ids for a while and are _very_ likely out of date
SELECT setval('contentdata_id_seq', (SELECT MAX(id) FROM contentdata) + 1);
