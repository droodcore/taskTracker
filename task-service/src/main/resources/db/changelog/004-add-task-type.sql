-- liquibase formatted sql

-- changeset tasktracker:4
ALTER TABLE tasks ADD COLUMN type VARCHAR(50) NOT NULL DEFAULT 'OTHER';
