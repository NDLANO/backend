CREATE SEQUENCE article_id_sequence;
-- Creating sequences doesn't allow for dynamic startvalue, lets start at highest article_id value + 1
SELECT setval('article_id_sequence', (select max(article_id) from articledata) + 1);