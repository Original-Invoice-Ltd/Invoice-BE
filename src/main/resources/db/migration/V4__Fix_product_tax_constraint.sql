-- Fix foreign key constraint issue between _taxes and _user_products tables
-- This migration removes any invalid foreign key constraints that may have been created

-- Check if the problematic foreign key constraint exists and drop it
DO $$
BEGIN
    -- Drop the foreign key constraint if it exists
    IF EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'fk3mj6kghnion69xe2igwxo2lff' 
        AND table_name = '_taxes'
    ) THEN
        ALTER TABLE _taxes DROP CONSTRAINT fk3mj6kghnion69xe2igwxo2lff;
        RAISE NOTICE 'Dropped foreign key constraint fk3mj6kghnion69xe2igwxo2lff from _taxes table';
    END IF;
    
    -- Also check for any product_id column in _taxes table that shouldn't be there
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = '_taxes' 
        AND column_name = 'product_id'
    ) THEN
        ALTER TABLE _taxes DROP COLUMN product_id;
        RAISE NOTICE 'Dropped product_id column from _taxes table';
    END IF;
    
EXCEPTION
    WHEN OTHERS THEN
        RAISE NOTICE 'Error occurred while fixing constraints: %', SQLERRM;
END $$;