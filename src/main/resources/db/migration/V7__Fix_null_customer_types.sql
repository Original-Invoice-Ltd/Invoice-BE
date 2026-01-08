-- Fix null customer types in existing client records
UPDATE _user_clients 
SET customer_type = 'INDIVIDUAL' 
WHERE customer_type IS NULL;