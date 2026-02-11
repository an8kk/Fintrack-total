# 💰 FinTrack Backend (Spring Boot)

FinTrack Backend is a secure, scalable REST API built with **Spring Boot 3.2**. It handles core financial logic, user authentication, and real-time transaction tracking.

## 🚀 Key Features
- **Security**: Stateless JWT-based authentication.
- **Data Integrity**: Automated balance calculations and validation guards.
- **Logging**: Comprehensive multi-level logging (INFO/DEBUG/ERROR).
- **Compliance**: Implements modern security patterns including BCrypt hashing and secure CORS.

## 🛠️ Tech Stack
- **Language**: Java 17
- **Framework**: Spring Boot 3.2
- **Persistence**: JPA / Hibernate (PostgreSQL 15)
- **Security**: Spring Security + JWT
- **Build Tool**: Maven

## 📦 Getting Started

### Prerequisites
- JDK 17+
- PostgreSQL 15

### Running Locally
1. Configure `src/main/resources/application.properties` or use environment variables.
2. Run with Maven:
```bash
mvn clean spring-boot:run
```

### Docker Support
Includes a multi-stage `Dockerfile` and `docker-compose.yml` (at root) for containerized deployment.

## 🧪 Testing
Includes **37 automated tests** (Unit + Integration).
```bash
mvn test
```

## 📖 API Documentation
Full endpoint details are available in the [Main Documentation](../documentation.md).
