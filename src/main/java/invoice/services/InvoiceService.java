package invoice.services;

import invoice.dtos.request.CreateInvoiceRequest;
import invoice.dtos.response.InvoiceResponse;

import java.util.List;
import java.util.UUID;

public interface InvoiceService {
    InvoiceResponse createInvoice(CreateInvoiceRequest request);
    InvoiceResponse getInvoiceById(UUID id);
    List<InvoiceResponse> getAllUserInvoices();
    InvoiceResponse updateInvoice(UUID id, CreateInvoiceRequest request);
    void deleteInvoice(UUID id);

    List<InvoiceResponse> getAllInvoices();
}
