UPDATE learningsteps ls
SET document=(
    SELECT document || jsonb_build_object('introduction', '[]'::json)
    FROM learningsteps ls2
    WHERE ls.id = ls2.id
)
