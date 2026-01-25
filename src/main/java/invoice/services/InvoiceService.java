package invoice.services;

import invoice.dtos.request.CreateInvoiceRequest;
import invoice.dtos.response.InvoiceResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface InvoiceService {
    InvoiceResponse createInvoice(CreateInvoiceRequest request);
    InvoiceResponse getInvoiceById(UUID id);
    InvoiceResponse getInvoiceByUuid(UUID uuid); // Public method for customers (no auth required)
    List<InvoiceResponse> getAllUserInvoices();
    List<InvoiceResponse> getAllUserInvoices(UUID userId);
    InvoiceResponse updateInvoice(UUID id, CreateInvoiceRequest request);
    void deleteInvoice(UUID id);
    InvoiceResponse uploadPaymentEvidence(UUID invoiceUuid, MultipartFile evidenceFile);
    Map<String, Long> getInvoiceStats(String email);
    InvoiceResponse markInvoiceAsPaid(UUID invoiceId);

    List<InvoiceResponse> getAllInvoices();
}
