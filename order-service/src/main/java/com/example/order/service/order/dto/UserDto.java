package com.example.order.service.order.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter @Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    // user-service의 응답을 받기 위한 Dto
    private Long id;
    private String username;
    private String name;
}
