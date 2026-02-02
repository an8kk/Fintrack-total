FinTrack Backend (Spring Boot)

Tech Stack
Java 17 / Spring Boot 3.x
Spring Security (JWT-based stateless authentication)
PostgreSQL 
Hibernate/JPA (ORM & Database mapping)
Lombok 

Security Features
Stateless Authentication: Uses JWT tokens to ensure horizontal scalability.
BCrypt Hashing: All passwords are salted and hashed before storage.
CORS Configuration: Strictly managed to allow secure communication with the Flutter frontend.

Getting Started

1. Environment Configuration
Create a `.env` file copying the .env.example file:
- `JWT_SECRET`: Your 256-bit signing key.
- `DB_URL`: Your local DB URL.
- `DB_USERNAME`: Your DB user.
- `DB_PASSWORD`: Your DB password.

2. Run the Application
Command line:
mvn clean spring-boot:run

3. API Endpoints
Method	Endpoint	Description
|POST|/api/auth/register|Create a new account|
|POST|/api/auth/login|Receive a JWT token|
|GET|/api/transactions/{id}|Fetch user transactions|
|PUT|/api/users/{id}|Update profile details|
