package invoice.services.implementation;

import invoice.data.constants.Item_Category;
import invoice.data.models.Product;
import invoice.data.models.User;
import invoice.data.repositories.ProductRepository;
import invoice.dtos.request.ProductRequest;
import invoice.dtos.response.ProductResponse;
import invoice.exception.ResourceNotFoundException;
import invoice.services.ProductService;
import invoice.services.UserService;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;
    private final ModelMapper modelMapper;
    private final UserService userService;

    @Override
    public ProductResponse addProduct(String email, ProductRequest productRequest) {
        User user = userService.getUserByEmail(email);
        Product product = modelMapper.map(productRequest, Product.class);
        
        // Set category enum
        if (productRequest.getCategory() != null) {
            product.setCategory(Item_Category.valueOf(productRequest.getCategory().toUpperCase()));
        }
        
        product.setUser(user);
        Product savedProduct = productRepository.save(product);
        return new ProductResponse(savedProduct);
    }

    @Override
    public ProductResponse updateProduct(UUID id, ProductRequest productRequest) {
        Product product = findProduct(id);
        User user = product.getUser();
        
        // Map basic fields
        modelMapper.map(productRequest, product);
        
        // Set category enum
        if (productRequest.getCategory() != null) {
            product.setCategory(Item_Category.valueOf(productRequest.getCategory().toUpperCase()));
        }
        
        product.setUser(user);
        Product savedProduct = productRepository.save(product);
        return new ProductResponse(savedProduct);
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
    @Transactional
    public String deleteProduct(UUID id) {
        Product product = findProduct(id);
        
        try {
            // Check for any references before deletion
            // The error suggests there's a foreign key from _taxes to _user_products
            // This might be a schema inconsistency that needs to be resolved
            
            productRepository.delete(product);
            return "product deleted successfully";
        } catch (DataIntegrityViolationException e) {
            // Handle foreign key constraint violations
            String errorMessage = e.getMessage();
            if (errorMessage.contains("_taxes") && errorMessage.contains("_user_products")) {
                // This specific error suggests a schema issue where taxes table 
                // has a foreign key to products table that shouldn't exist
                throw new RuntimeException("Cannot delete product: Database schema issue detected. " +
                    "The taxes table appears to have an unexpected reference to this product. " +
                    "Please check the database schema and remove any invalid foreign key constraints between _taxes and _user_products tables.");
            } else if (errorMessage.contains("foreign key constraint")) {
                throw new RuntimeException("Cannot delete product: It is referenced by other records. " +
                    "Please remove all references to this product before deleting it.");
            }
            throw new RuntimeException("Failed to delete product: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error while deleting product: " + e.getMessage());
        }
    }
    
    /**
     * Helper method to check if a product can be safely deleted
     * This method can be expanded to check various relationships
     */
    public boolean canDeleteProduct(UUID productId) {
        try {
            // Add checks here for any known relationships
            // For now, we'll rely on the database constraints to catch issues
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Product findProduct(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("product not found"));
    }
}