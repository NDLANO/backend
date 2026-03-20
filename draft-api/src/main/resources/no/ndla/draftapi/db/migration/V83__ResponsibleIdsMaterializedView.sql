CREATE MATERIALIZED VIEW IF NOT EXISTS responsible_view AS
    SELECT distinct ("document" -> 'responsible' ->> 'responsibleId') as responsibleId
    FROM articledata WHERE ("document" -> 'responsible' ->> 'responsibleId') IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS responsibleId_idx ON responsible_view(responsibleId);

-- CREATE OR REPLACE FUNCTION refresh_responsible_view()
-- RETURNS trigger AS $$
-- BEGIN
--     EXECUTE format(
--         'REFRESH MATERIALIZED VIEW CONCURRENTLY %I.responsible_view',
--         TG_TABLE_SCHEMA
--     );
-- RETURN NULL;
-- END;
-- $$ LANGUAGE plpgsql;
--
-- CREATE OR REPLACE TRIGGER refresh_responsible_view_trigger
--     AFTER UPDATE OR INSERT OR DELETE ON articledata
--     FOR EACH STATEMENT
--     EXECUTE FUNCTION refresh_responsible_view();