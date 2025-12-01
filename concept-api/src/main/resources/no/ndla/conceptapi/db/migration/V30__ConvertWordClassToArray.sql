UPDATE conceptdata
SET document = jsonb_set(document, '{glossData,wordClass}', to_jsonb(ARRAY[document->'glossData'->>'wordClass']::text[]))
WHERE document->'glossData'->>'wordClass' IS NOT NULL
AND document IS NOT NULL;

UPDATE publishedconceptdata
SET document = jsonb_set(document, '{glossData,wordClass}', to_jsonb(ARRAY[document->'glossData'->>'wordClass']::text[]))
WHERE document->'glossData'->>'wordClass' IS NOT NULL
AND document IS NOT NULL;
