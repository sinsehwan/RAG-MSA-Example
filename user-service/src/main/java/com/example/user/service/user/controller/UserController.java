package com.example.user.service.user.controller;

import com.example.user.service.user.entity.User;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final Map<Long, User> userStore = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong();

    @PostConstruct
    public void init() {
        // 테스트용 초기 데이터
        Long id = idCounter.incrementAndGet();
        userStore.put(id, new User(id, "testUser", "Test User"));
        log.info("초기 사용자 데이터 생성: {}", userStore.get(id));
    }

    @PostMapping("/register")
    public User registerUser(@RequestBody User user){
        Long id = idCounter.incrementAndGet();
        user.setId(id);
        userStore.put(id, user);
        log.info("새로운 사용자 등록: {}", user);
        return user;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<User> getUserById(@PathVariable Long userId){
        User user = userStore.get(userId);

        if(user != null){
            log.info("{}번 사용자 정보 조회 성공", userId);
            return ResponseEntity.ok(user);
        }
        else{
            log.warn("{}번 사용자 조회 실패", userId);
            return ResponseEntity.notFound().build();
        }
    }
}
