CREATE MATERIALIZED VIEW IF NOT EXISTS responsible_view AS
    SELECT distinct ("document" -> 'responsible' ->> 'responsibleId') as responsibleId
    FROM articledata WHERE ("document" -> 'responsible' ->> 'responsibleId') IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS responsibleId_idx ON responsible_view(responsibleId);