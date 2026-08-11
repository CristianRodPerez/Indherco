package com.indherco.postes.products;

import com.indherco.postes.products.dto.ProductResponse;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public ProductResponse toResponse(Product product) {
        return new ProductResponse(
            product.getId(),
            product.getName(),
            product.getType(),
            product.getUnitOfMeasure(),
            product.getCurrentStock(),
            product.getMinimumStock(),
            product.isActive()
        );
    }
}
