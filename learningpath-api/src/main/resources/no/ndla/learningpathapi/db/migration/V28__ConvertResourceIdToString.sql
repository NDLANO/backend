UPDATE resources r
SET document=(
    SELECT document || jsonb_build_object('resourceId', document->>'resourceId')
    FROM resources r2
    WHERE r2.id = r.id
);

