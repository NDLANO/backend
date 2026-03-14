-- Add exifData to each entry in the images array if not present
update imagemetadata
set metadata = (
  select jsonb_set(
    metadata,
    '{images}',
    coalesce(
      (select jsonb_agg(
        case
          when elem->>'exifData' is null then elem || '{"exifData": {}}'::jsonb
          else elem
        end
      ) from jsonb_array_elements(metadata->'images') as elem),
      '[]'::jsonb
    )
  )
)
where metadata is not null
and metadata->'images' is not null;

