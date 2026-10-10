-- A shared row serializes removal/deactivation, including two administrators acting concurrently.
CREATE TABLE account_management_guard (id INTEGER PRIMARY KEY CHECK (id = 1));
INSERT INTO account_management_guard (id) VALUES (1);
