package com.example.ordersystem.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class ProductResponseDto {
    private Long id;
    private String name;
    private int price;
    private int stockQuantity;
}
