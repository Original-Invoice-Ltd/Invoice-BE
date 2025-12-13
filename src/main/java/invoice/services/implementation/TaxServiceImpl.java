package invoice.services.implementation;

import invoice.data.constants.CustomerType;
import invoice.data.constants.TaxType;
import invoice.data.models.Tax;
import invoice.data.repositories.TaxRepository;
import invoice.dtos.request.TaxRequest;
import invoice.dtos.response.TaxResponse;
import invoice.exception.ResourceNotFoundException;
import invoice.services.TaxService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class TaxServiceImpl implements TaxService {
    private final TaxRepository taxRepository;
    private final ModelMapper modelMapper;

    @Override
    public TaxResponse addTax(TaxRequest request) {
        Tax tax = modelMapper.map(request, Tax.class);
        taxRepository.save(tax);
        log.info("Created new tax: {} ({})", tax.getName(), tax.getTaxType());
        return new TaxResponse(tax);
    }

    @Override
    public TaxResponse updateTax(UUID id, TaxRequest request) {
        Tax tax = getTax(id);
        modelMapper.map(request, tax);
        taxRepository.save(tax);
        log.info("Updated tax: {} ({})", tax.getName(), tax.getTaxType());
        return new TaxResponse(tax);
    }

    private Tax getTax(UUID id) {
        return taxRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tax not found"));
    }

    @Override
    public TaxResponse findById(UUID id) {
        Tax tax = getTax(id);
        return new TaxResponse(tax);
    }

    @Override
    public List<TaxResponse> findAll() {
        List<Tax> taxes = taxRepository.findAll();
        if(taxes.isEmpty()) return List.of();
        return taxes.stream().map(TaxResponse::new)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<TaxResponse> findActiveTaxes() {
        List<Tax> taxes = taxRepository.findByIsActiveTrue();
        return taxes.stream().map(TaxResponse::new)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<TaxResponse> findByTaxType(TaxType taxType) {
        List<Tax> taxes = taxRepository.findByTaxTypeAndIsActiveTrue(taxType);
        return taxes.stream().map(TaxResponse::new)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<TaxResponse> findApplicableTaxes(CustomerType clientType) {
        List<Tax> allActiveTaxes = taxRepository.findByIsActiveTrue();
        List<Tax> applicableTaxes = allActiveTaxes.stream()
                .filter(tax -> {
                    BigDecimal applicableRate = tax.getApplicableRate(clientType);
                    return applicableRate != null && applicableRate.compareTo(BigDecimal.ZERO) > 0;
                })
                .collect(Collectors.toList());
        
        return applicableTaxes.stream().map(TaxResponse::new)
                .collect(Collectors.toList());
    }

    @Override
    public String deleteById(UUID id) {
        Tax tax = getTax(id);
        taxRepository.delete(tax);
        log.info("Deleted tax: {} ({})", tax.getName(), tax.getTaxType());
        return "tax deleted";
    }

    @Override
    public String deleteAll() {
        taxRepository.deleteAll();
        log.warn("Deleted all taxes");
        return "all taxes deleted";
    }
    
    @Override
    public void initializeDefaultTaxes() {
        // Check if taxes already exist
        if (!taxRepository.findAll().isEmpty()) {
            log.info("Taxes already exist, skipping initialization");
            return;
        }
        
        // Create WHT tax
        Tax whtTax = new Tax();
        whtTax.setName("Withholding Tax");
        whtTax.setTaxType(TaxType.WHT);
        whtTax.setDescription("Withholding Tax - varies by client type");
        whtTax.setIndividualRate(BigDecimal.valueOf(5.0)); // 5% for individuals
        whtTax.setBusinessRate(BigDecimal.valueOf(10.0));   // 10% for businesses
        whtTax.setBaseTaxRate(BigDecimal.valueOf(5.0));     // Default rate
        whtTax.setActive(true);
        taxRepository.save(whtTax);
        
        // Create VAT tax
        Tax vatTax = new Tax();
        vatTax.setName("Value Added Tax");
        vatTax.setTaxType(TaxType.VAT);
        vatTax.setDescription("Value Added Tax - 7.5% for all clients");
        vatTax.setIndividualRate(BigDecimal.valueOf(7.5));  // 7.5% for individuals
        vatTax.setBusinessRate(BigDecimal.valueOf(7.5));    // 7.5% for businesses
        vatTax.setBaseTaxRate(BigDecimal.valueOf(7.5));     // Default rate
        vatTax.setActive(true);
        taxRepository.save(vatTax);
        
        log.info("Initialized default taxes: WHT and VAT");
    }
}
