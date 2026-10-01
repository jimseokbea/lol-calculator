-- Run once on an existing MySQL database before starting the updated application.
-- Hibernate ddl-auto=update does not reliably replace an existing primary key.
ALTER TABLE processed_match
    DROP PRIMARY KEY,
    ADD PRIMARY KEY (user_id, match_id);
