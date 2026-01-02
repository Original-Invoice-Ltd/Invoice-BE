-- Create notifications table
CREATE TABLE notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    type ENUM('INVOICE', 'ITEM', 'CLIENT', 'SYSTEM', 'PAYMENT') NOT NULL,
    title VARCHAR(255) NOT NULL,
    message VARCHAR(500) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    is_new BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    related_entity_id VARCHAR(255),
    related_entity_type VARCHAR(50),
    
    INDEX idx_user_id (user_id),
    INDEX idx_user_id_created_at (user_id, created_at DESC),
    INDEX idx_user_id_type (user_id, type),
    INDEX idx_user_id_is_read (user_id, is_read)
);