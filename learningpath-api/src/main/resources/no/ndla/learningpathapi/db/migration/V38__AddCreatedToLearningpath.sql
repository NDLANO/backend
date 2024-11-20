UPDATE learningpaths lp
SET document=(
    SELECT document || jsonb_build_object('created', document ->> 'lastUpdated')
    FROM learningpaths lp2
    WHERE lp.id = lp2.id
)
