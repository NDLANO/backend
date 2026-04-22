-- Add revised date to articledata based on published date.
-- Update published date depending
UPDATE articledata
SET document = document
    || jsonb_build_object('revised', document -> 'published')
    || jsonb_build_object('published',
                          CASE
                              WHEN document -> 'status' ->> 'current' = 'PUBLISHED'
                                  THEN document -> 'updated'
                              WHEN NOT (document -> 'status' -> 'other' @> '["PUBLISHED"]'::jsonb)
                                  THEN 'null'::jsonb
                              ELSE document -> 'published'
                              END)
WHERE document IS NOT NULL;

-- Add firstPublished based on published from first version published
WITH first_published AS (SELECT DISTINCT ON (article_id) article_id,
    document -> 'published' AS published
FROM articledata
WHERE document IS NOT NULL
  AND document ->> 'published' IS NOT NULL
ORDER BY article_id, revision ASC)
UPDATE articledata ad
SET document = jsonb_set(
        ad.document,
        '{firstPublished}',
        COALESCE(fp.published, 'null'::jsonb),
        true)
    FROM articledata src
         LEFT JOIN first_published fp ON fp.article_id = src.article_id
WHERE ad.id = src.id
  AND ad.document IS NOT NULL;