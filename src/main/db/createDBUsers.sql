-- Schema
CREATE SCHEMA audioapi;

-- READONLY
CREATE USER audioapi_read with PASSWORD '<passord>';
ALTER DEFAULT PRIVILEGES IN SCHEMA audioapi GRANT SELECT ON TABLES TO audioapi_read;

GRANT CONNECT ON DATABASE data_prod to audioapi_read;
GRANT USAGE ON SCHEMA audioapi to audioapi_read;
GRANT SELECT ON ALL SEQUENCES IN SCHEMA audioapi TO audioapi_read;
GRANT SELECT ON ALL TABLES IN SCHEMA audioapi TO audioapi_read;

-- WRITE
CREATE USER audioapi_write with PASSWORD '<passord>';

GRANT CONNECT ON DATABASE data_prod to audioapi_write;
GRANT USAGE ON SCHEMA audioapi to audioapi_write;
GRANT CREATE ON SCHEMA audioapi to audioapi_write;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA audioapi TO audioapi_write;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA audioapi TO audioapi_write;
