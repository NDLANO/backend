update articledata
set document = jsonb_set(document, '{revised}', to_jsonb(document->>'published'), true)
where document is not null;

update articledata
set document = jsonb_set(document, '{published}', to_jsonb(document->>'updated'), false)
where document is not null
and (document->'status' ->> 'current' = 'PUBLISHED');

update articledata
set document = jsonb_set(document, '{published}', 'null'::jsonb, false)
where document is not null
and document->'status' ->> 'current' != 'PUBLISHED'
and not (document->'status' -> 'other' @> '["PUBLISHED"]'::jsonb);