package com.indherco.postes.products;

import com.indherco.postes.products.dto.ProductRequest;
import com.indherco.postes.products.dto.ProductResponse;
import com.indherco.postes.users.dto.StatusRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<ProductResponse> findAll(@RequestParam(defaultValue = "false") boolean activeOnly) {
        return activeOnly ? productService.findActive() : productService.findAll();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('USUARIOS_ADMINISTRAR')")
    public ProductResponse create(@Valid @RequestBody ProductRequest request) {
        return productService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USUARIOS_ADMINISTRAR')")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return productService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('USUARIOS_ADMINISTRAR')")
    public ProductResponse setStatus(@PathVariable Long id, @RequestBody StatusRequest request) {
        return productService.setStatus(id, request.active());
    }
}
