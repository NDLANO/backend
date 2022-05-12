ALTER TABLE articledata ALTER external_id TYPE text[] USING ARRAY[external_id];
ALTER TABLE conceptdata ALTER external_id TYPE text[] USING ARRAY[external_id];
