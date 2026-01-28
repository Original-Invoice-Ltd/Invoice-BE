package invoice.services.implementation;

import invoice.data.models.Receipt;
import invoice.data.repositories.ReceiptRepository;
import invoice.dtos.response.ReceiptResponse;
import invoice.exception.ResourceNotFoundException;
import invoice.services.ReceiptService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@AllArgsConstructor
@Slf4j
public class ReceiptServiceImpl implements ReceiptService {
    
    private final ReceiptRepository receiptRepository;
    
    @Override
    public ReceiptResponse getReceiptById(UUID receiptId) {
        log.info("Fetching receipt with ID: {}", receiptId);
        
        Receipt receipt = receiptRepository.findByIdWithInvoice(receiptId)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt not found with ID: " + receiptId));
        
        return new ReceiptResponse(receipt);
    }
    
    @Override
    public ReceiptResponse getReceiptByInvoiceId(UUID invoiceId) {
        log.info("Fetching receipt for invoice ID: {}", invoiceId);
        
        Receipt receipt = receiptRepository.findByInvoiceId(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt not found for invoice ID: " + invoiceId));
        
        return new ReceiptResponse(receipt);
    }
    
    @Override
    public byte[] generateReceiptPdf(UUID receiptId) {
        log.info("Generating PDF for receipt ID: {}", receiptId);
        
        // TODO: Implement PDF generation using a library like iText or Apache PDFBox
        // For now, return empty byte array as placeholder
        throw new UnsupportedOperationException("PDF generation not yet implemented");
    }
}
