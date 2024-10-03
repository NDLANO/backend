ALTER TABLE saved_shared_folder
    ADD COLUMN "rank" int4 NULL;


-- Update rank to be incremental for each user
WITH ranked_data AS (
    SELECT
        folder_id,
        feide_id,
        ROW_NUMBER() OVER (PARTITION BY feide_id ORDER BY folder_id) AS new_rank
    FROM saved_shared_folder
)
UPDATE saved_shared_folder sf
SET rank = ranked_data.new_rank
FROM ranked_data
WHERE sf.folder_id = ranked_data.folder_id
  AND sf.feide_id = ranked_data.feide_id;

ALTER TABLE saved_shared_folder
    ALTER COLUMN "rank" DROP NOT NULL;

ALTER TABLE folders
    ALTER COLUMN "rank" DROP NOT NULL;

ALTER TABLE folder_resources
    ALTER COLUMN "rank" DROP NOT NULL;
