UPDATE configtable c
SET value=(
    SELECT value || jsonb_build_object('value', jsonb_build_object('value', (c2.value->>'value')::jsonb))
    FROM configtable c2
    WHERE c2.configkey = c.configkey
)
