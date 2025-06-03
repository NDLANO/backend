-- 1. Find resources that have duplicate data
WITH duplicate_groups AS (
  SELECT feide_id, path, resource_type
  FROM resources
  WHERE feide_id IS NOT NULL
    AND path IS NOT NULL
    AND resource_type IS NOT NULL
  GROUP BY feide_id, path, resource_type
  HAVING COUNT(*) > 1
),
-- 2. Rank these resources based on creation date
ranked_resources AS (
  SELECT
    r.id AS resource_id,
    r.feide_id,
    r.path,
    r.resource_type,
    r.created,
    ROW_NUMBER() OVER (
      PARTITION BY r.feide_id, r.path, r.resource_type
      ORDER BY r.created ASC NULLS LAST, r.id ASC
    ) AS idx
  FROM resources r
  JOIN duplicate_groups dg
    ON r.feide_id = dg.feide_id
    AND r.path = dg.path
    AND r.resource_type = dg.resource_type
),
-- 3. Create a mapping from duplicate ids -> canonical ids
resource_map AS (
    SELECT dup.resource_id AS duplicate_id, canon.resource_id AS canonical_id
    FROM ranked_resources dup
    JOIN ranked_resources canon
      ON dup.feide_id = canon.feide_id
      AND dup.path = canon.path
      AND dup.resource_type = canon.resource_type
      AND canon.idx = 1
    WHERE dup.idx > 1
),
-- 4. Update connections to resources that are to be deleted for duplication
updated_folder_resources AS (
  UPDATE folder_resources fr
  SET resource_id = rm.canonical_id
  FROM resource_map rm
  WHERE fr.resource_id = rm.duplicate_id
  AND NOT EXISTS (
    SELECT 1 FROM folder_resources fr2
    WHERE fr2.folder_id = fr.folder_id
    AND fr2.resource_id = rm.canonical_id
  )
)
-- 5. Delete the duplicates
DELETE FROM resources r
USING resource_map rm
WHERE r.id = rm.duplicate_id;

-- 6. Fix ranks after duplicated resources in folders have been deleted
WITH ranked AS (
  SELECT
  folder_id,
  resource_id,
  ROW_NUMBER() OVER (
    PARTITION BY folder_id
    ORDER BY rank, favorited_date
  ) AS new_rank
  FROM folder_resources
)
UPDATE folder_resources fr
SET rank = r.new_rank
FROM ranked r
WHERE fr.folder_id = r.folder_id
AND fr.resource_id = r.resource_id;

-- 7. Add constraint to resources to avoid duplicates in the future
ALTER TABLE resources
ADD CONSTRAINT unique_resource_field_combo
UNIQUE (resource_type, path, feide_id)
