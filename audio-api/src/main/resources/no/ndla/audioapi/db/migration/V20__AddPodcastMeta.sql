update audiodata
set document = jsonb_set(document, '{podcastMeta}', '[]')
where document->>'podcastMeta' is null
and document is not null;
