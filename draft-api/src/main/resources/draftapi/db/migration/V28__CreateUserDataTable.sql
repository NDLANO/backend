CREATE TABLE userdata (
  id BIGSERIAL PRIMARY KEY,
  user_id TEXT,
  document JSONB
);