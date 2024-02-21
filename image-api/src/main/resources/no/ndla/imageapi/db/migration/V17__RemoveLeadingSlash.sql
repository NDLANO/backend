UPDATE imagefiledata
SET file_name = regexp_replace(file_name, '^/', '')
