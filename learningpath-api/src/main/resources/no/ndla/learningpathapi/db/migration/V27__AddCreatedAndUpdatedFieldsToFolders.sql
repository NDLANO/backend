ALTER TABLE folders
ADD COLUMN created TIMESTAMP DEFAULT NOW(),
ADD COLUMN updated TIMESTAMP DEFAULT NOW();
