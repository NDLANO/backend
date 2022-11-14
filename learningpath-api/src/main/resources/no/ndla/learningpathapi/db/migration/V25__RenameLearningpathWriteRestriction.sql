UPDATE configtable SET
    value = jsonb_set(value, '{key}', '"LEARNINGPATH_WRITE_RESTRICTED"'),
    configkey = 'LEARNINGPATH_WRITE_RESTRICTED'
WHERE
    configkey = 'IS_WRITE_RESTRICTED';
