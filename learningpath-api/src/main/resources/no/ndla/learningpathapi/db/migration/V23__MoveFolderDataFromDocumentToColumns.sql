ALTER TABLE folders ADD COLUMN name text DEFAULT NULL;
UPDATE folders SET name = (document ->> 'name');
ALTER TABLE folders ADD COLUMN status text DEFAULT NULL;
UPDATE folders SET status = (document ->> 'status');
ALTER TABLE folders DROP COLUMN document;
