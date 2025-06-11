package com.example.order.service.order.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {

    private Long userId;
    private Long productid;
    private int quantity;
}
