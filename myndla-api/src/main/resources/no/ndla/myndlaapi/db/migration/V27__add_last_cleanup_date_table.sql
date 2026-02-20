CREATE TABLE user_cleanup_audit (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    num_cleanup INT,
    num_emailed INT,
    last_cleanup_date TIMESTAMP
);


