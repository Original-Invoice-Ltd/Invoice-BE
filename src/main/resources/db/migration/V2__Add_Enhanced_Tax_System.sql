-- Enhanced Tax System Migration
-- This migration adds support for multiple distinct taxes per item with client-specific rates

-- Add new columns to _taxes table
ALTER TABLE _taxes ADD COLUMN IF NOT EXISTS tax_type VARCHAR(50);
ALTER TABLE _taxes ADD COLUMN IF NOT EXISTS base_tax_rate DECIMAL(5,2);
ALTER TABLE _taxes ADD COLUMN IF NOT EXISTS individual_rate DECIMAL(5,2);
ALTER TABLE _taxes ADD COLUMN IF NOT EXISTS business_rate DECIMAL(5,2);
ALTER TABLE _taxes ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE _taxes ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT true;

-- Migrate existing tax_rate to base_tax_rate
UPDATE _taxes SET base_tax_rate = tax_rate WHERE base_tax_rate IS NULL;

-- Drop the old tax_rate column (optional - keep for backward compatibility)
-- ALTER TABLE _taxes DROP COLUMN IF EXISTS tax_rate;

-- Add total_tax_amount column to _invoices table
ALTER TABLE _invoices ADD COLUMN IF NOT EXISTS total_tax_amount DOUBLE PRECISION;

-- Update existing invoice items table to use DECIMAL for better precision
ALTER TABLE _invoice_items ALTER COLUMN rate TYPE DECIMAL(15,2);
ALTER TABLE _invoice_items ALTER COLUMN amount TYPE DECIMAL(15,2);
ALTER TABLE _invoice_items ALTER COLUMN tax TYPE DECIMAL(15,2);

-- Create invoice_item_taxes table for the new tax structure
CREATE TABLE IF NOT EXISTS _invoice_item_taxes (
    id BIGSERIAL PRIMARY KEY,
    invoice_item_id BIGINT NOT NULL,
    tax_id UUID NOT NULL,
    applied_rate DECIMAL(5,2) NOT NULL,
    tax_amount DECIMAL(15,2) NOT NULL,
    taxable_amount DECIMAL(15,2) NOT NULL,
    FOREIGN KEY (invoice_item_id) REFERENCES _invoice_items(id) ON DELETE CASCADE,
    FOREIGN KEY (tax_id) REFERENCES _taxes(id) ON DELETE CASCADE
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_invoice_item_taxes_item_id ON _invoice_item_taxes(invoice_item_id);
CREATE INDEX IF NOT EXISTS idx_invoice_item_taxes_tax_id ON _invoice_item_taxes(tax_id);
CREATE INDEX IF NOT EXISTS idx_taxes_type_active ON _taxes(tax_type, is_active);

-- Drop the old many-to-many table if it exists
DROP TABLE IF EXISTS invoice_item_taxes;

-- Insert default taxes if they don't exist
INSERT INTO _taxes (id, name, tax_type, base_tax_rate, individual_rate, business_rate, description, is_active, created_at)
SELECT 
    gen_random_uuid(),
    'Withholding Tax',
    'WHT',
    5.00,
    5.00,
    10.00,
    'Withholding Tax - 5% for individuals, 10% for businesses',
    true,
    NOW()
WHERE NOT EXISTS (SELECT 1 FROM _taxes WHERE name = 'Withholding Tax' AND tax_type = 'WHT');

INSERT INTO _taxes (id, name, tax_type, base_tax_rate, individual_rate, business_rate, description, is_active, created_at)
SELECT 
    gen_random_uuid(),
    'Value Added Tax',
    'VAT',
    7.50,
    7.50,
    7.50,
    'Value Added Tax - 7.5% for all client types',
    true,
    NOW()
WHERE NOT EXISTS (SELECT 1 FROM _taxes WHERE name = 'Value Added Tax' AND tax_type = 'VAT');

-- Update existing invoices to have safe total_tax_amount values
UPDATE _invoices SET total_tax_amount = 0.0 WHERE total_tax_amount IS NULL;