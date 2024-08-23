CREATE TABLE registry.contracts (
    subject VARCHAR NOT NULL,
    version INTEGER NOT NULL,
    id INTEGER NOT NULL,
    isMerged BOOLEAN NOT NULL DEFAULT FALSE,
    schematype TEXT NOT NULL,
    schema TEXT NOT NULL,
    PRIMARY KEY (subject, id)
);

