update userdata
set document = jsonb_set(document, '{latestEditedLearningpaths}', '[]')
where document is not null;
