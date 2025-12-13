# Enhanced Tax System Guide

## Overview

The enhanced tax system supports multiple distinct taxes per invoice item with client-specific rates. This system is designed to handle complex tax scenarios where different client types (INDIVIDUAL vs BUSINESS) have different tax rates for the same tax type.

## Key Features

### 1. Multiple Tax Types
- **WHT (Withholding Tax)**: 5% for individuals, 10% for businesses
- **VAT (Value Added Tax)**: 7.5% for all client types
- **SALES_TAX**: Configurable sales tax
- **EXCISE_TAX**: Configurable excise tax
- **CUSTOM**: Custom tax types

### 2. Client-Specific Rates
Each tax can have different rates based on client type:
- `individualRate`: Rate applied to INDIVIDUAL clients
- `businessRate`: Rate applied to BUSINESS clients
- `baseTaxRate`: Default/fallback rate

### 3. Multiple Taxes Per Item
Each invoice item can have multiple taxes applied simultaneously, with each tax calculated independently.

### 4. Safe Mapping
All optional fields are safely mapped to prevent null pointer exceptions in responses.

## API Endpoints

### Tax Management

#### Create Tax
```http
POST /api/tax/add
Content-Type: application/json

{
  "name": "Custom Tax",
  "taxType": "CUSTOM",
  "baseTaxRate": 5.0,
  "individualRate": 3.0,
  "businessRate": 7.0,
  "description": "Custom tax with different rates",
  "isActive": true
}
```

#### Get Active Taxes
```http
GET /api/tax/active
```

#### Get Applicable Taxes for Client Type
```http
GET /api/tax/applicable?clientType=INDIVIDUAL
GET /api/tax/applicable?clientType=BUSINESS
```

#### Initialize Default Taxes
```http
POST /api/tax/initialize-defaults
```

### Tax Calculation Testing

#### Calculate Tax for Specific Amount
```http
POST /api/tax-calculation/calculate?taxId={uuid}&itemAmount=1000&clientType=INDIVIDUAL
```

#### Test Tax Rates
```http
GET /api/tax-calculation/test-rates?taxId={uuid}
```

### Invoice Item Management

#### Add Item with Taxes
```http
POST /api/invoices/{invoiceId}/items
Content-Type: application/json

{
  "itemName": "MacBook Pro 2020 Laptop",
  "category": "ELECTRONICS",
  "description": "High-performance laptop",
  "quantity": 1,
  "rate": 50000,
  "amount": 50000,
  "taxIds": ["tax-uuid-1", "tax-uuid-2"]
}
```

## Database Schema

### Enhanced _taxes Table
```sql
CREATE TABLE _taxes (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    tax_type VARCHAR(50),
    base_tax_rate DECIMAL(5,2),
    individual_rate DECIMAL(5,2),
    business_rate DECIMAL(5,2),
    description TEXT,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
```

### New _invoice_item_taxes Table
```sql
CREATE TABLE _invoice_item_taxes (
    id BIGSERIAL PRIMARY KEY,
    invoice_item_id BIGINT NOT NULL,
    tax_id UUID NOT NULL,
    applied_rate DECIMAL(5,2) NOT NULL,
    tax_amount DECIMAL(15,2) NOT NULL,
    taxable_amount DECIMAL(15,2) NOT NULL,
    FOREIGN KEY (invoice_item_id) REFERENCES _invoice_items(id) ON DELETE CASCADE,
    FOREIGN KEY (tax_id) REFERENCES _taxes(id) ON DELETE CASCADE
);
```

## Tax Calculation Logic

### Rate Selection
1. Check if client-specific rate exists (individualRate or businessRate)
2. Fall back to baseTaxRate if client-specific rate is null
3. Use 0% if no rates are defined

### Tax Amount Calculation
```
taxAmount = (itemAmount × applicableRate) / 100
```

### Example Calculations

#### WHT for Individual Client (5%)
- Item Amount: ₦50,000
- Tax Rate: 5%
- Tax Amount: ₦2,500
- Total: ₦52,500

#### WHT for Business Client (10%)
- Item Amount: ₦50,000
- Tax Rate: 10%
- Tax Amount: ₦5,000
- Total: ₦55,000

#### Multiple Taxes (WHT + VAT for Business)
- Item Amount: ₦50,000
- WHT (10%): ₦5,000
- VAT (7.5%): ₦3,750
- Total Tax: ₦8,750
- Total Amount: ₦58,750

## Response Structure

### Invoice Response with Taxes
```json
{
  "id": "invoice-uuid",
  "items": [
    {
      "id": 1,
      "itemName": "MacBook Pro 2020 Laptop",
      "amount": 50000,
      "totalTaxAmount": 8750,
      "amountWithTax": 58750,
      "appliedTaxes": [
        {
          "taxId": "wht-uuid",
          "taxName": "Withholding Tax",
          "taxType": "WHT",
          "appliedRate": 10.0,
          "taxAmount": 5000,
          "taxableAmount": 50000
        },
        {
          "taxId": "vat-uuid",
          "taxName": "Value Added Tax",
          "taxType": "VAT",
          "appliedRate": 7.5,
          "taxAmount": 3750,
          "taxableAmount": 50000
        }
      ]
    }
  ],
  "subtotal": 50000,
  "totalTaxAmount": 8750,
  "totalDue": 58750
}
```

## Migration Notes

### Backward Compatibility
- Legacy `tax` field in invoice items is preserved
- Existing tax data is migrated to new structure
- Old API endpoints continue to work

### Database Migration
The system includes automatic database migration (V2__Add_Enhanced_Tax_System.sql) that:
1. Adds new columns to existing tables
2. Creates new tax relationship table
3. Migrates existing data
4. Initializes default taxes

## Error Handling

### Safe Mapping
All optional fields are safely mapped with null checks:
- Missing tax amounts default to 0
- Missing rates default to base rate or 0
- Invalid client types default to base rate

### Exception Handling
- Invalid tax IDs are logged and skipped
- Calculation errors are caught and logged
- Safe defaults are provided for all totals

## Testing

### Manual Testing Steps
1. Initialize default taxes: `POST /api/tax/initialize-defaults`
2. Create a client (INDIVIDUAL or BUSINESS)
3. Create an invoice for that client
4. Add items with multiple taxes
5. Verify calculations in response

### Test Scenarios
1. **Single Tax**: Apply only WHT to an item
2. **Multiple Taxes**: Apply both WHT and VAT
3. **Client Type Differences**: Same item for INDIVIDUAL vs BUSINESS
4. **Edge Cases**: Zero amounts, missing taxes, invalid data

## Performance Considerations

### Database Indexes
- Index on `(tax_type, is_active)` for efficient tax lookups
- Index on `invoice_item_id` for tax relationship queries
- Index on `tax_id` for reverse lookups

### Caching
Consider implementing caching for:
- Active taxes by client type
- Tax calculation results for common amounts
- Client type lookups

## Future Enhancements

### Planned Features
1. **Tax Groups**: Predefined sets of taxes for different industries
2. **Geographic Taxes**: Location-based tax rates
3. **Time-based Rates**: Tax rates that change over time
4. **Tax Exemptions**: Client-specific tax exemptions
5. **Compound Taxes**: Taxes calculated on top of other taxes

### API Versioning
The current implementation maintains backward compatibility. Future breaking changes will use API versioning (e.g., `/api/v2/tax`).