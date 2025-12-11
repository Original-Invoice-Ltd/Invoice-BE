package invoice.services;

import invoice.dtos.request.InvoiceItemRequest;
import invoice.dtos.response.InvoiceItemResponse;

import java.util.List;
import java.util.UUID;

public interface InvoiceItemService {
    InvoiceItemResponse addItemToInvoice(UUID invoiceId, InvoiceItemRequest request);
    InvoiceItemResponse updateInvoiceItem(Long itemId, InvoiceItemRequest request);
    void deleteInvoiceItem(Long itemId);
    List<InvoiceItemResponse> getInvoiceItems(UUID invoiceId);
    InvoiceItemResponse getInvoiceItemById(Long itemId);
}
