package com.example.order.service.order.service;

import com.example.order.service.order.config.ServiceProperties;
import com.example.order.service.order.dto.OrderRequest;
import com.example.order.service.order.dto.ProductDto;
import com.example.order.service.order.dto.UserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    private final RestTemplate restTemplate = new RestTemplate();
    private final ServiceProperties properties;

    //@Value("${app.services.user-url}")
    private final String userUrl = properties.getUserUrl();
    private final String productUrl = properties.getProductUrl();

    public String createOrder(OrderRequest orderRequest) {
        log.info("주문 생성 요청 수신: {}", orderRequest);

        // 1.사용자 정보 확인
        try {
            log.info("사용자 서비스 호출: URL={}, userId={}", userUrl, orderRequest.getUserId());

            ResponseEntity<UserDto> userResponse = restTemplate.getForEntity(userUrl, UserDto.class, orderRequest.getUserId());

            log.info("사용자 확인 완료: {}", userResponse.getBody());
        }
        catch (HttpClientErrorException.NotFound e){
            log.error("사용자를 찾을 수 없음: ID - {}. User-Service 연결에 실패했거나 사용자가 없습니다.", orderRequest.getUserId());
            throw new RuntimeException("존재하지 않는 사용자입니다.");
        }
        catch (Exception e){
            log.error("User-Service 호출 중 알 수 없는 오류 발생: {}", e.getMessage());
            throw new RuntimeException("사용자 서비스 통신 오류입니다.");
        }

        // 상품 정보 및 재고 확인
        ProductDto product;
        try {
            log.info("상품 서비스 호출: URL={}, productId={}", productUrl, orderRequest.getProductid());
            ResponseEntity<ProductDto> productResponse = restTemplate.getForEntity(productUrl, ProductDto.class, orderRequest.getProductid());
            product = productResponse.getBody();
            log.info("상품 확인 완료: {}", product);
        }
        catch (HttpClientErrorException.NotFound e) {
            log.error("상품을 찾을 수 없음: ID - {}. Product-Service 연결에 실패했거나 상품이 없습니다.", orderRequest.getProductid());
            throw new RuntimeException("존재하지 않는 상품입니다.");
        }
        catch(Exception e){
            log.error("Product-Service 호출 중 알 수 없는 오류 발생: {}", e.getMessage());
            throw new RuntimeException("상품 서비스 통신 오류입니다.");
        }

        if(product.getStock() < orderRequest.getQuantity()) {
            log.warn("재고 부족: 상품 ID - {}, 요청 수량 - {}, 현재 재고 - {}",
                    product.getId(), orderRequest.getQuantity(), product.getStock());
            throw new RuntimeException("상품 재고가 부족합니다.");
        }

        log.info("주문이 성공적으로 생성되었습니다. 사용자 ID: {}, 상품 ID: {}",
                orderRequest.getUserId(), orderRequest.getProductid());
        return "주문 성공";
    }

}
