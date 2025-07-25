# Virtual Card Issuance Platform

This project implements a backend API for a virtual card issuance and spending platform, built with Spring Boot and Java. The platform allows users to create virtual cards, top up balances, and spend, with full transactional integrity and robust concurrency handling.

---

## Folder Structure

- **controller/**

REST endpoints. Maps HTTP requests to service calls and returns DTOs.

- **service/**

Business logic, transaction management, and concurrency control.

- **repository/**

Spring Data JPA repositories for Card and Transaction entities.

- **dto/**

Data Transfer Objects for API requests/responses.

- **exception/**

Custom exceptions and a global error handler.

- **config/**

Configuration classes (e.g., Swagger, rate limiter beans).

- **test/**

Unit and integration tests for controllers and services.

---

## Key Files Description

- **CardPlatformApplication.java** – Main entry point for the Spring Boot application.
  
- **application.yml** – Configuration for database, server port, etc.
  
- **controller/CardController.java** – Exposes REST API endpoints for card operations.
  
- **service/CardService.java** – Business logic.
  
- **repository/CardRepository.java** – Spring Data JPA repositories for Card.
  
- **repository/TransactionRepository.java** – Spring Data JPA repositories for Transaction.
  
- **exception/GlobalExceptionHandler.java** – Handles errors and HTTP codes.
  
- **exception/CardNotFoundException.java** – Handles errors and HTTP codes.
  
- **exception/InsufficientBalanceException.java** – Handles errors and HTTP codes.

---

## Build & Run (Maven)

### Prerequisites

- Java 17+ (recommended)

- Maven 3.8+

### Build

- mvn clean package

### Run

- mvn spring-boot:run
  
  or

- java -jar target/card-platform-*.jar

---

## API Documentation

Once the application is running, access the interactive Swagger UI at:

[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

This documentation includes:
- All available endpoints and their usage
- Example requests and responses
- Error model and error codes
- Business rules and data constraints

---

## Contact

- Artur Gomes Barreto
  + [LinkedIn](https://www.linkedin.com/in/arturgomesbarreto/)
  + [GitHub](https://github.com/ArturBarreto/VirtualCardIssuancePlatform)
  + [E-mail](mailto:artur.gomes.barreto@gmail.com)
  + [WhatsApp](https://api.whatsapp.com/send?phone=35677562008)
