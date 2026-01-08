-- Add client information fields to invoice_recipients table
-- This allows storing client data directly in the invoice without maintaining a reference

ALTER TABLE _invoice_recipients 
ADD COLUMN IF NOT EXISTS full_name VARCHAR(255),
ADD COLUMN IF NOT EXISTS business_name VARCHAR(255),
ADD COLUMN IF NOT EXISTS customer_type VARCHAR(50),
ADD COLUMN IF NOT EXISTS title VARCHAR(50),
ADD COLUMN IF NOT EXISTS country VARCHAR(100);