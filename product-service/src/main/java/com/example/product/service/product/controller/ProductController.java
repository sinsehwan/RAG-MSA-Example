package com.example.product.service.product.controller;

import com.example.product.service.product.entity.Product;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
public class ProductController {
    private final Map<Long, Product> productStore = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong();

    @PostConstruct
    public void init(){
        Long id1 = idCounter.incrementAndGet();
        productStore.put(id1, new Product(id1, "Spring T-Shirt", 25000, 100));

        log.info("초기 상품 데이터 생성: {}", productStore.get(id1));

        Long id2 = idCounter.incrementAndGet();
        productStore.put(id2, new Product(id2, "K8s Cup", 15000, 50));
        log.info("초기 상품 데이터 생성: {}", productStore.get(id2));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<Product> getProductById(@PathVariable Long productId) {
        Product product = productStore.get(productId);

        if(product != null) {
            log.info("{}번 상품 정보 조회 성공", productId);
            return ResponseEntity.ok(product);
        }
        else{
            log.warn("{}번 상품을 찾을 수 없음", productId);
            return ResponseEntity.notFound().build();
        }
    }
}
