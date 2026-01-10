package invoice.services.implementation;

import invoice.data.constants.Item_Category;
import invoice.data.models.Invoice;
import invoice.data.models.InvoiceItem;
import invoice.data.repositories.InvoiceItemRepository;
import invoice.data.repositories.InvoiceRepository;
import invoice.dtos.request.InvoiceItemRequest;
import invoice.dtos.response.InvoiceItemResponse;
import invoice.exception.ResourceNotFoundException;
import invoice.services.InvoiceItemService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class InvoiceItemServiceImpl implements InvoiceItemService {

    private final InvoiceItemRepository invoiceItemRepository;
    private final InvoiceRepository invoiceRepository;

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

        invoice.addItem(item);
        
        // Update invoice totals
        updateInvoiceTotals(invoice);
        
        invoiceRepository.save(invoice);

        log.info("Added item '{}' to invoice {}", item.getItemName(), invoiceId);

        return new InvoiceItemResponse(item);
    }

    @Override
    @Transactional
    public InvoiceItemResponse updateInvoiceItem(Long itemId, InvoiceItemRequest request) {
        InvoiceItem item = invoiceItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice item not found"));
        
        Invoice invoice = item.getInvoice();

        // Update basic item properties
        item.setItemName(request.getItemName());
        item.setCategory(request.getCategory() != null ? Item_Category.valueOf(request.getCategory()) : null);
        item.setDescription(request.getDescription());
        item.setQuantity(request.getQuantity());
        item.setRate(request.getRate());
        item.setAmount(request.getAmount());

        // Update invoice totals
        updateInvoiceTotals(invoice);
        
        InvoiceItem updated = invoiceItemRepository.save(item);
        
        log.info("Updated item '{}'", updated.getItemName());

        return new InvoiceItemResponse(updated);
    }

    @Override
    @Transactional
    public void deleteInvoiceItem(Long itemId) {
        InvoiceItem item = invoiceItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice item not found"));
        
        Invoice invoice = item.getInvoice();
        invoice.removeItem(item);
        
        // Update invoice totals
        updateInvoiceTotals(invoice);
        
        invoiceRepository.save(invoice);
        
        log.info("Deleted item '{}' from invoice {}", item.getItemName(), invoice.getId());
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
    
    private void updateInvoiceTotals(Invoice invoice) {
        try {
            Double subtotal = invoice.calculateSubtotal();
            Double totalTaxAmount = invoice.calculateTotalTaxAmount();
            Double totalDue = invoice.calculateTotalDue();
            
            invoice.setSubtotal(subtotal);
            invoice.setTotalTaxAmount(totalTaxAmount);
            invoice.setTotalDue(totalDue);
            
            log.debug("Updated invoice totals - Subtotal: {}, Tax: {}, Total: {}", 
                    subtotal, totalTaxAmount, totalDue);
        } catch (Exception e) {
            log.error("Error updating invoice totals for invoice {}: {}", invoice.getId(), e.getMessage());
            // Set safe defaults
            invoice.setSubtotal(0.0);
            invoice.setTotalTaxAmount(0.0);
            invoice.setTotalDue(0.0);
        }
    }
}
