package invoice.services;

import invoice.dtos.request.CreateInvoiceRequest;
import invoice.dtos.response.CreateInvoiceResponse;

import java.util.List;

public interface InvoiceService {
    CreateInvoiceResponse createInvoice(CreateInvoiceRequest request);
    CreateInvoiceResponse getInvoiceById(Long id);
    List<CreateInvoiceResponse> getAllInvoices();
    CreateInvoiceResponse updateInvoice(Long id, CreateInvoiceRequest request);
    void deleteInvoice(Long id);
}
