update imagemetadata
set metadata = jsonb_set(metadata, '{inactive}', 'false')
where metadata->>'inactive' is null
and metadata is not null;
