package com.example.taskcontracts.event;

public enum TaskEventType {
    CREATED {
        @Override
        public String buildMessage(TaskNotificationEvent event) {
            return "Task '%s' was created with status %s".formatted(event.title(), event.status());
        }
    },
    UPDATED {
        @Override
        public String buildMessage(TaskNotificationEvent event) {
            return "Task '%s' was updated. Current status: %s".formatted(event.title(), event.status());
        }
    },
    DELETED {
        @Override
        public String buildMessage(TaskNotificationEvent event) {
            return "Task '%s' was deleted".formatted(event.title());
        }
    };

    public abstract String buildMessage(TaskNotificationEvent event);
}
