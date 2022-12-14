ALTER TABLE articledata ADD COLUMN slug text;
CREATE INDEX ON articledata(slug);