BEGIN;
-- protect against concurrent inserts while you update the counter
LOCK TABLE articledata IN EXCLUSIVE MODE;
-- Update the sequence
SELECT setval('articledata_id_seq', COALESCE((SELECT MAX(id)+1 FROM articledata), 1), false);
COMMIT;