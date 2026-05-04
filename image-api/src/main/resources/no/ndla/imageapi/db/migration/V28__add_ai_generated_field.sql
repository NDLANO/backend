update imagemetadata
set metadata = jsonb_set(metadata, '{aiGenerated}', '"No"'::jsonb)
where metadata->>'aiGenerated' is null
  and metadata is not null;
