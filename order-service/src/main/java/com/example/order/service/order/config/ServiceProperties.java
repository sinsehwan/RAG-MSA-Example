package com.example.order.service.order.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.services")
@Getter @Setter
public class ServiceProperties {

    private String userUrl;
    private String productUrl;
}
