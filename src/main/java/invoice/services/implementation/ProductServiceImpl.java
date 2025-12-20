package invoice.services.implementation;

import invoice.data.constants.Item_Category;
import invoice.data.models.Product;
import invoice.data.models.Tax;
import invoice.data.models.User;
import invoice.data.repositories.ProductRepository;
import invoice.data.repositories.TaxRepository;
import invoice.dtos.request.ProductRequest;
import invoice.dtos.response.ProductResponse;
import invoice.exception.ResourceNotFoundException;
import invoice.services.ProductService;
import invoice.services.UserService;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;
    private final ModelMapper modelMapper;
    private final UserService userService;
    private final TaxRepository taxRepository;

    @Override
    public String addProduct(String email, ProductRequest productRequest) {
        User user = userService.getUserByEmail(email);
        Product product = modelMapper.map(productRequest, Product.class);
        
        // Set category enum
        if (productRequest.getCategory() != null) {
            product.setCategory(Item_Category.valueOf(productRequest.getCategory().toUpperCase()));
        }
        
        // Set taxes if provided
        if (productRequest.getTaxIds() != null && !productRequest.getTaxIds().isEmpty()) {
            List<Tax> taxes = taxRepository.findAllById(productRequest.getTaxIds());
            product.setTaxes(taxes);
        }
        
        product.setUser(user);
        productRepository.save(product);
        return "product added successfully";
    }

    @Override
    public String updateProduct(UUID id, ProductRequest productRequest) {
        Product product = findProduct(id);
        User user = product.getUser();
        
        // Map basic fields
        modelMapper.map(productRequest, product);
        
        // Set category enum
        if (productRequest.getCategory() != null) {
            product.setCategory(Item_Category.valueOf(productRequest.getCategory().toUpperCase()));
        }
        
        // Update taxes if provided
        if (productRequest.getTaxIds() != null) {
            if (productRequest.getTaxIds().isEmpty()) {
                product.setTaxes(new ArrayList<>());
            } else {
                List<Tax> taxes = taxRepository.findAllById(productRequest.getTaxIds());
                product.setTaxes(taxes);
            }
        }
        
        product.setUser(user);
        productRepository.save(product);
        return "product updated successfully";
    }

    @Override
    public ProductResponse getProduct(UUID id) {
        Product product = findProduct(id);
        return new ProductResponse(product);
    }

    @Override
    public List<ProductResponse> getAllProducts() {
        List<Product> products = productRepository.findAll();
        if (products.isEmpty()) return List.of();
        return products.stream()
                .map(ProductResponse::new)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductResponse> getAllUserProducts(String email) {
        List<Product> products = productRepository.findAllByUserEmail(email);
        if (products.isEmpty()) return List.of();
        return products.stream()
                .map(ProductResponse::new)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductResponse> getProductsByIds(List<UUID> ids, String email) {
        List<Product> products = productRepository.findByIdsAndUserEmail(ids, email);
        return products.stream()
                .map(ProductResponse::new)
                .collect(Collectors.toList());
    }

    @Override
    public String deleteAllUserProducts(String email) {
        List<Product> products = productRepository.findAllByUserEmail(email);
        if (!products.isEmpty()) productRepository.deleteAll(products);
        return "products deleted successfully";
    }

    @Override
    public String deleteAllProducts() {
        List<Product> products = productRepository.findAll();
        if (!products.isEmpty()) productRepository.deleteAll(products);
        return "products deleted successfully";
    }

    @Override
    public String deleteProduct(UUID id) {
        Product product = findProduct(id);
        productRepository.delete(product);
        return "product deleted successfully";
    }

    private Product findProduct(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("product not found"));
    }
}