CREATE TABLE robot_definitions
(
    id            uuid      NOT NULL DEFAULT gen_random_uuid(),
    feide_id      text NULL,
    status        text      NOT NULL,
    created       timestamp NOT NULL DEFAULT now(),
    updated       timestamp NOT NULL DEFAULT now(),
    shared        timestamp NULL,
    configuration jsonb     NOT NULL,
    CONSTRAINT robot_definitions_pkey PRIMARY KEY (id)
);
