update imagemetadata
set metadata = jsonb_set(metadata, '{editorNotes}', '[]')
where metadata->>'editorNotes' is null
and metadata is not null;
