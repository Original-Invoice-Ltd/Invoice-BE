package invoice.services.implementation;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import invoice.config.CloudinaryService;
import invoice.data.models.Invoice;
import invoice.data.repositories.InvoiceRepository;
import invoice.dtos.request.CreateInvoiceRequest;
import invoice.dtos.response.CreateInvoiceResponse;
import invoice.services.InvoiceService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@AllArgsConstructor
@Slf4j
public class InvoiceServiceImplementation implements InvoiceService {
    
    private final InvoiceRepository invoiceRepository;
    private final ModelMapper modelMapper;
    private final CloudinaryService cloudinaryService;

    @Override
    public CreateInvoiceResponse createInvoice(CreateInvoiceRequest request) {
        log.info("Creating invoice with title: {}", request.getTitle());

        String imageUrl = null;
        String logoUrl = null;

        try {
            if (request.getImage() != null && !request.getImage().isEmpty()) {
                imageUrl = cloudinaryService.uploadFile(request.getImage());
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload image file", e);
        }

        try {
            if (request.getLogo() != null && !request.getLogo().isEmpty()) {
                logoUrl = cloudinaryService.uploadFile(request.getLogo());
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload logo file", e);
        }

        Invoice invoice = modelMapper.map(request, Invoice.class);
        invoice.setImageUrl(imageUrl);
        invoice.setLogoUrl(logoUrl);

        Invoice savedInvoice = invoiceRepository.save(invoice);
        log.info("Invoice created successfully with ID: {}", savedInvoice.getId());
        return modelMapper.map(savedInvoice, CreateInvoiceResponse.class);
    }

    @Override
    public CreateInvoiceResponse getInvoiceById(Long id) {
        log.info("Fetching invoice with ID: {}", id);
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));
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
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        String oldImageUrl = existingInvoice.getImageUrl();
        String oldLogoUrl = existingInvoice.getLogoUrl();

        modelMapper.map(request, existingInvoice);

        try {
            if (request.getImage() != null && !request.getImage().isEmpty()) {
                String newImageUrl = cloudinaryService.uploadFile(request.getImage());
                existingInvoice.setImageUrl(newImageUrl);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload updated image", e);
        }

        try {
            if (request.getLogo() != null && !request.getLogo().isEmpty()) {
                String newLogoUrl = cloudinaryService.uploadFile(request.getLogo());
                existingInvoice.setLogoUrl(newLogoUrl);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload updated logo", e);
        }

        Invoice updatedInvoice = invoiceRepository.save(existingInvoice);
        log.info("Invoice updated successfully with ID: {}", updatedInvoice.getId());

        if (request.getImage() != null && !request.getImage().isEmpty() && oldImageUrl != null) {
            cloudinaryService.deleteFile(oldImageUrl);
        }
        if (request.getLogo() != null && !request.getLogo().isEmpty() && oldLogoUrl != null) {
            cloudinaryService.deleteFile(oldLogoUrl);
        }

        return modelMapper.map(updatedInvoice, CreateInvoiceResponse.class);
    }

    @Override
    public void deleteInvoice(Long id) {
        log.info("Deleting invoice with ID: {}", id);
        
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));
        
        if (invoice.getImageUrl() != null) {
            cloudinaryService.deleteFile(invoice.getImageUrl());
        }
        if (invoice.getLogoUrl() != null) {
            cloudinaryService.deleteFile(invoice.getLogoUrl());
        }
        
        invoiceRepository.deleteById(id);
        log.info("Invoice deleted successfully with ID: {}", id);
    }
}
