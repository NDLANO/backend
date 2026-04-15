update contentdata
set document = jsonb_set(document, '{revised}', to_jsonb(document->>'published'), true)
where document is not null;

update contentdata
set document = jsonb_set(document, '{published}', to_jsonb(document->>'updated'), false)
where document is not null;