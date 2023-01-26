ALTER TABLE folders
ADD COLUMN last_shared TIMESTAMP DEFAULT NULL;

UPDATE folders SET last_shared = NOW() WHERE "folders".status = 'shared';