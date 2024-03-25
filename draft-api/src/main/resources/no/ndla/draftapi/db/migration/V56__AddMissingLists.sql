update articledata
set document = jsonb_set(document, '{comments}', '[]')
where document->>'comments' is null
and document is not null;
