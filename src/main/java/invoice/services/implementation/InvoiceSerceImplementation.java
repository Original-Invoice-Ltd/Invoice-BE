package invoice.services.implementation;

import invoice.data.models.Invoice;
import invoice.data.repositories.InvoiceRepository;
import invoice.dtos.request.CreateInvoiceRequest;
import invoice.dtos.response.CreateInvoiceResponse;
import invoice.services.InvoiceService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
public class InvoiceSerceImplementation implements InvoiceService {
    
    private final InvoiceRepository invoiceRepository;
    private final ModelMapper modelMapper;

    @Override
    public CreateInvoiceResponse createInvoice(CreateInvoiceRequest request) {
        log.info("Creating invoice with title: {}", request.getTitle());
        Invoice invoice = modelMapper.map(request, Invoice.class);
        Invoice savedInvoice = invoiceRepository.save(invoice);
        log.info("Invoice created successfully with ID: {}", savedInvoice.getId());
        return modelMapper.map(savedInvoice, CreateInvoiceResponse.class);
    }

    @Override
    public CreateInvoiceResponse getInvoiceById(Long id) {
        log.info("Fetching invoice with ID: {}", id);
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found with ID"));
        return modelMapper.map(invoice, CreateInvoiceResponse.class);
    }

    @Override
    public List<CreateInvoiceResponse> getAllInvoices() {
        log.info("Fetching all invoices");
        List<Invoice> invoices = invoiceRepository.findAll();
        return invoices.stream()
                .map(invoice -> modelMapper.map(invoice, CreateInvoiceResponse.class))
                .collect(Collectors.toList());
    }

    @Override
    public CreateInvoiceResponse updateInvoice(Long id, CreateInvoiceRequest request) {
        log.info("Updating invoice with ID: {}", id);
        Invoice existingInvoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found with" + id));
        
        modelMapper.map(request, existingInvoice);
        existingInvoice.setId(id);
        
        Invoice updatedInvoice = invoiceRepository.save(existingInvoice);
        log.info("Invoice updated successfully with ID: {}", updatedInvoice.getId());
        return modelMapper.map(updatedInvoice, CreateInvoiceResponse.class);
    }

    @Override
    public void deleteInvoice(Long id) {
        log.info("Deleting invoice with ID: {}", id);
        if (!invoiceRepository.existsById(id)) {
            throw new RuntimeException("Invoice not found");
        }
        invoiceRepository.deleteById(id);
        log.info("Invoice deleted successfully with ID: {}", 
