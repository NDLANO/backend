update articledata a
set document = jsonb_set(a.document, '{id}', a.article_id::text::jsonb)
where document->>'id' is null
and document is not null;