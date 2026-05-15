# 🚀 TechLearner - Complete Backend Learning Project

## Topics Covered (30+)

| # | Topic | File |
|---|-------|------|
| 1 | IOC Container & Bean Lifecycle | `TechLearnerApplication.java`, `SecurityConfig.java` |
| 2 | Dependency Injection Internals | All `@Service` classes (Constructor Injection) |
| 3 | Spring AOP Internals | `LoggingAspect.java`, `RateLimitingAspect.java` |
| 4 | DispatcherServlet Request Flow | `AuthController.java` comments |
| 5 | Spring Security + JWT Flow | `SecurityConfig.java`, `JwtUtils.java`, `JwtAuthFilter.java` |
| 6 | JVM Memory Model | `JvmInternalsService.java` → `GET /api/internals/jvm` |
| 7 | Garbage Collection | `JvmInternalsService.java` → `GET /api/internals/gc` |
| 8 | HashMap Internal | `JvmInternalsService.java` → `GET /api/internals/hashmap` |
| 9 | ConcurrentHashMap | `JvmInternalsService.java` → `GET /api/internals/concurrent-hashmap` |
| 10 | Java Streams Internals | `ProductService.java` - stream pipelines |
| 11 | Thread Pool | `AsyncConfig.java`, `GET /api/internals/threads` |
| 12 | ThreadLocal | `JwtAuthFilter.java` - SecurityContextHolder comments |
| 13 | Race Condition & Synchronization | `ConcurrentHashMap` demo, `@Version` Optimistic Lock |
| 14 | @Transactional Internals | `ProductService.java`, `OrderService.java` |
| 15 | Hibernate Session & ORM | `User.java`, `Order.java`, `Product.java` |
| 16 | JDBC + Connection Pool | `application.yml` - HikariCP config |
| 17 | DB Transactions & Isolation | `OrderService.java` - Isolation levels |
| 18 | Caching (Redis) | `ProductService.java` - @Cacheable, @CacheEvict, @CachePut |
| 19 | Async Processing | `ProductService.java` - @Async + CompletableFuture |
| 20 | Message Queues (Kafka) | `KafkaConfig.java`, `EventProducer.java` |
| 21 | Rate Limiting | `RateLimitingAspect.java` - Redis-based sliding window |
| 22 | Indexing Internals | `Product.java`, `Order.java` - @Index annotations |
| 23 | Query Optimization | `ProductRepository.java` - JPQL, projections, hints |
| 24 | N+1 Problem Deep Dive | `OrderService.java` - `/fixed` vs `/nplusone` endpoints |
| 25 | Pagination Strategies | `ProductService.java` - Offset + Cursor-based |
| 26 | API Gateway (concept) | `OrderController.java` → `GET /api/internals/architecture` |
| 27 | Microservices Basics | `GET /api/internals/architecture` |
| 28 | Load Balancer | `GET /api/internals/architecture` |

---

## 📋 Prerequisites

Install karo (ek baar):
- **Java 17+**: https://adoptium.net
- **Maven 3.8+**: https://maven.apache.org
- **Docker Desktop**: https://www.docker.com/products/docker-desktop

---

## 🚀 Step-by-Step Setup

### Step 1: Docker se services start karo

```bash
cd techlearner
docker-compose up -d
```

Verify kar lo:
```bash
docker ps
# postgres, redis, zookeeper, kafka, kafka-ui - sab RUNNING hone chahiye
```

### Step 2: Application start karo

```bash
mvn spring-boot:run
```

Ya first build karo:
```bash
mvn clean install -DskipTests
java -jar target/techlearner-1.0.0.jar
```

### Step 3: Check karo

```
http://localhost:8080/actuator/health   → {"status":"UP"}
http://localhost:8090                   → Kafka UI Dashboard
```

---

## 🧪 API Testing Guide (cURL commands)

### 1. Register User
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"john","email":"john@test.com","password":"secret123"}'
```
Response mein `token` milega. Ise copy karo!

### 2. Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"john@test.com","password":"secret123"}'
```

### 3. Products list karo (with pagination)
```bash
# TOKEN replace karo apne real token se
TOKEN="eyJhbGci..."

curl http://localhost:8080/api/products?page=0&size=5 \
  -H "Authorization: Bearer $TOKEN"
```

### 4. Product create karo (ADMIN only - register with admin role manually)
```bash
curl -X POST http://localhost:8080/api/products \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Laptop","description":"Gaming laptop","price":75000,"category":"ELECTRONICS","stock":10}'
```

### 5. Cursor-based Pagination demo
```bash
curl "http://localhost:8080/api/products/cursor?lastId=0&size=5" \
  -H "Authorization: Bearer $TOKEN"
```

### 6. N+1 Problem Demo ← IMPORTANT!
```bash
# Open 2 terminal tabs. In both, watch logs: tail -f logs

# ✅ FIXED (1 query):
curl http://localhost:8080/api/orders/fixed -H "Authorization: Bearer $TOKEN"

# ❌ N+1 Problem (many queries):
curl http://localhost:8080/api/orders/nplusone -H "Authorization: Bearer $TOKEN"
```
Logs mein query count compare karo!

### 7. JVM Memory Info
```bash
curl http://localhost:8080/api/internals/jvm
```

### 8. GC Stats
```bash
curl http://localhost:8080/api/internals/gc
```

### 9. Thread Pool Info
```bash
curl http://localhost:8080/api/internals/threads
```

### 10. HashMap Internals
```bash
curl http://localhost:8080/api/internals/hashmap
```

