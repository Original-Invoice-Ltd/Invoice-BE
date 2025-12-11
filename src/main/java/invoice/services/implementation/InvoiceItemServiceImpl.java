package invoice.services.implementation;

import invoice.data.constants.Item_Category;
import invoice.data.models.Invoice;
import invoice.data.models.InvoiceItem;
import invoice.data.models.Tax;
import invoice.data.repositories.InvoiceItemRepository;
import invoice.data.repositories.InvoiceRepository;
import invoice.data.repositories.TaxRepository;
import invoice.dtos.request.InvoiceItemRequest;
import invoice.dtos.response.InvoiceItemResponse;
import invoice.exception.ResourceNotFoundException;
import invoice.services.InvoiceItemService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class InvoiceItemServiceImpl implements InvoiceItemService {

    private final InvoiceItemRepository invoiceItemRepository;
    private final InvoiceRepository invoiceRepository;
    private final TaxRepository taxRepository;

    @Override
    @Transactional
    public InvoiceItemResponse addItemToInvoice(UUID invoiceId, InvoiceItemRequest request) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        InvoiceItem item = new InvoiceItem();
        item.setItemName(request.getItemName());
        item.setCategory(request.getCategory() != null ? Item_Category.valueOf(request.getCategory()) : null);
        item.setDescription(request.getDescription());
        item.setQuantity(request.getQuantity());
        item.setRate(request.getRate());
        item.setAmount(request.getAmount());
        item.setTax(request.getTax());

        if (request.getTaxIds() != null && !request.getTaxIds().isEmpty()) {
            List<Tax> taxes = taxRepository.findAllById(request.getTaxIds());
            item.setTaxes(taxes);
        }

        invoice.addItem(item);
        invoiceRepository.save(invoice);

        return new InvoiceItemResponse(item);
    }

    @Override
    @Transactional
    public InvoiceItemResponse updateInvoiceItem(Long itemId, InvoiceItemRequest request) {
        InvoiceItem item = invoiceItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice item not found"));

        item.setItemName(request.getItemName());
        item.setCategory(request.getCategory() != null ? Item_Category.valueOf(request.getCategory()) : null);
        item.setDescription(request.getDescription());
        item.setQuantity(request.getQuantity());
        item.setRate(request.getRate());
        item.setAmount(request.getAmount());
        item.setTax(request.getTax());

        if (request.getTaxIds() != null) {
            List<Tax> taxes = taxRepository.findAllById(request.getTaxIds());
            item.setTaxes(taxes);
        }

        InvoiceItem updated = invoiceItemRepository.save(item);
        return new InvoiceItemResponse(updated);
    }

    @Override
    @Transactional
    public void deleteInvoiceItem(Long itemId) {
        InvoiceItem item = invoiceItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice item not found"));
        
        Invoice invoice = item.getInvoice();
        invoice.removeItem(item);
        invoiceRepository.save(invoice);
    }

    @Override
    public List<InvoiceItemResponse> getInvoiceItems(UUID invoiceId) {
        List<InvoiceItem> items = invoiceItemRepository.findByInvoiceId(invoiceId);
        return items.stream()
                .map(InvoiceItemResponse::new)
                .collect(Collectors.toList());
    }

    @Override
    public InvoiceItemResponse getInvoiceItemById(Long itemId) {
        InvoiceItem item = invoiceItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice item not found"));
        return new InvoiceItemResponse(item);
    }
}
