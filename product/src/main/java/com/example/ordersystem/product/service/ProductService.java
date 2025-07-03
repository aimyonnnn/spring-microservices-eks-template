package com.example.ordersystem.product.service;

import com.example.ordersystem.product.domain.Product;
import com.example.ordersystem.product.dto.ProductRegisterDto;
import com.example.ordersystem.product.dto.ProductResponseDto;
import com.example.ordersystem.product.dto.ProductUpdateStockDto;
import com.example.ordersystem.product.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Product productCreate(
            ProductRegisterDto ProductRegisterDto,
            String userId
    ) {

        Product product = productRepository.save(
                ProductRegisterDto.toEntity(Long.parseLong(userId)));

        return product;
    }

    public ProductResponseDto productDetail(Long id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("상품이 존재하지 않습니다."));

        return ProductResponseDto.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .build();
    }

    public Product updateStockQuantity(ProductUpdateStockDto productUpdateStockDto) {

        Product product = productRepository.findById(productUpdateStockDto.getProductId())
                .orElseThrow(() -> new EntityNotFoundException("상품이 존재하지 않습니다."));

        product.updateStockQuantity(productUpdateStockDto.getProductQuantity());

        return product;
    }
}
