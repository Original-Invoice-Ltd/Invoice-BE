package invoice.services.implementation;

import invoice.data.models.Tax;
import invoice.data.repositories.TaxRepository;
import invoice.dtos.request.TaxRequest;
import invoice.dtos.response.TaxResponse;
import invoice.exception.ResourceNotFoundException;
import invoice.services.TaxService;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class TaxServiceImpl implements TaxService {
    private final TaxRepository taxRepository;
    private final ModelMapper modelMapper;

    @Override
    public TaxResponse addTax(TaxRequest request) {
        Tax tax = modelMapper.map(request, Tax.class);
        taxRepository.save(tax);
        return modelMapper.map(tax, TaxResponse.class);
    }

    @Override
    public TaxResponse updateTax(UUID id, TaxRequest request) {
        Tax tax = getTax(id);
        modelMapper.map(request, tax);
        taxRepository.save(tax);
        return modelMapper.map(tax, TaxResponse.class);
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
        if(taxes.isEmpty())return List.of();
        return taxes.stream().map(TaxResponse::new)
                .collect(Collectors.toList());
    }

    @Override
    public String deleteById(UUID id) {

        Tax tax = getTax(id);
        taxRepository.delete(tax);
        return "tax deleted";
    }

    @Override
    public String deleteAll() {
        taxRepository.deleteAll();
        return "all taxes deleted";
    }
}
