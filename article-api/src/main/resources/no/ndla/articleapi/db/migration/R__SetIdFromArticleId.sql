update contentdata c
set document = jsonb_set(c.document, '{id}', c.article_id::text::jsonb)
where document->>'id' is null
and document is not null;