### 11. ConcurrentHashMap Demo
```bash
curl http://localhost:8080/api/internals/concurrent-hashmap
```

### 12. Architecture Concepts
```bash
curl http://localhost:8080/api/internals/architecture
```

### 13. Redis Cache verify karo
```bash
# Redis CLI mein
docker exec -it techlearner-redis redis-cli

# Keys dekho
KEYS *

# Products cache
KEYS products*

# TTL dekho
TTL "products::page_0_size_10_sort_id"
```

### 14. Actuator Endpoints
```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/metrics
curl http://localhost:8080/actuator/caches
curl http://localhost:8080/actuator/threaddump
```

---

## 🔬 Learning Experiments

### Experiment 1: Cache Hit/Miss
```bash
# First call → logs mein "Cache MISS - Loading from DB"
curl http://localhost:8080/api/products?page=0&size=10&sortBy=id -H "Auth..."

# Second call → NO DB log (served from Redis cache!)
curl http://localhost:8080/api/products?page=0&size=10&sortBy=id -H "Auth..."
```

### Experiment 2: N+1 Problem
```bash
# 2-3 orders create karo alag alag users se
# Phir compare karo:
curl http://localhost:8080/api/orders/fixed     # Check: how many SQL queries in logs?
curl http://localhost:8080/api/orders/nplusone  # Much more queries!
```

### Experiment 3: Rate Limiting
```bash
# 60+ requests ek minute mein bhejo
for i in {1..65}; do
  curl -s -o /dev/null -w "%{http_code}\n" \
    "http://localhost:8080/api/auth/login" \
    -X POST -H "Content-Type: application/json" \
    -d '{"email":"a@b.com","password":"wrong"}'
done
# After 60: 429 Too Many Requests dekhoge!
```

### Experiment 4: JVM Heap Pressure
```bash
# Force GC
curl http://localhost:8080/api/internals/gc

# Before and after memory compare karo
curl http://localhost:8080/api/internals/jvm
```

### Experiment 5: Kafka Events
```bash
# Open Kafka UI: http://localhost:8090
# Create a product → "product-created" topic mein message dekhega
curl -X POST http://localhost:8080/api/products ...

# Kafka topic messages:
docker exec -it techlearner-kafka \
  kafka-console-consumer --bootstrap-server localhost:9092 \
  --topic product-created --from-beginning
```

---

## 🗄️ Database Exploration

Connect karo PostgreSQL:
```bash
docker exec -it techlearner-postgres psql -U postgres -d techlearner_db

# Tables dekho
\dt

# Indexes dekho (TOPIC: Indexing)
\di

# Explain a query (TOPIC: Query Optimization)
EXPLAIN ANALYZE SELECT * FROM products WHERE category = 'ELECTRONICS';

# Index usage stats
SELECT schemaname, tablename, indexname, idx_scan, idx_tup_read
FROM pg_stat_user_indexes
ORDER BY idx_scan DESC;
```

---

## 📁 Project Structure

```
techlearner/
├── docker-compose.yml              ← PostgreSQL + Redis + Kafka
├── pom.xml                         ← All dependencies
└── src/main/
    ├── resources/
    │   └── application.yml         ← All configuration
    └── java/com/techlearner/
        ├── TechLearnerApplication.java   ← Entry point (IOC, DI explained)
        ├── aop/
        │   ├── LoggingAspect.java        ← AOP Internals
        │   ├── RateLimitingAspect.java   ← Rate Limiting via AOP+Redis
        │   └── RateLimit.java            ← Custom annotation
        ├── config/
        │   ├── AsyncConfig.java          ← Thread Pool setup
        │   ├── RedisConfig.java          ← Cache configuration
        │   ├── SecurityConfig.java       ← JWT Security chain
        │   └── GlobalExceptionHandler.java
        ├── controller/
        │   ├── AuthController.java       ← Register/Login
        │   ├── ProductController.java    ← CRUD + Pagination
        │   └── OrderController.java      ← N+1 Demo + JVM endpoints
        ├── dto/
        │   └── Dtos.java                 ← Request/Response objects
        ├── entity/
        │   ├── User.java                 ← Hibernate + Indexing
        │   ├── Product.java              ← Composite index, @Version
        │   ├── Order.java                ← N+1 problem source
        │   └── OrderItem.java
        ├── kafka/
        │   ├── KafkaConfig.java          ← Topics, partitions
        │   └── EventProducer.java        ← Producer + Consumer + Events
        ├── repository/
        │   ├── ProductRepository.java    ← JPQL, projections, pagination
        │   ├── OrderRepository.java      ← N+1 fixes (JOIN FETCH, EntityGraph)
        │   └── UserRepository.java
        ├── security/
        │   ├── JwtUtils.java             ← Token generation/validation
        │   ├── JwtAuthFilter.java        ← Filter chain + ThreadLocal
        │   └── UserDetailsServiceImpl.java
        └── service/
            ├── AuthService.java          ← Register/Login logic
            ├── ProductService.java       ← Cache + Async + Streams + Transactions
            ├── OrderService.java         ← N+1 demo + Isolation levels
            └── JvmInternalsService.java  ← JVM + GC + HashMap + CHM + Threads
```

---

## ❓ Common Issues

**Port already in use:**
```bash
lsof -i :5432  # Find process
kill -9 <PID>
```

**Kafka not connecting:**
```bash
docker-compose restart kafka
```

**Redis connection refused:**
```bash
docker-compose restart redis
```

**Build fails:**
```bash
mvn clean install -DskipTests
```
