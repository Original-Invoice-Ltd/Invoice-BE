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
import org.springframework.web.multipart.MultipartFile;

@Service
@AllArgsConstructor
@Slf4j
public class Invoic implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final ModelMapper modelMapper;
    private final CloudinaryService cloudinaryService;

    // Helper for safe uploads
    private String safeUpload(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) return null;
        return cloudinaryService.uploadFile(file);
    }

    @Override
    public CreateInvoiceResponse createInvoice(CreateInvoiceRequest request) {
        log.info("Creating invoice with title: {}", request.getTitle());

        Invoice invoice = modelMapper.map(request, Invoice.class);

        try {
            invoice.setImageUrl(safeUpload(request.getImage()));
            invoice.setLogoUrl(safeUpload(request.getLogo()));
        } catch (IOException e) {
            throw new RuntimeException("Error uploading files", e);
        }

        Invoice saved = invoiceRepository.save(invoice);
        return modelMapper.map(saved, CreateInvoiceResponse.class);
    }

    @Override
    public CreateInvoiceResponse getInvoiceById(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));
        return modelMapper.map(invoice, CreateInvoiceResponse.class);
    }

    @Override
    public List<CreateInvoiceResponse>> getAllInvoices() {
        return invoiceRepository.findAll().stream()
                .map(inv -> modelMapper.map(inv, CreateInvoiceResponse.class))
                .collect(Collectors.toList());
    }

    @Override
    public CreateInvoiceResponse updateInvoice(Long id, CreateInvoiceRequest request) {
        Invoice existing = invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        String oldImage = existing.getImageUrl();
        String oldLogo = existing.getLogoUrl();

        // Map only NON-NULL fields (keeps PATCH behaviour correct)
        if (request.getTitle() != null) existing.setTitle(request.getTitle());
        if (request.getDescription() != null) existing.setDescription(request.getDescription());
        if (request.getAmount() != null) existing.setAmount(request.getAmount());
        if (request.getDate() != null) existing.setDate(request.getDate());

        String newImage = null;
        String newLogo = null;

        try {
            if (request.getImage() != null && !request.getImage().isEmpty()) {
                newImage = safeUpload(request.getImage());
                existing.setImageUrl(newImage);
            }

            if (request.getLogo() != null && !request.getLogo().isEmpty()) {
                newLogo = safeUpload(request.getLogo());
                existing.setLogoUrl(newLogo);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error uploading updated files", e);
        }

        Invoice updated = invoiceRepository.save(existing);

        // Cleanup old files after success
        if (newImage != null && oldImage != null)
            cloudinaryService.deleteFile(oldImage);

        if (newLogo != null && oldLogo != null)
            cloudinaryService.deleteFile(oldLogo);

        return modelMapper.map(updated, CreateInvoiceResponse.class);
    }

    @Override
    public void deleteInvoice(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        // Delete cloud files
        if (invoice.getImageUrl() != null) cloudinaryService.deleteFile(invoice.getImageUrl());
        if (invoice.getLogoUrl() != null) cloudinaryService.deleteFile(invoice.getLogoUrl());

        invoiceRepository.deleteById(id);
        log.info("Invoice deleted successfully with ID: {}", id);
    }
}
