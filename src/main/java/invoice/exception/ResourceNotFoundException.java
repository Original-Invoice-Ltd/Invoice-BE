package invoice.exception;

public class ResourceNotFoundException extends OriginalInvoiceBaseException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
