update conceptdata
set document = jsonb_set(document, '{updatedBy}', '[]')
where document->>'updatedBy' is null
and document is not null;

update publishedconceptdata
set document = jsonb_set(document, '{updatedBy}', '[]')
where document->>'updatedBy' is null
and document is not null;

update conceptdata
set document = jsonb_set(document, '{editorNotes}', '[]')
where document->>'editorNotes' is null
and document is not null;

update publishedconceptdata
set document = jsonb_set(document, '{editorNotes}', '[]')
where document->>'editorNotes' is null
and document is not null;
