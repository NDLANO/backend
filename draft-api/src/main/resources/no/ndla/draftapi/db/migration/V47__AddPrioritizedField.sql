update articledata
set document=(document||'{"prioritized":false}'::jsonb)
where document is not null
