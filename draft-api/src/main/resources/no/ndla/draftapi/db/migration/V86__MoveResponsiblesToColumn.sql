alter table articledata
    add column if not exists responsible            text,
    add column if not exists responsible_updated_at timestamptz;

update articledata
set responsible            = document -> 'responsible' ->> 'responsibleId',
    responsible_updated_at = (document -> 'responsible' ->> 'lastUpdated')::timestamptz
where document is not null;

create index if not exists articledata_responsible_idx on articledata (responsible);

drop materialized view if exists responsible_view;
