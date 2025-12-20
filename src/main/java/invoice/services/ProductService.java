package invoice.services;

import invoice.dtos.request.ProductRequest;
import invoice.dtos.response.ProductResponse;

import java.util.List;
import java.util.UUID;

public interface ProductService {
    String addProduct(String email, ProductRequest productRequest);

    String updateProduct(UUID id, ProductRequest productRequest);

    ProductResponse getProduct(UUID id);

    List<ProductResponse> getAllProducts();

    List<ProductResponse> getAllUserProducts(String email);

    List<ProductResponse> getProductsByIds(List<UUID> ids, String email);

    String deleteAllUserProducts(String email);

    String deleteAllProducts();

    String deleteProduct(UUID id);
}