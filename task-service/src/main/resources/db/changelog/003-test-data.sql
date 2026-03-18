-- liquibase formatted sql

-- changeset tasktracker:3
INSERT INTO users (username, email) VALUES
    ('admin', 'admin@example.com'),
    ('testuser', 'test@example.com');

INSERT INTO tasks (title, description, deadline, user_id, category_id, status) VALUES
    ('Buy groceries', 'Milk, Bread, Cheese', '2026-03-01 10:00:00', 1, 1, 'TODO'),
    ('Finish report', 'Q1 financial report', '2026-03-05 18:00:00', 1, 2, 'IN_PROGRESS'),
    ('Learn Spring Boot', 'Read documentation', null, 2, 3, 'DONE');
