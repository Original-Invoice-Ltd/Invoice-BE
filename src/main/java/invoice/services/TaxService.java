package invoice.services;

import invoice.data.constants.CustomerType;
import invoice.data.constants.TaxType;
import invoice.dtos.request.TaxRequest;
import invoice.dtos.response.TaxResponse;

import java.util.List;
import java.util.UUID;

public interface TaxService {
    TaxResponse addTax(TaxRequest request);

    TaxResponse updateTax(UUID id, TaxRequest request);

    TaxResponse findById(UUID id);

    List<TaxResponse> findAll();
    
    List<TaxResponse> findActiveTaxes();
    
    List<TaxResponse> findByTaxType(TaxType taxType);
    
    List<TaxResponse> findApplicableTaxes(CustomerType clientType);

    String deleteById(UUID id);

    String deleteAll();
    
    void initializeDefaultTaxes();
}
