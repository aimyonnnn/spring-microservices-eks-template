package com.example.ordersystem.product.service;

import com.example.ordersystem.product.domain.Product;
import com.example.ordersystem.product.dto.ProductRegisterDto;
import com.example.ordersystem.product.dto.ProductResponseDto;
import com.example.ordersystem.product.dto.ProductUpdateStockDto;
import com.example.ordersystem.product.repository.ProductRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.jboss.logging.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

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

        return productRepository.save(
                ProductRegisterDto.toEntity(Long.parseLong(userId)));
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

    @KafkaListener(topics = "update-stock-topic", containerFactory = "kafkaListener")
    public void stockConsumer(String message) {

        log.info("재고 업데이트 요청 수신: {}", message);

        ObjectMapper objectMapper = new ObjectMapper();
        ProductUpdateStockDto productUpdateStockDto = null;

        try {
            productUpdateStockDto = objectMapper.readValue(message, ProductUpdateStockDto.class);
            updateStockQuantity(productUpdateStockDto);
            log.info("재고 업데이트 완료: ProductId={}", productUpdateStockDto.getProductId());

        } catch (JsonProcessingException e) {
            log.error("JSON 파싱 실패: {}", e.getMessage());
            throw new RuntimeException("JSON 파싱 처리에 실패하였습니다.", e);

        } catch (Exception e) {
            log.error("재고 업데이트 실패: {}", e.getMessage());
            throw new RuntimeException("재고 업데이트 처리에 실패하였습니다.", e);
        }
    }

}
