UPDATE conceptdata cd
SET document=(
    SELECT document || jsonb_build_object('editorNotes', JSON '[]')
    FROM conceptdata cd2
    WHERE cd.id = cd2.id
)