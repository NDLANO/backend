CREATE INDEX ON learningpaths USING GIN ((jsonb_path_query_array(document, '$.learningsteps[*].id')));
