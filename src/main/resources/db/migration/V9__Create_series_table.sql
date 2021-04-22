CREATE TABLE seriesdata (
  id BIGSERIAL PRIMARY KEY,
  revision integer not null default 1,
  document JSONB
);

-- ALTER TABLE audiodata
--     ADD COLUMN series_id BIGSERIAL
--         REFERENCES seriesdata(id);

ALTER TABLE audiodata
    ADD COLUMN series_id BIGINT NULL;

ALTER TABLE audiodata
    ADD CONSTRAINT fk_series
    FOREIGN KEY(series_id)
    REFERENCES seriesdata(id)
    ON DELETE SET NULL;


