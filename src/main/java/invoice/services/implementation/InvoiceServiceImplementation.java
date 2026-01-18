package invoice.services.implementation;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import invoice.data.constants.Invoice_Status;
import invoice.data.constants.NotificationType;
import invoice.services.NotificationService;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import invoice.config.CloudinaryService;
import invoice.dtos.request.CreateInvoiceRequest;
import invoice.dtos.response.CreateInvoiceResponse;
import invoice.services.InvoiceService;
import invoice.services.EmailService;
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
    private final InvoiceTaxRepository invoiceTaxRepository;
    private final InvoiceSenderRepository invoiceSenderRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;


    @Override
    @Transactional
    public InvoiceResponse createInvoice(CreateInvoiceRequest request) {
        User currentUser = getCurrentUser();
        
        // Fetch client data if clientId is provided (for populating billTo)
        Client client = null;
        if (request.getClientId() != null) {
            client = clientRepository.findById(request.getClientId())
                    .orElseThrow(() -> new ResourceNotFoundException("client not found"));
        }
        
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
        
        // Set default status to UNPAID for new invoices
        invoice.setStatus(Invoice_Status.UNPAID);
        
        // Handle invoice items manually to avoid detached entity issues
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            for (InvoiceItemRequest itemRequest : request.getItems()) {
                InvoiceItem item = new InvoiceItem();
                item.setItemName(itemRequest.getItemName());
                item.setDescription(itemRequest.getDescription());
                item.setQuantity(itemRequest.getQuantity());
                item.setRate(itemRequest.getRate());
                item.setAmount(itemRequest.getAmount());
                
                // Set category if provided
                if (itemRequest.getCategory() != null) {
                    try {
                        item.setCategory(Item_Category.valueOf(itemRequest.getCategory().toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        log.warn("Invalid category '{}' for item '{}', skipping category", 
                                itemRequest.getCategory(), itemRequest.getItemName());
                    }
                }
                
                // Add item to invoice using helper method to maintain bidirectional relationship
                invoice.addItem(item);
            }
        }
        
        // Handle invoice-level taxes if provided
        if (request.getTaxIds() != null && !request.getTaxIds().isEmpty()) {
            for (UUID taxId : request.getTaxIds()) {
                    Tax tax = taxRepository.findById(taxId).orElse(null);
                    if (tax != null) {
                        InvoiceTax invoiceTax = new InvoiceTax();
                        invoiceTax.setTax(tax);
                        
                        // Calculate tax amount based on subtotal
                        Double subtotal = invoice.calculateSubtotal();
                        if (subtotal != null && tax.getBaseTaxRate() != null) {
                            BigDecimal taxableAmount = BigDecimal.valueOf(subtotal);
                            // Use client-specific tax rate if client is available and has customerType, otherwise use base rate
                            BigDecimal appliedRate = (client != null && client.getCustomerType() != null) ? 
                                tax.getApplicableRate(client.getCustomerType()) : 
                                tax.getBaseTaxRate();
                            BigDecimal taxAmount = taxableAmount
                                    .multiply(appliedRate)
                                    .divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP);
                            
                            invoiceTax.setTaxableAmount(taxableAmount);
                            invoiceTax.setAppliedRate(appliedRate);
                            invoiceTax.setTaxAmount(taxAmount);
                        } else {
                            // Set default values if calculation not possible
                            invoiceTax.setTaxableAmount(BigDecimal.ZERO);
                            invoiceTax.setAppliedRate(BigDecimal.ZERO);
                            invoiceTax.setTaxAmount(BigDecimal.ZERO);
                        }
                        
                        invoice.addInvoiceTax(invoiceTax);
                    }
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
        
        // Create invoice recipient (Bill To) - populate from client data if available
        InvoiceRecipient recipient = new InvoiceRecipient();
        if (client != null) {
            recipient.setFullName(client.getFullName());
            recipient.setBusinessName(client.getBusinessName());
            recipient.setEmail(client.getEmail());
            recipient.setPhone(client.getPhone());
            // Skip address field since it's not essential for invoice creation
            // Handle null CustomerType safely
            if (client.getCustomerType() != null) {
                recipient.setCustomerType(client.getCustomerType().name()); // Convert enum to string
            } else {
                recipient.setCustomerType("INDIVIDUAL"); // Default value if null
            }
            recipient.setTitle(client.getTitle());
            recipient.setCountry(client.getCountry());
        }
        // Note: clientId is NOT stored in the invoice - only the client data is transferred
        
        invoice.setRecipient(recipient);
        Invoice savedInvoice = invoiceRepository.save(invoice);
        sender.setInvoice(savedInvoice);
        invoiceSenderRepository.save(sender);
        
        // Send invoice notification email to recipient
        try {
            if (recipient.getEmail() != null) {
                String paymentUrl = "https://originalinvoice.com/customer/invoice/" + savedInvoice.getId().toString();
                emailService.sendInvoiceNotificationEmail(
                        recipient.getEmail(),
                        currentUser.getFullName(),
                        savedInvoice.getId().toString(),
                        paymentUrl, // Use the payment URL with invoice UUID
                        savedInvoice.getInvoiceNumber(),
                        savedInvoice.getCreationDate() != null ? savedInvoice.getCreationDate().toString() : "N/A",
                        savedInvoice.getDueDate() != null ? savedInvoice.getDueDate().toString() : "N/A",
                        savedInvoice.getTotalDue() != null ? savedInvoice.getTotalDue().toString() : "0.00",
                        recipient.getFullName()
                );
                log.info("Invoice notification email sent to {} for invoice {} with payment URL: {}", 
                        recipient.getEmail(), savedInvoice.getInvoiceNumber(), paymentUrl);
            }
        } catch (Exception e) {
            log.warn("Failed to send invoice notification email: {}", e.getMessage());
            // Don't throw exception - invoice creation should succeed even if email fails
        }
        
        // Create notification for invoice sent
        try {
            notificationService.createNotification(
                currentUser,
                "Invoice sent",
                "Your invoice to " + recipient.getFullName() + " was delivered successfully and is now viewable.",
                NotificationType.INVOICE_CREATED,
                savedInvoice.getId(),
                "INVOICE"
            );
        } catch (Exception e) {
            log.warn("Failed to create invoice sent notification: {}", e.getMessage());
        }
        
        return mapToResponse(savedInvoice, client, sender);
    }


    private InvoiceResponse mapToResponse(Invoice savedInvoice, Client client, InvoiceSender sender) {
        InvoiceResponse response = new InvoiceResponse(savedInvoice);
        
        try {
            // Safe mapping for recipient (billTo) - use InvoiceRecipient if available, fallback to Client
            if (savedInvoice.getRecipient() != null) {
                ClientResponse billTo = new ClientResponse(savedInvoice.getRecipient());
                response.setBillTo(billTo);
            } else if (client != null) {
                // Fallback to client data (for backward compatibility during transition)
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
        // Since clientId was removed from Invoice model, we pass null for client
        // The mapToResponse method will use InvoiceRecipient data instead
        InvoiceSender sender = invoiceSenderRepository.findByInvoice(invoice.getId())
                .orElseThrow(()->new ResourceNotFoundException("Sender not found"));
        return mapToResponse(invoice, null, sender);
    }

    @Override
    public List<InvoiceResponse> getAllUserInvoices() {
        User currentUser = getCurrentUser();
        log.info("Fetching all invoices for user: {}", currentUser.getEmail());
        
        List<Invoice> invoices = invoiceRepository.findAllByUserId(currentUser.getId());
        return invoices.stream()
                .map(invoice -> {
                    // Since clientId was removed from Invoice model, we pass null for client
                    // The mapToResponse method will use InvoiceRecipient data instead
                    InvoiceSender sender = invoiceSenderRepository.findByInvoice(invoice.getId())
                            .orElseThrow(()->new ResourceNotFoundException("Sender not found"));
                    return mapToResponse(invoice, null, sender);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<InvoiceResponse> getAllUserInvoices(UUID userId) {
        log.info("Fetching all invoices for user ID: {}", userId);
        
        // First find the user by ID
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        
        // Then find all invoices related to that user
        List<Invoice> invoices = invoiceRepository.findAllByUserId(user.getId());
        return invoices.stream()
                .map(invoice -> {
                    // Since clientId was removed from Invoice model, we pass null for client
                    // The mapToResponse method will use InvoiceRecipient data instead
                    InvoiceSender sender = invoiceSenderRepository.findByInvoice(invoice.getId())
                            .orElseThrow(()->new ResourceNotFoundException("Sender not found"));
                    return mapToResponse(invoice, null, sender);
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
                
                // Set category if provided
                if (itemRequest.getCategory() != null) {
                    try {
                        item.setCategory(Item_Category.valueOf(itemRequest.getCategory().toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        log.warn("Invalid category '{}' for item '{}', skipping category", 
                                itemRequest.getCategory(), itemRequest.getItemName());
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

        // Create notification for invoice update
        try {
            notificationService.createNotification(
                currentUser,
                "Invoice Updated",
                "Invoice " + updatedInvoice.getInvoiceNumber() + " has been updated successfully",
                NotificationType.INVOICE_UPDATED,
                updatedInvoice.getId(),
                "INVOICE"
            );
        } catch (Exception e) {
            log.error("Failed to create notification for invoice update: {}", e.getMessage());
        }

        if (request.getLogo() != null && !request.getLogo().isEmpty() && oldLogoUrl != null) {
            cloudinaryService.deleteFile(oldLogoUrl);
        }
        if (request.getSignature() != null && !request.getSignature().isEmpty() && oldSignatureUrl != null) {
            cloudinaryService.deleteFile(oldSignatureUrl);
        }

        // Get sender for response mapping (client data is in InvoiceRecipient now)
        InvoiceSender sender = invoiceSenderRepository.findByInvoice(updatedInvoice.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Sender not found"));
        
        return mapToResponse(updatedInvoice, null, sender);
    }

    @Override
    @Transactional
    public void deleteInvoice(UUID id) {
        User currentUser = getCurrentUser();
        log.info("Deleting invoice with ID: {} for user: {}", id, currentUser.getEmail());
        
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));
        
        // Verify the invoice belongs to the current user
        if (!invoice.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Access denied: Invoice does not belong to current user");
        }

        // Store invoice number for notification before deletion
        String invoiceNumber = invoice.getInvoiceNumber();

        // Delete related entities first to avoid foreign key constraint violations
        
        // 1. Delete invoice sender
        invoiceSenderRepository.findByInvoice(id).ifPresent(sender -> {
            log.info("Deleting invoice sender for invoice: {}", id);
            invoiceSenderRepository.delete(sender);
        });
        
        // 2. Delete invoice taxes
        List<InvoiceTax> invoiceTaxes = invoiceTaxRepository.findByInvoiceId(id);
        if (!invoiceTaxes.isEmpty()) {
            log.info("Deleting {} invoice taxes for invoice: {}", invoiceTaxes.size(), id);
            invoiceTaxRepository.deleteAll(invoiceTaxes);
        }
        
        // 3. Delete invoice items (should be handled by cascade, but let's be explicit)
        if (invoice.getItems() != null && !invoice.getItems().isEmpty()) {
            log.info("Clearing {} invoice items for invoice: {}", invoice.getItems().size(), id);
            invoice.getItems().clear();
        }

        // 4. Delete uploaded files from Cloudinary
        if (invoice.getLogoUrl() != null) {
            try {
                cloudinaryService.deleteFile(invoice.getLogoUrl());
                log.info("Deleted logo file for invoice: {}", id);
            } catch (Exception e) {
                log.warn("Failed to delete logo file for invoice {}: {}", id, e.getMessage());
            }
        }
        
        if (invoice.getSignatureUrl() != null) {
            try {
                cloudinaryService.deleteFile(invoice.getSignatureUrl());
                log.info("Deleted signature file for invoice: {}", id);
            } catch (Exception e) {
                log.warn("Failed to delete signature file for invoice {}: {}", id, e.getMessage());
            }
        }
        
        // 5. Finally delete the invoice itself
        invoiceRepository.deleteById(id);
        log.info("Invoice deleted successfully with ID: {} for user: {}", id, currentUser.getEmail());
        
        // Create notification for invoice deletion
        try {
            notificationService.createNotification(
                currentUser,
                "Invoice Deleted",
                "Invoice " + invoiceNumber + " has been deleted successfully",
                NotificationType.INVOICE_DELETED,
                id,
                "INVOICE"
            );
        } catch (Exception e) {
            log.error("Failed to create notification for invoice deletion: {}", e.getMessage());
        }
    }

    @Override
    public List<InvoiceResponse> getAllInvoices() {
        List<Invoice> invoices = invoiceRepository.findAll();
        if(invoices.isEmpty())return List.of();
        return invoices.stream()
                .map(invoice -> {
                    // Since clientId was removed from Invoice model, we pass null for client
                    // The mapToResponse method will use InvoiceRecipient data instead
                    InvoiceSender sender = invoiceSenderRepository.findByInvoice(invoice.getId())
                            .orElseThrow(()->new ResourceNotFoundException("Sender not found"));
                    return mapToResponse(invoice, null, sender);
                })
                .collect(Collectors.toList());
    }

    @Override
    public InvoiceResponse getInvoiceByUuid(UUID uuid) {
        log.info("Fetching invoice with UUID: {} for public access", uuid);
        
        Invoice invoice = invoiceRepository.findById(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
        
        InvoiceSender sender = invoiceSenderRepository.findByInvoice(invoice.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Sender not found"));
        
        return mapToResponse(invoice, null, sender);
    }

    @Override
    @Transactional
    public InvoiceResponse uploadPaymentEvidence(UUID invoiceUuid, MultipartFile evidenceFile) {
        log.info("Uploading payment evidence for invoice UUID: {}", invoiceUuid);
        
        // Find the invoice by UUID
        Invoice invoice = invoiceRepository.findById(invoiceUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
        
        // Upload the evidence file to Cloudinary
        String evidenceUrl;
        try {
            evidenceUrl = cloudinaryService.uploadFile(evidenceFile);
            log.info("Evidence file uploaded successfully: {}", evidenceUrl);
        } catch (IOException e) {
            log.error("Failed to upload evidence file: {}", e.getMessage());
            throw new RuntimeException("Failed to upload evidence file", e);
        }
        
        // Update invoice status to PENDING (evidence URL will be stored separately if needed)
        invoice.setStatus(Invoice_Status.PENDING);
        
        Invoice updatedInvoice = invoiceRepository.save(invoice);
        log.info("Invoice status updated to PENDING for invoice: {}", invoiceUuid);
        
        // Get invoice sender and recipient for notifications
        InvoiceSender sender = invoiceSenderRepository.findByInvoice(invoice.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Sender not found"));
        
        User invoiceSender = invoice.getUser();
        String customerName = invoice.getRecipient() != null ? 
                invoice.getRecipient().getFullName() : "Customer";
        
        // Send in-app notification to invoice sender
        try {
            notificationService.createNotification(
                invoiceSender,
                "Payment Evidence Uploaded",
                "Customer " + customerName + " has uploaded proof of payment for Invoice " + invoice.getInvoiceNumber(),
                NotificationType.PAYMENT_EVIDENCE_UPLOADED,
                invoice.getId(),
                "INVOICE"
            );
            log.info("In-app notification sent to invoice sender: {}", invoiceSender.getEmail());
        } catch (Exception e) {
            log.warn("Failed to create in-app notification: {}", e.getMessage());
        }
        
        // Send email notification to invoice sender
        try {
            String dashboardUrl = "https://myapp.com/dashboard/invoices/" + invoice.getId();
            emailService.sendPaymentEvidenceNotificationEmail(
                invoiceSender.getEmail(),
                invoiceSender.getFullName(),
                invoice.getInvoiceNumber(),
                customerName,
                dashboardUrl
            );
            log.info("Email notification sent to invoice sender: {}", invoiceSender.getEmail());
        } catch (Exception e) {
            log.warn("Failed to send email notification: {}", e.getMessage());
        }
        
        return mapToResponse(updatedInvoice, null, sender);
    }

    @Override
    public Map<String, Long> getInvoiceStats(String email) {
        log.info("Fetching invoice statistics for recipient email: {}", email);
        
        // Get all invoices sent to this email address
        List<Invoice> invoices = invoiceRepository.findAllByRecipientEmail(email);
        
        // Get current date/time for overdue calculation
        LocalDateTime now = LocalDateTime.now();
        
        // Calculate statistics
        long totalReceived = invoices.size();
        
        long paid = invoices.stream()
                .filter(invoice -> invoice.getStatus() == Invoice_Status.PAID)
                .count();
        
        long overdue = invoices.stream()
                .filter(invoice -> 
                    invoice.getStatus() == Invoice_Status.OVERDUE || 
                    (invoice.getDueDate() != null && 
                     invoice.getDueDate().isBefore(now) && 
                     invoice.getStatus() != Invoice_Status.PAID))
                .count();
        
        long pending = invoices.stream()
                .filter(invoice -> invoice.getStatus() == Invoice_Status.PENDING)
                .count();
        
        long unpaid = invoices.stream()
                .filter(invoice -> 
                    invoice.getStatus() == Invoice_Status.UNPAID || 
                    invoice.getStatus() == Invoice_Status.OUTSTANDING)
                .count();
        
        // Build response map
        Map<String, Long> stats = new HashMap<>();
        stats.put("totalReceived", totalReceived);
        stats.put("paid", paid);
        stats.put("overdue", overdue);
        stats.put("pending", pending);
        stats.put("unpaid", unpaid);
        
        log.info("Invoice stats for recipient {}: Total={}, Paid={}, Overdue={}, Pending={}, Unpaid={}", 
                email, totalReceived, paid, overdue, pending, unpaid);
        
        return stats;
    }
}