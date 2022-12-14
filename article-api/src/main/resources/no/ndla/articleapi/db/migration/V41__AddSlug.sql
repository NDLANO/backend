ALTER TABLE contentdata ADD COLUMN slug text;
CREATE INDEX ON contentdata(slug);