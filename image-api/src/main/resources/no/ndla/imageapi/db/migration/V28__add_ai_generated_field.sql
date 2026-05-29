update imagemetadata
set metadata = jsonb_set(metadata, '{aiGenerated}', 'null'::jsonb)
where metadata is not null;
