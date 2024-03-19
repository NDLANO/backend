update contentdata
set document = jsonb_set(document, '{conceptIds}', '[]')
where document->>'conceptIds' is null
and document is not null;

update contentdata
set document = jsonb_set(document, '{grepCodes}', '[]')
where document->>'grepCodes' is null
and document is not null;

update contentdata
set document = jsonb_set(document, '{availability}', '"everyone"')
where document->>'availability' is null
and document is not null;

update contentdata
set document = jsonb_set(document, '{relatedContent}', '[]')
where document->>'relatedContent' is null
and document is not null;
