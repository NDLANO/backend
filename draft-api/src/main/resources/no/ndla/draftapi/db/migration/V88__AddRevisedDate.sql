-- Add revised date to articledata based on published date.
UPDATE articledata
SET document = jsonb_set(document, '{revised}', to_jsonb(document->>'published'), true)
WHERE document IS NOT NULL;

-- Update published with updated date if article is published,
-- otherwise set published to null if article is not published and has never been published before.
UPDATE articledata
SET document = jsonb_set(document, '{published}', to_jsonb(document->>'updated'), false)
WHERE document IS NOT NULL
AND (document->'status' ->> 'current' = 'PUBLISHED');

UPDATE articledata
SET document = jsonb_set(document, '{published}', 'null'::jsonb, false)
WHERE document IS NOT NULL
AND document->'status' ->> 'current' != 'PUBLISHED'
AND NOT (document->'status' -> 'other' @> '["PUBLISHED"]'::jsonb);

-- Set firstPublished to published, including null values.
UPDATE articledata
SET document = jsonb_set(document, '{firstPublished}', to_jsonb(document->>'published'), true)
WHERE document IS NOT NULL;

-- Set firstPublished to the first published date for the first revision with published of the article.
-- Only updates the current version of the article.
WITH first_published AS (
    SELECT DISTINCT ON (ad.article_id)
           ad.article_id,
           ad.document->>'published' AS published
    FROM articledata ad
    WHERE ad.document IS NOT NULL
      AND ad.document->>'published' IS NOT NULL
    ORDER BY ad.article_id, ad.revision ASC
),
current_article AS (
    SELECT article_id, max(revision) AS revision
    FROM articledata
    WHERE document IS NOT NULL
    GROUP BY article_id
)
UPDATE articledata ad
SET document = jsonb_set(
        ad.document,
        '{firstPublished}',
        to_jsonb(fp.published),
        true
               )
    FROM current_article ca
JOIN first_published fp
ON fp.article_id = ca.article_id
WHERE ad.article_id = ca.article_id
  AND ad.revision = ca.revision
  AND ad.document IS NOT NULL
