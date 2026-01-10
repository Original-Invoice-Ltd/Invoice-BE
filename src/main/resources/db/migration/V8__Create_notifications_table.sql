-- Create notifications table
CREATE TABLE _notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    message VARCHAR(500) NOT NULL,
    type VARCHAR(50) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    related_entity_id UUID,
    related_entity_type VARCHAR(50),
    user_id UUID NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    
    CONSTRAINT fk_notification_user 
        FOREIGN KEY (user_id) 
        REFERENCES _users(id) 
        ON DELETE CASCADE
);

-- Create indexes for better performance
CREATE INDEX idx_notifications_user_id ON _notifications(user_id);
CREATE INDEX idx_notifications_is_read ON _notifications(is_read);
CREATE INDEX idx_notifications_created_at ON _notifications(created_at);
CREATE INDEX idx_notifications_user_unread ON _notifications(user_id, is_read) WHERE is_read = FALSE;