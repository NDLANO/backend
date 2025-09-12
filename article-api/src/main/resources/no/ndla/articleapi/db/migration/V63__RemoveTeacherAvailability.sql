update contentdata
set document = jsonb_set(document, '{availability}', '"everyone"')
where document->>'availability' = 'teacher';
