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
  ) || coalesce(metadata->'editorNotes', '[]'::jsonb)
)
where metadata is not null
  and metadata->>'created' is not null
  and metadata->>'createdBy' is not null
  and (
    metadata->'editorNotes' is null
    or jsonb_typeof(metadata->'editorNotes') = 'array'
  )
  and not exists (
    select 1
    from jsonb_array_elements(coalesce(metadata->'editorNotes', '[]'::jsonb)) as editor_note(note_json)
    where editor_note.note_json->>'note' like 'Image created%'
  );

