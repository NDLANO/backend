ALTER TABLE folders ADD COLUMN name text DEFAULT NULL;
UPDATE folders SET name = (document ->> 'name');
ALTER TABLE folders ALTER COLUMN name SET NOT NULL;

ALTER TABLE folders ADD COLUMN status text DEFAULT NULL;
UPDATE folders SET status = (document ->> 'status');
ALTER TABLE folders ALTER COLUMN status SET NOT NULL;

ALTER TABLE folders DROP COLUMN document;
