UPDATE my_ndla_users mnu
SET document=(
    SELECT document || jsonb_build_object('arenaEnabled', false)
    FROM my_ndla_users mnu2
    WHERE mnu.id = mnu2.id
)
