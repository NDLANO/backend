CREATE TABLE articledata (
  id BIGSERIAL PRIMARY KEY,
  external_id TEXT,
  document JSONB,
  external_subject_id TEXT[],
  revision integer not null default 1
);

CREATE TABLE conceptdata (
  id BIGSERIAL PRIMARY KEY,
  external_id TEXT,
  document JSONB
);

CREATE TABLE agreementdata (
  id BIGSERIAL PRIMARY KEY,
  document JSONB
);
