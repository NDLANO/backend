update imagemetadata
set metadata = jsonb_set(
  metadata,
  '{editorNotes}',
  jsonb_build_array(
	jsonb_build_object(
	  'note', 'Image created.',
	  'timeStamp', metadata->>'created',
	  'updatedBy', metadata->>'createdBy'
	)
  )
)
where metadata is not null
  and metadata->'editorNotes' = '[]'::jsonb
  and metadata->>'created' is not null
  and metadata->>'createdBy' is not null;

