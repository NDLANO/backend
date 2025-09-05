update learningpaths
set document = jsonb_set(document, '{status}', '"PUBLISHED"'::jsonb)
where document->>'status' = 'UNLISTED'
and document ->> 'verificationStatus' = 'CREATED_BY_NDLA'
and document is not null;
