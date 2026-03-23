CREATE MATERIALIZED VIEW IF NOT EXISTS editor_view AS
SELECT DISTINCT editorId FROM (
    SELECT document ->> 'updatedBy' AS editorId FROM articledata WHERE document ->> 'updatedBy' IS NOT NULL
    UNION
    SELECT jsonb_array_elements(document -> 'notes') ->> 'user' AS editorId FROM articledata WHERE jsonb_array_length(COALESCE(document -> 'notes', '[]'::jsonb)) > 0
    UNION
    SELECT jsonb_array_elements(document -> 'previousVersionsNotes') ->> 'user' AS editorId FROM articledata WHERE jsonb_array_length(COALESCE(document -> 'previousVersionsNotes', '[]'::jsonb)) > 0
) AS all_users
WHERE editorId IS NOT NULL AND editorId != '';

CREATE UNIQUE INDEX IF NOT EXISTS editorId_idx ON editor_view(editorId);
