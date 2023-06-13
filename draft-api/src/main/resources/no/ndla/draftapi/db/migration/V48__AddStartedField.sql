update articledata
set document=(document||'{"started":false}'::jsonb)
where document is not null
