-- Made a blunder that inserted multiple rows on insert, this deletes rows which have duplicate
DELETE FROM articledata
WHERE id NOT IN (
    SELECT max(id)
    FROM articledata GROUP BY article_id, revision
);