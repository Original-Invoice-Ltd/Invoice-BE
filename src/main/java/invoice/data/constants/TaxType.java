package invoice.data.constants;

public enum TaxType {
    WHT("Withholding Tax"),
    VAT("Value Added Tax"),
    SALES_TAX("Sales Tax"),
    EXCISE_TAX("Excise Tax"),
    CUSTOM("Custom Tax");
    
    private final String displayName;
    
    TaxType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}