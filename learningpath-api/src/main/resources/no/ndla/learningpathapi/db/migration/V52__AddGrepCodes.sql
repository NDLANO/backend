update learningpaths
set document = jsonb_set(document, '{grepCodes}', '[]')
where document->>'grepCodes' is null
and document is not null;
