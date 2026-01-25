package invoice.services;

import invoice.dtos.response.ReceiptResponse;

import java.util.UUID;

public interface ReceiptService {
    ReceiptResponse getReceiptById(UUID receiptId);
    ReceiptResponse getReceiptByInvoiceId(UUID invoiceId);
    byte[] generateReceiptPdf(UUID receiptId);
}
