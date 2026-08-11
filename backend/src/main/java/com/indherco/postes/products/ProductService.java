package com.indherco.postes.products;

import com.indherco.postes.products.dto.ProductRequest;
import com.indherco.postes.products.dto.ProductResponse;
import com.indherco.postes.shared.exception.ApiException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public ProductService(ProductRepository productRepository, ProductMapper productMapper) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
    }

    public List<ProductResponse> findAll() {
        return productRepository.findAll().stream().map(productMapper::toResponse).toList();
    }

    public List<ProductResponse> findActive() {
        return productRepository.findByActiveTrueOrderByNameAsc().stream().map(productMapper::toResponse).toList();
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        Product product = new Product();
        apply(product, request, true);
        return productMapper.toResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Producto no encontrado."));
        apply(product, request, false);
        return productMapper.toResponse(product);
    }

    @Transactional
    public ProductResponse setStatus(Long id, boolean active) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Producto no encontrado."));
        product.setActive(active);
        return productMapper.toResponse(product);
    }

    private void apply(Product product, ProductRequest request, boolean allowInitialStock) {
        product.setName(request.name());
        product.setType(request.type());
        product.setUnitOfMeasure(request.unitOfMeasure());
        product.setMinimumStock(request.minimumStock());
        if (allowInitialStock) {
            product.setCurrentStock(request.currentStock() == null ? 0 : request.currentStock());
        }
    }
}
