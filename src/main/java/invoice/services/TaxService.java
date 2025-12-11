package invoice.services;

import invoice.dtos.request.TaxRequest;
import invoice.dtos.response.TaxResponse;

import java.util.List;
import java.util.UUID;

public interface TaxService {
    TaxResponse addTax(TaxRequest request);

    TaxResponse updateTax(UUID id, TaxRequest request);

    TaxResponse findById(UUID id);

    List<TaxResponse> findAll();

    String deleteById(UUID id);

    String deleteAll();
}
