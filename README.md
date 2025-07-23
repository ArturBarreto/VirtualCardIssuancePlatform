\# Virtual Card Issuance Platform



This project implements a backend API for a virtual card issuance and spending platform, built with Spring Boot and Java. The platform allows users to create virtual cards, top up balances, and spend, with full transactional integrity and robust concurrency handling.



---



\## Folder Structure



\- \*\*controller/\*\*  

&nbsp; REST endpoints. Maps HTTP requests to service calls and returns DTOs.



\- \*\*service/\*\*  

&nbsp; Business logic, transaction management, and concurrency control.



\- \*\*repository/\*\*  

&nbsp; Spring Data JPA repositories for Card and Transaction entities.



\- \*\*entity/\*\*  

&nbsp; JPA entity classes: Card, Transaction.



\- \*\*dto/\*\*  

&nbsp; Data Transfer Objects for API requests/responses.



\- \*\*exception/\*\*  

&nbsp; Custom exceptions and a global error handler.



\- \*\*config/\*\*  

&nbsp; Configuration classes (e.g., Swagger, rate limiter beans).



\- \*\*test/\*\*  

&nbsp; Unit and integration tests for controllers and services.



---



\## Key Files Description



\- \*\*CardPlatformApplication.java\*\* – Main entry point for the Spring Boot application.

\- \*\*application.yml\*\* – Configuration for database, server port, etc.

\- \*\*entity/Card.java\*\* – Card entity (balance, status, version, etc.).

\- \*\*entity/Transaction.java\*\* – Transaction entity for top-ups and spends.

\- \*\*controller/CardController.java\*\* – Exposes REST API endpoints for card operations.

\- \*\*service/CardService.java\*\* – Business logic.

\- \*\*repository/CardRepository.java\*\* – Spring Data JPA repositories for Card.

\- \*\*repository/TransactionRepository.java\*\* – Spring Data JPA repositories for Transaction.

\- \*\*exception/GlobalExceptionHandler.java\*\* – Handles errors and HTTP codes.

\- \*\*test/CardServiceTest.java\*\*, \*\*test/CardControllerTest.java\*\* – Tests for core logic and endpoints.



---



\## Build \& Run (Maven)



\### Prerequisites



\- Java 17+ (recommended)

\- Maven 3.8+



\### Build



\- mvn clean package



\### Run



\- mvn spring-boot:run

or 

\- java -jar target/card-platform-\*.jar



\## API Documentation



\- Access at http://localhost:8080/swagger-ui.html



\## Contact



\- Artur Gomes Barreto

\+ \[LinkedIn](https://www.linkedin.com/in/arturgomesbarreto/)

\+ \[GitHub](https://github.com/ArturBarreto/VirtualCardIssuancePlatform)

\+ \[E-mail](artur.gomes.barreto@gmail.com)

\+ \[+356 7756 2008](https://api.whatsapp.com/send?phone=35677562008)

