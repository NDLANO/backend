CREATE MATERIALIZED VIEW IF NOT EXISTS user_ids_view AS
SELECT DISTINCT userId FROM (
    SELECT document ->> 'updatedBy' AS userId FROM articledata WHERE document ->> 'updatedBy' IS NOT NULL
    UNION
    SELECT jsonb_array_elements(document -> 'notes') ->> 'user' AS userId FROM articledata WHERE jsonb_array_length(COALESCE(document -> 'notes', '[]'::jsonb)) > 0
    UNION
    SELECT jsonb_array_elements(document -> 'previousVersionsNotes') ->> 'user' AS userId FROM articledata WHERE jsonb_array_length(COALESCE(document -> 'previousVersionsNotes', '[]'::jsonb)) > 0
) AS all_users
WHERE userId IS NOT NULL AND userId != '';
CREATE UNIQUE INDEX IF NOT EXISTS userId_idx ON user_ids_view(userId);

