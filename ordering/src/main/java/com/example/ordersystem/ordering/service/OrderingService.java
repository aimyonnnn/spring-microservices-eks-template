package com.example.ordersystem.ordering.service;

import com.example.ordersystem.ordering.domain.Ordering;
import com.example.ordersystem.ordering.dto.OrderCreateDto;
import com.example.ordersystem.ordering.dto.ProductDto;
import com.example.ordersystem.ordering.dto.ProductUpdateStockDto;
import com.example.ordersystem.ordering.repository.OrderingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

@Service
@Transactional
@Slf4j
public class OrderingService {

    private final OrderingRepository orderingRepository;
    private final RestTemplate restTemplate;
    private final ProductFeign productFeign;
//    private final KafkaTemplate<String, Object> kafkaTemplate;

    public OrderingService(
            OrderingRepository orderingRepository,
            RestTemplate restTemplate,
            ProductFeign productFeign
//            KafkaTemplate<String, Object> kafkaTemplate
    ) {

        this.orderingRepository = orderingRepository;
        this.restTemplate = restTemplate;
        this.productFeign = productFeign;
//        this.kafkaTemplate = kafkaTemplate;
    }

    // restTemplate
    public Ordering orderCreate(OrderCreateDto OrderCreateDto, String userId) {

        String productGetUrl = "http://product-service/product/" + OrderCreateDto.getProductId();

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set("X-User-Id", userId);
        HttpEntity<String> httpEntity = new HttpEntity<>(httpHeaders);

        ResponseEntity<ProductDto> response
                = restTemplate.exchange(productGetUrl, HttpMethod.GET, httpEntity, ProductDto.class);

        ProductDto productDto = response.getBody();
        int requestedQuantity = OrderCreateDto.getProductCount();

        if(productDto.getStockQuantity() < requestedQuantity){
           throw new IllegalArgumentException(
                    "재고가 부족합니다. 요청 수량: " + requestedQuantity + ", 현재 재고: " + productDto.getStockQuantity());
        }

//        주문 생성
        Ordering ordering = Ordering.builder()
                .memberId(Long.parseLong(userId))
                .productId(OrderCreateDto.getProductId())
                .quantity(OrderCreateDto.getProductCount())
                .build();

        orderingRepository.save(ordering);

//        재고 수량 업데이트 요청
        String productPutUrl = "http://product-service/product/updatestock";
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ProductUpdateStockDto> updateEntity = new HttpEntity<>(
                ProductUpdateStockDto.builder()
                        .productId(OrderCreateDto.getProductId())
                        .productQuantity(OrderCreateDto.getProductCount())
                        .build(),
                httpHeaders
        );

        restTemplate.exchange(productPutUrl, HttpMethod.PUT, updateEntity, Void.class);

        return  ordering;
    }

    // FeignClient + KafkaTemplate
    public Ordering orderFeignKafkaCreate(OrderCreateDto OrderCreateDto, String userId) {

        ProductDto productDto = productFeign.getProductById(OrderCreateDto.getProductId(), userId);

        int requestedQuantity = OrderCreateDto.getProductCount();

        if(productDto.getStockQuantity() < requestedQuantity){
            throw new IllegalArgumentException(
                    "재고가 부족합니다. 요청 수량: " + requestedQuantity + ", 현재 재고: " + productDto.getStockQuantity());
        }

//        주문 생성
        Ordering ordering = Ordering.builder()
                .memberId(Long.parseLong(userId))
                .productId(OrderCreateDto.getProductId())
                .quantity(OrderCreateDto.getProductCount())
                .build();

        orderingRepository.save(ordering);

//        재고 수량 업데이트 요청
        productFeign.updateProductStock(
                ProductUpdateStockDto.builder()
                        .productId(OrderCreateDto.getProductId())
                        .productQuantity(OrderCreateDto.getProductCount())
                        .build()
        );

        return  ordering;
    }

}
