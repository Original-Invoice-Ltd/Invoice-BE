package invoice.services.implementation;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import invoice.data.models.*;
import invoice.data.repositories.*;
import invoice.dtos.request.InvoiceItemRequest;
import invoice.dtos.response.ClientResponse;
import invoice.dtos.response.InvoiceItemResponse;
import invoice.dtos.response.InvoiceResponse;
import invoice.dtos.response.InvoiceSenderResponse;
import invoice.exception.ResourceNotFoundException;
import invoice.data.constants.Item_Category;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import invoice.config.CloudinaryService;
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
    private final InvoiceSequenceRepository invoiceSequenceRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final CloudinaryService cloudinaryService;
    private final ClientRepository clientRepository;
    private final TaxRepository taxRepository;
    private InvoiceSenderRepository invoiceSenderRepository;


    @Override
    @Transactional
    public InvoiceResponse createInvoice(CreateInvoiceRequest request) {
        User currentUser = getCurrentUser();
        Client client = clientRepository.findById(request.getClientId())
                .orElseThrow(()->new ResourceNotFoundException("client not found"));
        String logoUrl = null;
        String signatureUrl = null;

        try {
            if (request.getLogo() != null && !request.getLogo().isEmpty()) {
                logoUrl = cloudinaryService.uploadFile(request.getLogo());
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload logo file", e);
        }

        try {
            if (request.getSignature() != null && !request.getSignature().isEmpty()) {
                signatureUrl = cloudinaryService.uploadFile(request.getSignature());
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload signature file", e);
        }

        // Create invoice entity manually to avoid detached entity issues
        Invoice invoice = new Invoice();
        
        // Map basic fields from request (excluding items which need special handling)
        invoice.setTitle(request.getTitle());
        invoice.setInvoiceColor(request.getInvoiceColor());
        invoice.setCreationDate(request.getInvoiceDate() != null ? request.getInvoiceDate().atStartOfDay() : null);
        invoice.setDueDate(request.getDueDate() != null ? request.getDueDate().atStartOfDay() : null);
        invoice.setPaymentTerms(request.getPaymentTerms());
        invoice.setAccountNumber(request.getAccountNumber());
        invoice.setAccountName(request.getAccountName());
        invoice.setBank(request.getBank());
        invoice.setCurrency(request.getCurrency());
        invoice.setSubtotal(request.getSubtotal());
        invoice.setTotalDue(request.getTotalDue());
        invoice.setNote(request.getNote());
        invoice.setTermsAndConditions(request.getTermsAndConditions());
        
        // Handle invoice items manually to avoid detached entity issues
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            for (InvoiceItemRequest itemRequest : request.getItems()) {
                InvoiceItem item = new InvoiceItem();
                item.setItemName(itemRequest.getItemName());
                item.setDescription(itemRequest.getDescription());
                item.setQuantity(itemRequest.getQuantity());
                item.setRate(itemRequest.getRate());
                item.setAmount(itemRequest.getAmount());
                item.setTax(itemRequest.getTax());
                
                // Set category if provided
                if (itemRequest.getCategory() != null) {
                    try {
                        item.setCategory(Item_Category.valueOf(itemRequest.getCategory().toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        log.warn("Invalid category '{}' for item '{}', skipping category", 
                                itemRequest.getCategory(), itemRequest.getItemName());
                    }
                }
                
                // Handle taxes if provided
                if (itemRequest.getTaxIds() != null && !itemRequest.getTaxIds().isEmpty()) {
                    for (UUID taxId : itemRequest.getTaxIds()) {
                        Tax tax = taxRepository.findById(taxId).orElse(null);
                        if (tax != null) {
                            InvoiceItemTax itemTax = new InvoiceItemTax();
                            itemTax.setTax(tax);
                            
                            // Calculate tax amount based on item amount and tax rate
                            if (item.getAmount() != null && tax.getBaseTaxRate() != null) {
                                BigDecimal taxableAmount = item.getAmount();
                                BigDecimal appliedRate = tax.getBaseTaxRate();
                                BigDecimal taxAmount = taxableAmount
                                        .multiply(appliedRate)
                                        .divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP);
                                
                                itemTax.setTaxableAmount(taxableAmount);
                                itemTax.setAppliedRate(appliedRate);
                                itemTax.setTaxAmount(taxAmount);
                            } else {
                                // Set default values if calculation not possible
                                itemTax.setTaxableAmount(BigDecimal.ZERO);
                                itemTax.setAppliedRate(BigDecimal.ZERO);
                                itemTax.setTaxAmount(BigDecimal.ZERO);
                            }
                            
                            item.addItemTax(itemTax);
                        }
                    }
                }
                
                // Add item to invoice using helper method to maintain bidirectional relationship
                invoice.addItem(item);
            }
        }
        
        InvoiceSender sender = new InvoiceSender();
        sender.setEmail(request.getEmail());
        sender.setFullName(request.getFullName());
        sender.setPhone(request.getPhone());

        if(request.getAddress() != null && !request.getAddress().isEmpty())
            sender.setAddress(request.getAddress());
        invoice.setUser(currentUser);
        if (logoUrl != null)
            invoice.setLogoUrl(logoUrl);
        if (signatureUrl != null)
            invoice.setSignatureUrl(signatureUrl);
        // Handle invoice number generation
        if (request.getInvoiceNumber() == null || request.getInvoiceNumber().trim().isEmpty()) {
            // Auto-generate invoice number for this user
            String generatedNumber = generateNextAvailableInvoiceNumber(currentUser);
            invoice.setInvoiceNumber(generatedNumber);
            log.info("Auto-generated invoice number: {} for user: {}", generatedNumber, currentUser.getEmail());
        } else {
            // User provided manual invoice number - check for duplicates
            if (invoiceRepository.findByInvoiceNumberAndUserId(request.getInvoiceNumber(), currentUser.getId()).isPresent()) {
                throw new RuntimeException("Invoice number already exists: " + request.getInvoiceNumber());
            }
            invoice.setInvoiceNumber(request.getInvoiceNumber());
            log.info("Using manual invoice number: {} for user: {}", request.getInvoiceNumber(), currentUser.getEmail());
        }
        invoice.setClientId(client.getId());
        Invoice savedInvoice = invoiceRepository.save(invoice);
        sender.setInvoice(savedInvoice);
        invoiceSenderRepository.save(sender);
        return mapToResponse(savedInvoice, client, sender);
    }

    private InvoiceResponse mapToResponse(Invoice savedInvoice, Client client, InvoiceSender sender) {
        InvoiceResponse response = new InvoiceResponse(savedInvoice);
        
        try {
            // Safe mapping for client
            if (client != null) {
                ClientResponse billTo = new ClientResponse(client);
                response.setBillTo(billTo);
            }
            
            // Safe mapping for sender
            if (sender != null) {
                InvoiceSenderResponse billFrom = new InvoiceSenderResponse(sender);
                response.setBillFrom(billFrom);
            }
            
            // Safe mapping for items with taxes
            if (savedInvoice.getItems() != null) {
                List<InvoiceItemResponse> itemResponses = savedInvoice.getItems().stream()
                        .map(item -> {
                            try {
                                return new InvoiceItemResponse(item);
                            } catch (Exception e) {
                                log.error("Error mapping item {}: {}", item.getId(), e.getMessage());
                                // Return a safe default item response
                                InvoiceItemResponse safeResponse = new InvoiceItemResponse();
                                safeResponse.setId(item.getId());
                                safeResponse.setItemName(item.getItemName() != null ? item.getItemName() : "");
                                safeResponse.setAmount(item.getAmount() != null ? item.getAmount() : BigDecimal.ZERO);
                                safeResponse.setTotalTaxAmount(BigDecimal.ZERO);
                                safeResponse.setAmountWithTax(item.getAmount() != null ? item.getAmount() : BigDecimal.ZERO);
                                return safeResponse;
                            }
                        })
                        .collect(Collectors.toList());
                response.setItems(itemResponses);
            }
            
            // Safe mapping for totals
            response.setSubtotal(savedInvoice.getSubtotal() != null ? savedInvoice.getSubtotal() : 0.0);
            response.setTotalTaxAmount(savedInvoice.getTotalTaxAmount() != null ? savedInvoice.getTotalTaxAmount() : 0.0);
            response.setTotalDue(savedInvoice.getTotalDue() != null ? savedInvoice.getTotalDue() : 0.0);
            
        } catch (Exception e) {
            log.error("Error mapping invoice response for invoice {}: {}", savedInvoice.getId(), e.getMessage());
            // Ensure response has safe defaults
            if (response.getSubtotal() == null) response.setSubtotal(0.0);
            if (response.getTotalTaxAmount() == null) response.setTotalTaxAmount(0.0);
            if (response.getTotalDue() == null) response.setTotalDue(0.0);
        }
        
        return response;
    }


    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }
    
    private String generateNextAvailableInvoiceNumber(User user) {
        InvoiceSequence sequence = invoiceSequenceRepository.findByUserIdForUpdate(user.getId())
                .orElseGet(() -> {
                    // Initialize sequence for this user if it doesn't exist
                    InvoiceSequence newSequence = new InvoiceSequence(user, 0);
                    return invoiceSequenceRepository.save(newSequence);
                });
        
        // Start from the last auto-generated sequence number
        int candidateNumber = sequence.getLastSequenceNumber() + 1;
        String candidateInvoiceNumber;
        
        // Keep incrementing until we find an available invoice number
        while (true) {
            candidateInvoiceNumber = String.format("INV-%03d", candidateNumber);
            
            // Check if this invoice number already exists for this user
            if (invoiceRepository.findByInvoiceNumberAndUserId(candidateInvoiceNumber, user.getId()).isEmpty()) {
                // Found an available number, update the sequence and return
                sequence.setLastSequenceNumber(candidateNumber);
                invoiceSequenceRepository.save(sequence);
                log.info("Generated available invoice number: {} (sequence: {}) for user: {}", 
                        candidateInvoiceNumber, candidateNumber, user.getEmail());
                return candidateInvoiceNumber;
            }
            
            // This number is taken, try the next one
            candidateNumber++;
            
            // Safety check to prevent infinite loop (though unlikely in practice)
            if (candidateNumber > 999999) {
                throw new RuntimeException("Unable to generate invoice number: sequence exhausted");
            }
        }
    }

    @Override
    public InvoiceResponse getInvoiceById(UUID id) {
        User currentUser = getCurrentUser();
        log.info("Fetching invoice with ID: {} for user: {}", id, currentUser.getEmail());
        
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));
        
        // Verify the invoice belongs to the current user
        if (!invoice.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Access denied: Invoice does not belong to current user");
        }
        Client client = clientRepository.findById(invoice.getClientId())
                .orElseThrow(()->new ResourceNotFoundException("client not found"));
        InvoiceSender sender = invoiceSenderRepository.findByInvoice(invoice.getId())
                .orElseThrow(()->new ResourceNotFoundException("Sender not found"));
        return mapToResponse(invoice,client,sender);
    }

    @Override
    public List<InvoiceResponse> getAllUserInvoices() {
        User currentUser = getCurrentUser();
        log.info("Fetching all invoices for user: {}", currentUser.getEmail());
        
        List<Invoice> invoices = invoiceRepository.findAllByUserId(currentUser.getId());
        return invoices.stream()
                .map(invoice -> {
                    Client client = clientRepository.findById(invoice.getClientId())
                            .orElseThrow(()->new ResourceNotFoundException("client not found"));
                    InvoiceSender sender = invoiceSenderRepository.findByInvoice(invoice.getId())
                            .orElseThrow(()->new ResourceNotFoundException("Sender not found"));
                    return mapToResponse(invoice,client,sender);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public InvoiceResponse updateInvoice(UUID id, CreateInvoiceRequest request) {
        User currentUser = getCurrentUser();
        log.info("Updating invoice with ID: {} for user: {}", id, currentUser.getEmail());

        Invoice existingInvoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));
        
        // Verify the invoice belongs to the current user
        if (!existingInvoice.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Access denied: Invoice does not belong to current user");
        }

        String oldLogoUrl = existingInvoice.getLogoUrl();
        String oldSignatureUrl = existingInvoice.getSignatureUrl();
        String oldInvoiceNumber = existingInvoice.getInvoiceNumber();
        
        // Update basic fields manually to avoid detached entity issues
        existingInvoice.setTitle(request.getTitle());
        existingInvoice.setInvoiceColor(request.getInvoiceColor());
        existingInvoice.setCreationDate(request.getInvoiceDate() != null ? request.getInvoiceDate().atStartOfDay() : null);
        existingInvoice.setDueDate(request.getDueDate() != null ? request.getDueDate().atStartOfDay() : null);
        existingInvoice.setPaymentTerms(request.getPaymentTerms());
        existingInvoice.setAccountNumber(request.getAccountNumber());
        existingInvoice.setAccountName(request.getAccountName());
        existingInvoice.setBank(request.getBank());
        existingInvoice.setCurrency(request.getCurrency());
        existingInvoice.setSubtotal(request.getSubtotal());
        existingInvoice.setTotalDue(request.getTotalDue());
        existingInvoice.setNote(request.getNote());
        existingInvoice.setTermsAndConditions(request.getTermsAndConditions());
        
        // Handle invoice items update manually to avoid detached entity issues
        if (request.getItems() != null) {
            // Clear existing items
            existingInvoice.getItems().clear();
            
            // Add new items
            for (InvoiceItemRequest itemRequest : request.getItems()) {
                InvoiceItem item = new InvoiceItem();
                item.setItemName(itemRequest.getItemName());
                item.setDescription(itemRequest.getDescription());
                item.setQuantity(itemRequest.getQuantity());
                item.setRate(itemRequest.getRate());
                item.setAmount(itemRequest.getAmount());
                item.setTax(itemRequest.getTax());
                
                // Set category if provided
                if (itemRequest.getCategory() != null) {
                    try {
                        item.setCategory(Item_Category.valueOf(itemRequest.getCategory().toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        log.warn("Invalid category '{}' for item '{}', skipping category", 
                                itemRequest.getCategory(), itemRequest.getItemName());
                    }
                }
                
                // Handle taxes if provided
                if (itemRequest.getTaxIds() != null && !itemRequest.getTaxIds().isEmpty()) {
                    for (UUID taxId : itemRequest.getTaxIds()) {
                        Tax tax = taxRepository.findById(taxId).orElse(null);
                        if (tax != null) {
                            InvoiceItemTax itemTax = new InvoiceItemTax();
                            itemTax.setTax(tax);
                            
                            // Calculate tax amount based on item amount and tax rate
                            if (item.getAmount() != null && tax.getBaseTaxRate() != null) {
                                BigDecimal taxableAmount = item.getAmount();
                                BigDecimal appliedRate = tax.getBaseTaxRate();
                                BigDecimal taxAmount = taxableAmount
                                        .multiply(appliedRate)
                                        .divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP);
                                
                                itemTax.setTaxableAmount(taxableAmount);
                                itemTax.setAppliedRate(appliedRate);
                                itemTax.setTaxAmount(taxAmount);
                            } else {
                                // Set default values if calculation not possible
                                itemTax.setTaxableAmount(BigDecimal.ZERO);
                                itemTax.setAppliedRate(BigDecimal.ZERO);
                                itemTax.setTaxAmount(BigDecimal.ZERO);
                            }
                            
                            item.addItemTax(itemTax);
                        }
                    }
                }
                
                // Add item to invoice using helper method to maintain bidirectional relationship
                existingInvoice.addItem(item);
            }
        }
        try {
            if (request.getLogo() != null && !request.getLogo().isEmpty()) {
                String newLogoUrl = cloudinaryService.uploadFile(request.getLogo());
                existingInvoice.setLogoUrl(newLogoUrl);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload updated logo", e);
        }

        try {
            if (request.getSignature() != null && !request.getSignature().isEmpty()) {
                String newSignatureUrl = cloudinaryService.uploadFile(request.getSignature());
                existingInvoice.setSignatureUrl(newSignatureUrl);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload updated signature", e);
        }
        
        // Handle invoice number update
        if (request.getInvoiceNumber() != null && !request.getInvoiceNumber().equals(oldInvoiceNumber)) {
            // Check if the new invoice number already exists for this user (excluding current invoice)
            invoiceRepository.findByInvoiceNumberAndUserId(request.getInvoiceNumber(), currentUser.getId())
                    .ifPresent(existingInv -> {
                        if (!existingInv.getId().equals(id)) {
                            throw new RuntimeException("Invoice number already exists: " + request.getInvoiceNumber());
                        }
                    });
            
            existingInvoice.setInvoiceNumber(request.getInvoiceNumber());
            log.info("Updated invoice number from {} to {} for user: {}", 
                    oldInvoiceNumber, request.getInvoiceNumber(), currentUser.getEmail());
        }

        Invoice updatedInvoice = invoiceRepository.save(existingInvoice);
        log.info("Invoice updated successfully with ID: {} for user: {}", updatedInvoice.getId(), currentUser.getEmail());

        if (request.getLogo() != null && !request.getLogo().isEmpty() && oldLogoUrl != null) {
            cloudinaryService.deleteFile(oldLogoUrl);
        }
        if (request.getSignature() != null && !request.getSignature().isEmpty() && oldSignatureUrl != null) {
            cloudinaryService.deleteFile(oldSignatureUrl);
        }

        // Get client and sender for response mapping
        Client client = clientRepository.findById(updatedInvoice.getClientId())
                .orElseThrow(() -> new ResourceNotFoundException("client not found"));
        InvoiceSender sender = invoiceSenderRepository.findByInvoice(updatedInvoice.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Sender not found"));
        
        return mapToResponse(updatedInvoice, client, sender);
    }

    @Override
    public void deleteInvoice(UUID id) {
        User currentUser = getCurrentUser();
        log.info("Deleting invoice with ID: {} for user: {}", id, currentUser.getEmail());
        
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));
        
        // Verify the invoice belongs to the current user
        if (!invoice.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Access denied: Invoice does not belong to current user");
        }

        if (invoice.getLogoUrl() != null) {
            cloudinaryService.deleteFile(invoice.getLogoUrl());
        }
        if (invoice.getSignatureUrl() != null) {
            cloudinaryService.deleteFile(invoice.getSignatureUrl());
        }
        
        invoiceRepository.deleteById(id);
        log.info("Invoice deleted successfully with ID: {} for user: {}", id, currentUser.getEmail());
    }

    @Override
    public List<InvoiceResponse> getAllInvoices() {
        List<Invoice> invoices = invoiceRepository.findAll();
        if(invoices.isEmpty())return List.of();
        return invoices.stream()
                .map(invoice -> {
                    Client client = clientRepository.findById(invoice.getClientId())
                            .orElseThrow(()->new ResourceNotFoundException("client not found"));
                    InvoiceSender sender = invoiceSenderRepository.findByInvoice(invoice.getId())
                            .orElseThrow(()->new ResourceNotFoundException("Sender not found"));
                    return mapToResponse(invoice,client,sender);
                })
                .collect(Collectors.toList());
    }
}