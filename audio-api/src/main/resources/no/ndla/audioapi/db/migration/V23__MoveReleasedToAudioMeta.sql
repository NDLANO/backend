UPDATE audiodata
SET document = jsonb_set(
    document || jsonb_build_object('released', document->>'created'),
    '{podcastMeta}',
    COALESCE(
        (
            SELECT jsonb_agg(meta - 'released')
            FROM jsonb_array_elements(document->'podcastMeta') AS meta
        ),
        '[]'
    )
);

UPDATE seriesdata
SET document = document - 'episodes' WHERE document ? 'episodes'