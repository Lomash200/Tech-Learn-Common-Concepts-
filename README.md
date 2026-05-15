# 🚀 TechLearner - Complete Backend Learning Project

TechLearner is a complete backend learning project built using Java and Spring Boot.
This project demonstrates real-world backend concepts used in modern enterprise applications.

It is designed for learning:

* Spring Boot Internals
* Security with JWT
* Database Optimization
* Redis Caching
* Kafka Messaging
* Multithreading
* JVM Internals
* Hibernate ORM
* Transactions
* Pagination
* Rate Limiting
* Async Processing
* Microservices Concepts

The project is structured like an industry-level backend system and includes practical implementations with hands-on experiments.

---

# 📚 Topics Covered

| #  | Topic                             |
| -- | --------------------------------- |
| 1  | IOC Container & Bean Lifecycle    |
| 2  | Dependency Injection              |
| 3  | Spring AOP                        |
| 4  | DispatcherServlet Flow            |
| 5  | Spring Security + JWT             |
| 6  | JVM Memory Model                  |
| 7  | Garbage Collection                |
| 8  | HashMap Internals                 |
| 9  | ConcurrentHashMap                 |
| 10 | Java Streams                      |
| 11 | Thread Pool                       |
| 12 | ThreadLocal                       |
| 13 | Synchronization & Race Conditions |
| 14 | Transaction Management            |
| 15 | Hibernate ORM                     |
| 16 | JDBC & HikariCP                   |
| 17 | Isolation Levels                  |
| 18 | Redis Caching                     |
| 19 | Async Processing                  |
| 20 | Kafka Messaging                   |
| 21 | Rate Limiting                     |
| 22 | Database Indexing                 |
| 23 | Query Optimization                |
| 24 | N+1 Problem                       |
| 25 | Pagination                        |
| 26 | API Gateway Concepts              |
| 27 | Microservices Basics              |
| 28 | Load Balancing                    |

---

# 🛠️ Tech Stack

## Backend

* Java 17
* Spring Boot
* Spring Security
* Spring Data JPA
* Hibernate
* Maven

## Database & Cache

* PostgreSQL
* Redis

## Messaging

* Apache Kafka
* Zookeeper

## DevOps & Tools

* Docker
* Docker Compose
* Kafka UI

---

# 📋 Prerequisites

Install the following tools before running the project:

## 1. Java 17+

Download:

* [https://adoptium.net](https://adoptium.net)

Verify:

```bash
java -version
```

---

## 2. Maven 3.8+

Download:

* [https://maven.apache.org](https://maven.apache.org)

Verify:

```bash
mvn -version
```

---

## 3. Docker Desktop

Download:

* [https://www.docker.com/products/docker-desktop](https://www.docker.com/products/docker-desktop)

Verify:

```bash
docker --version
```

---

# 🚀 Project Setup

## Step 1: Clone the Repository

```bash
git clone <repository-url>
cd techlearner
```

---

## Step 2: Start Infrastructure Services

Start PostgreSQL, Redis, Kafka, and Zookeeper using Docker:

```bash
docker-compose up -d
```

Verify containers:

```bash
docker ps
```

Expected running containers:

* postgres
* redis
* kafka
* zookeeper
* kafka-ui

---

## Step 3: Build the Project

```bash
mvn clean install -DskipTests
```

---

## Step 4: Run the Application

```bash
mvn spring-boot:run
```

OR

```bash
java -jar target/techlearner-1.0.0.jar
```

---

# ✅ Verify Application

## Health Check

```text
http://localhost:8080/actuator/health
```

Expected Response:

```json
{
  "status": "UP"
}
```

---

## Kafka UI

```text
http://localhost:8090
```

---

# 🔐 Authentication Flow

The project uses JWT-based authentication.

Flow:

1. Register user
2. Login user
3. Receive JWT token
4. Pass token in Authorization header
5. Access secured APIs

Authorization Header:

```text
Authorization: Bearer <TOKEN>
```

---

# 🧪 API Testing

# 1. Register User

```bash
curl -X POST http://localhost:8080/api/auth/register \
-H "Content-Type: application/json" \
-d '{
  "username":"john",
  "email":"john@test.com",
  "password":"secret123"
}'
```

---

# 2. Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
-H "Content-Type: application/json" \
-d '{
  "email":"john@test.com",
  "password":"secret123"
}'
```

Copy the token from the response.

---

# 3. Get Products

```bash
curl http://localhost:8080/api/products?page=0&size=5 \
-H "Authorization: Bearer TOKEN"
```

---

# 4. Create Product

```bash
curl -X POST http://localhost:8080/api/products \
-H "Authorization: Bearer TOKEN" \
-H "Content-Type: application/json" \
-d '{
  "name":"Laptop",
  "description":"Gaming Laptop",
  "price":75000,
  "category":"ELECTRONICS",
  "stock":10
}'
```

---

# 5. Cursor Pagination

```bash
curl "http://localhost:8080/api/products/cursor?lastId=0&size=5" \
-H "Authorization: Bearer TOKEN"
```

---

# 6. JVM Information

```bash
curl http://localhost:8080/api/internals/jvm
```

---

# 7. GC Information

```bash
curl http://localhost:8080/api/internals/gc
```

---

# 8. Thread Pool Information

```bash
curl http://localhost:8080/api/internals/threads
```

---

# 9. HashMap Internals

```bash
curl http://localhost:8080/api/internals/hashmap
```

---

# 10. ConcurrentHashMap Demo

```bash
curl http://localhost:8080/api/internals/concurrent-hashmap
```

---

# 11. Architecture Concepts

```bash
curl http://localhost:8080/api/internals/architecture
```

---

# 🧠 Learning Experiments

## 1. Redis Cache Experiment

First request:

```bash
curl http://localhost:8080/api/products?page=0&size=10
```

Second request:

* Response comes from Redis cache
* Database query is skipped

Observe logs carefully.

---

## 2. N+1 Problem Experiment

### Optimized Endpoint

```bash
curl http://localhost:8080/api/orders/fixed
```

### Non-Optimized Endpoint

```bash
curl http://localhost:8080/api/orders/nplusone
```

Compare SQL queries generated in logs.

---

## 3. Rate Limiting Experiment

Send many requests:

```bash
for i in {1..65}; do
curl -X POST http://localhost:8080/api/auth/login
done
```

After limit exceeds:

* API returns HTTP 429

---

## 4. JVM Heap Experiment

Force Garbage Collection:

```bash
curl http://localhost:8080/api/internals/gc
```

Compare JVM memory before and after.

---

## 5. Kafka Messaging Experiment

Open Kafka UI:

```text
http://localhost:8090
```

Create a product:

* Event is published to Kafka topic

Consume messages manually:

```bash
docker exec -it techlearner-kafka \
kafka-console-consumer \
--bootstrap-server localhost:9092 \
--topic product-created \
--from-beginning
```

---

# 🗄️ Database Exploration

Connect to PostgreSQL:

```bash
docker exec -it techlearner-postgres \
psql -U postgres -d techlearner_db
```

---

## View Tables

```sql
\dt
```

---

## View Indexes

```sql
\di
```

---

## Query Optimization

```sql
EXPLAIN ANALYZE
SELECT * FROM products
WHERE category='ELECTRONICS';
```

---

# 📁 Project Structure

```text
techlearner/
├── docker-compose.yml
├── pom.xml
├── README.md
├── TechLearner_Postman_Collection.json
│
├── src/main/
│   ├── java/com.techlearner/
│   │
│   │   ├── aop/
│   │   │   ├── LoggingAspect.java
│   │   │   ├── RateLimit.java
│   │   │   └── RateLimitingAspect.java
│   │   │
│   │   ├── config/
│   │   │   ├── AsyncConfig.java
│   │   │   ├── DataInitializer.java
│   │   │   ├── GlobalExceptionHandler.java
│   │   │   ├── RedisConfig.java
│   │   │   └── SecurityConfig.java
│   │   │
│   │   ├── controller/
│   │   │   ├── AuthController.java
│   │   │   ├── JvmController.java
│   │   │   ├── OrderController.java
│   │   │   └── ProductController.java
│   │   │
│   │   ├── dto/
│   │   │   └── Dtos.java
│   │   │
│   │   ├── entity/
│   │   │   ├── Order.java
│   │   │   ├── OrderItem.java
│   │   │   ├── Product.java
│   │   │   └── User.java
│   │   │
│   │   ├── kafka/
│   │   │   ├── EventProducer.java
│   │   │   ├── KafkaConfig.java
│   │   │   ├── KafkaConsumer.java
│   │   │   ├── OrderPlacedEvent.java
│   │   │   └── ProductCreatedEvent.java
│   │   │
│   │   ├── repository/
│   │   │   ├── OrderRepository.java
│   │   │   ├── ProductRepository.java
│   │   │   └── UserRepository.java
│   │   │
│   │   ├── security/
│   │   │   ├── JwtAuthFilter.java
│   │   │   ├── JwtUtils.java
│   │   │   └── UserDetailsServiceImpl.java
│   │   │
│   │   ├── service/
│   │   │   ├── AuthService.java
│   │   │   ├── JvmInternalsService.java
│   │   │   ├── OrderService.java
│   │   │   └── ProductService.java
│   │   │
│   │   └── TechLearnerApplication.java
│   │
│   └── resources/
│       └── application.yml
│
└── target/
```

---

# 🔍 Major Concepts Explained

## IOC Container

Spring creates and manages objects automatically.

---

## Dependency Injection

Dependencies are injected through constructors.

---

## JWT Authentication

Secure stateless authentication using tokens.

---

## Hibernate ORM

Maps Java objects to database tables.

---

## Redis Cache

Improves performance by reducing database calls.

---

## Kafka

Used for asynchronous event-driven communication.

---

## Thread Pool

Handles async tasks efficiently.

---

## Transaction Management

Ensures database consistency.

---

## Rate Limiting

Protects APIs from abuse.

---

## N+1 Problem

Demonstrates poor query design and optimization.

---

## Pagination

Supports:

* Offset Pagination
* Cursor Pagination

---

# ❓ Common Issues

## Port Already in Use

```bash
lsof -i :5432
kill -9 <PID>
```

---

## Kafka Connection Issue

```bash
docker-compose restart kafka
```

---

## Redis Connection Issue

```bash
docker-compose restart redis
```

---

## Build Failure

```bash
mvn clean install -DskipTests
```

---

# 🎯 What You Will Learn From This Project

By building and understanding this project, you will learn:

* Real backend architecture
* Enterprise-level Spring Boot development
* Security implementation
* Database optimization
* JVM internals
* Caching strategies
* Event-driven systems
* Async programming
* System design basics
* Performance optimization
* Production-level coding practices

---

# 👨‍💻 Ideal For

This project is useful for:

* Backend Developers
* Java Developers
* Spring Boot Learners
* Interview Preparation
* System Design Basics
* College Projects
* Backend Architecture Learning

---

# 🚀 Future Improvements

Possible enhancements:

* API Gateway
* Microservices Separation
* Dockerized Deployment
* Kubernetes
* Monitoring with Prometheus/Grafana
* CI/CD Pipeline
* Distributed Tracing
* Elasticsearch
* Notification Service

---

# 📌 Conclusion

TechLearner is not just a CRUD project.

It is a complete backend engineering learning platform that helps developers understand how real-world enterprise applications work internally.

This project focuses on:

* Performance
* Scalability
* Security
* Maintainability
* Architecture
* Optimization

It combines theory with practical implementation so that developers can understand both concepts and real execution flow.

