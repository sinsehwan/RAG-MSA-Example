
# MSA-Example 배포 가이드 (General)

---

## 목차
* [1. 개요](#1-개요)
* [2. 시스템-아키텍처](#2-시스템-아키텍처)
* [3. 배포를-위한-핵심-요구사항](#3-배포를-위한-핵심-요구사항)
    * [3.1. Docker-이미지](#31-docker-이미지)
    * [3.2. 서비스-디스커버리-Service-Discovery](#32-서비스-디스커버리-service-discovery)
    * [3.3. 환경-변수-설정](#33-환경-변수-설정)
    * [3.4. 포트-및-네트워크](#34-포트-및-네트워크)
    * [3.5. 서비스-의존성-및-상태-관리](#35-서비스-의존성-및-상태-관리)
* [4. 배포-후-확인-및-테스트](#4-배포-후-확인-및-테스트)
* [5. 로깅-및-트러블슈팅](#5-로깅-및-트러블슈팅)

---

## 1. 개요

본 문서는 MSA-Example 프로젝트를 컨테이너 환경에 배포하기 위해 DevOps 관리자가 알아야 할 핵심 정보를 제공합니다. 특정 배포 도구에 대한 지침이 아닌, 애플리케이션의 **구조, 요구사항, 설정 방법**에 초점을 맞춥니다.

이 프로젝트는 Spring Cloud Gateway와 3개의 마이크로서비스(User, Product, Order)로 구성되며, In-Memory 데이터 저장소를 사용하므로 별도의 데이터베이스가 필요하지 않습니다.

## 2. 시스템 아키텍처

애플리케이션은 4개의 독립적인 서비스로 구성되며, 서비스 간 통신은 컨테이너 네트워크 내에서 이루어집니다.

- **API Gateway (`api-gateway`)**: 외부의 모든 요청을 수신하는 단일 진입점(Entrypoint). 요청 경로에 따라 내부 서비스로 라우팅합니다.
- **User Service (`user-service`)**: 사용자 데이터를 관리합니다.
- **Product Service (`product-service`)**: 상품 데이터를 관리합니다.
- **Order Service (`order-service`)**: 주문을 처리하며, `user-service`와 `product-service`를 호출하여 데이터를 검증합니다.

**서비스 통신 흐름:**
```
[Client] <--> [api-gateway] --+--> [user-service]
                              |
                              +--> [product-service]
                              |
                              +--> [order-service] --+--> [user-service]
                                                    |
                                                    +--> [product-service]
```

## 3. 배포를 위한 핵심 요구사항

이 애플리케이션을 배포하기 위해 컨테이너 오케스트레이션 환경(예: Kubernetes, Docker Compose)은 다음 요구사항을 충족해야 합니다.

### 3.1. Docker 이미지
- 각 서비스(`api-gateway`, `user-service`, `product-service`, `order-service`)는 개별 `Dockerfile`을 가지고 있습니다.
- 배포 전, 각 서비스의 Docker 이미지를 빌드하여 사용할 컨테이너 레지스트리(예: Docker Hub, ECR, GCR)에 푸시해야 합니다.
- **빌드 컨텍스트**: 각 서비스의 루트 디렉토리 (예: `./user-service`, `./api-gateway`)
- **기반 이미지**: `eclipse-temurin:21-jdk-jammy` (빌더), `eclipse-temurin:21-jre-jammy` (런타임)

### 3.2. 서비스 디스커버리 (Service Discovery)
- 서비스 간 통신은 **서비스 이름(Service Name)**을 사용한 DNS 조회를 통해 이루어집니다.
- 배포 환경은 컨테이너의 서비스 이름을 해당 컨테이너의 내부 IP로 해석할 수 있어야 합니다. (Kubernetes의 `Service` 리소스, Docker Compose의 네트워크 기능이 이에 해당)
- 각 서비스의 애플리케이션 설정 파일(`application.yml`)에는 아래와 같이 서비스 이름이 하드코딩되어 있습니다.

    - **API Gateway (`api-gateway`) 라우팅 설정:**
        - `uri: http://user-service:8081`
        - `uri: http://product-service:8082`
        - `uri: http://order-service:8083`

    - **Order Service (`order-service`) 내부 호출 URL:**
        - 이 서비스는 환경 변수를 통해 호출 대상 URL을 주입받습니다. (아래 3.3. 환경 변수 설정 참고)

### 3.3. 환경 변수 설정
`order-service`는 다른 서비스를 호출하기 위한 URL 정보를 **환경 변수**로부터 주입받습니다. 배포 시 반드시 아래 환경 변수를 설정해야 합니다.

| 서비스명        | 환경 변수 Key                          | 값 (Value) 예시                                     | 설명                               |
|---------------|--------------------------------------|---------------------------------------------------|------------------------------------|
| `order-service` | `APP_SERVICES_USER-URL`                | `http://user-service:8081/api/users/{userId}`     | 사용자 정보 조회를 위한 URL 템플릿     |
| `order-service` | `APP_SERVICES_PRODUCT-URL`             | `http://product-service:8082/api/products/{productId}` | 상품 정보 조회를 위한 URL 템플릿     |
| (모든 서비스)    | `SPRING_PROFILES_ACTIVE` (선택)        | `docker` 또는 `k8s`                               | 특정 환경에 맞는 프로파일을 활성화할 때 사용 |

*참고: Spring Boot는 환경 변수 형식을 자동으로 변환합니다. `APP_SERVICES_USER-URL`은 애플리케이션 내부에서 `app.services.user-url`로 인식됩니다.*

### 3.4. 포트 및 네트워크
- **외부 노출**: `api-gateway`의 **8080 포트**만 외부 인터넷에 노출되어야 합니다. (예: Kubernetes `Ingress`, `LoadBalancer` 또는 Docker 포트 포워딩 `8080:8080`)
- **내부 통신**: 나머지 서비스들은 클러스터 내부 네트워크에서만 통신하며 외부에 노출될 필요가 없습니다.

| 서비스명            | 컨테이너 포트 | 역할 및 노출 필요성      |
|-------------------|-------------|----------------------|
| `api-gateway`     | `8080`      | **외부 노출 필요 (필수)**  |
| `user-service`    | `8081`      | 내부 통신용 (외부 노출 불필요) |
| `product-service` | `8082`      | 내부 통신용 (외부 노출 불필요) |
| `order-service`   | `8083`      | 내부 통신용 (외부 노출 불필요) |

### 3.5. 서비스 의존성 및 상태 관리
- **시작 순서**: `order-service`는 `user-service`와 `product-service`에 의존합니다. 배포 시 `user-service`와 `product-service`가 먼저 실행되고 준비(Ready) 상태가 되는 것이 안정적입니다.
- **상태 없음(Stateless)**: 모든 서비스는 In-Memory 데이터를 사용하므로 상태가 없습니다(Stateless). Pod/컨테이너가 재시작되면 모든 데이터(사용자, 상품, 주문 내역)는 초기화됩니다.
- **초기 데이터**: `user-service`와 `product-service`는 시작 시 테스트용 데이터를 자동으로 생성합니다.

## 4. 배포 후 확인 및 테스트

배포가 완료된 후, 외부에서 접근 가능한 **API Gateway 주소**를 통해 아래 API를 호출하여 시스템 전체가 정상 동작하는지 확인합니다. (아래 예시에서는 Gateway 주소를 `http://<GATEWAY_IP>:8080` 로 가정합니다.)

**1. 사용자 정보 조회**
```bash
curl -X GET http://<GATEWAY_IP>:8080/api/users/1
# 예상 응답: {"id":1,"username":"testUser","name":"Test User"}
```

**2. 상품 정보 조회**
```bash
curl -X GET http://<GATEWAY_IP>:8080/api/products/2
# 예상 응답: {"id":2,"name":"K8s Cup","price":15000,"stock":50}
```

**3. 정상 주문 생성**
```bash
curl -X POST http://<GATEWAY_IP>:8080/api/orders/create \
-H "Content-Type: application/json" \
-d '{"userId": 1, "productId": 2, "quantity": 5}'
# 예상 응답: "주문 성공"
```

**4. 재고 부족 주문 (오류 케이스)**
```bash
curl -X POST http://<GATEWAY_IP>:8080/api/orders/create \
-H "Content-Type: application/json" \
-d '{"userId": 1, "productId": 1, "quantity": 999}'
# 예상 응답: "상품 재고가 부족합니다."
```

## 5. 로깅 및 트러블슈팅

- **로그 확인**: 각 서비스의 표준 출력(stdout) 로그를 확인하여 동작 상태 및 오류를 파악합니다. 사용 중인 오케스트레이션 도구의 로그 조회 명령(예: `kubectl logs`, `docker logs`)을 사용합니다.
- **`Connection refused` 오류**: `order-service` 로그에 해당 오류가 발생하면, `user-service` 또는 `product-service`가 아직 완전히 준비되지 않았거나 서비스 디스커버리에 문제가 있을 수 있습니다. 서비스 이름과 포트가 올바른지, 네트워크 정책(Network Policy)이 통신을 막고 있지 않은지 확인해야 합니다.