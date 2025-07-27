# Virtual Card Issuance Platform

A Spring Boot backend system for managing virtual cards, implementing creation, top-up, spending, blocking/unblocking, and transaction history retrieval.  

This solution uses JOOQ for database access, full RESTful APIs, custom exception handling, and robust unit & integration tests.

---

## **Project Overview**

This service exposes a REST API for:

- Creating a new virtual card (with initial balance)
- Performing top-ups and spends
- Blocking and unblocking cards
- Retrieving card details and transaction history (paginated)
- Enforcing business rules (rate limiting, optimistic concurrency, active/inactive states)

The code is organized using **controller-service-repository** pattern.  

Persistence is handled with **JOOQ**, and all DTOs, error handling, and mapping are hand-rolled for clarity.

---

## **How to Run Locally**

### **Requirements**
- Java 21
- Maven 3.8+
- No external DB needed. Uses in-memory H2

### **Build**

```sh
mvn clean package
```

### **Run**

```sh
mvn spring-boot:run
```

After that, API will be available at: http://localhost:8080

### **Tests**

All tests (unit + integration) can be run with:

```sh
mvn test
```

You can find coverage for:

- CardService logic (unit tests)

- Rate limiter logic (unit tests)

- Controller layer (mock MVC tests)

- Full end-to-end tests (@SpringBootTest + H2, with true concurrent operations and error cases)

---

## Accessing the In-Memory H2 Database

During development and testing, the application uses an in-memory H2 database. You can inspect data and run SQL queries using the H2 web console, once the application is running.

### **H2 Console**

- **URL:** [http://localhost:8080/h2-console](http://localhost:8080/h2-console)
- **JDBC URL:** `jdbc:h2:mem:testdb`
- **Username:** `sa`
- **Password:** *(leave blank)*

> **Note:**  
> Data is cleared every time the app restarts, as this is an in-memory database.

---

## **API Documentation**

Once the application is running, access the interactive Swagger UI at: http://localhost:8080/swagger-ui/index.html

This documentation includes:
- All available endpoints and their usage
- Example requests and responses
- Error model and error codes
- Business rules and data constraints
- Schemas of requests and responses DTOs

---

## Folder Structure

```
virtual-card-issuance-platform/
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── nium/
│   │   │           └── cardplatform/
│   │   │               ├── controller/         # REST controllers
│   │   │               ├── dto/                # Data Transfer Objects
│   │   │               ├── exception/          # Custom exception classes & handler
│   │   │               ├── repository/         # JOOQ-based repositories
│   │   │               ├── service/            # Business logic & services
│   │   │               └── config/             # Configuration classes (Swagger, JOOQ)
│   │   └── resources/
│   │       │     └── db/
│   │       │        └── schema.sql             # DB schema
│   │       └── application.yml                 # Spring Boot configuration
│   └── test/
│       └── java/
│           └── com/
│               └── nium/
│                   └── cardplatform/
│                       ├── controller/                    # Controller tests
│                       ├── service/                       # Service unit tests
│                       └── CardPlatformEndToEndTest.java  # E2E/integration tests
├── target/
│   ├── generated-sources/
│   │                 ├── jooq/                 # JOOQ generated classes
│   │                 └── ...
│   └── ...  
├── pom.xml                                     # Maven build file
├── README.md					# Basic documentation
└── ...                                         
```

---

## Design Decisions

- JOOQ was selected over JPA for explicit SQL and type-safe query construction, as required by the challenge.

- Rate limiting implemented in-memory for simplicity, but can easily be swapped for Redis or a distributed approach if needed scalability becomes a requirement.

- Each card has a version field, so all balance updates use compare-and-swap to prevent lost updates to achieve optimistic concurrency.

- Robust error handling achieved by custom @ControllerAdvice for mapping business and framework exceptions to JSON error responses.

- Test pyramid focused on service/controller unit tests and end-to-end (E2E) integration tests, including true concurrency/race conditions.

---

## Trade-Offs Due to Time Constraints

- Used H2 in-memory for easy test and run, but would use Postgres/MySQL in production.

- No authentication/authorization on endpoints (could add JWT or OAuth with more time).

- Only core properties in application.yml. For production, would parameterize further (profiles, DB pools, etc.).

- Transaction history endpoint supports limit/offset implementing basic pagination. Could be extended with better filtering/sorting.

- No Dockerfile or deployment scripts omitted for time, but trivial to add if required.

---

## Potential Improvements

- Switch to distributed rate limiting (e.g., Redis) for horizontal scaling.

- Add security (JWT auth, user context).

- Use PostgreSQL or MySQL for real data.

- API contract tests and more validation (use Spring Cloud Contract and enhance DTO validation and response structure).

- Add Spring Boot Actuator, distributed tracing, and robust logging for ops visibility and monitoring.

- Use JMeter, Gatling or similar to simulate real-world usage for performance/load tests.

- Provide a Dockerfile for containerized local or cloud deployment (easy for reviewers and DevOps to run anywhere).

- Integrate JaCoCo for code coverage reports, to demonstrate tested lines/methods/classes.

- Improved Code Documentation, adding more in-line comments and JavaDoc, especially for public APIs and business logic, to make the codebase even more maintainable.

- Use the latest LTS (Java 24) for enhanced performance and language features. 
  > **Note:**  
  > I encountered issues with Mockito/Byte Buddy support for Java 24 during local testing, so reverted to Java 21 for maximal compatibility.

- Review and update dependency versions to fix vulnerabilities and deprecated warnings (e.g., switch from @MockBean to @BindTo or native test annotations in latest Spring Boot).

- Deploy the project to AWS

- CI/CD automation (GitHub Actions, CodePipeline), automated test deployment, and blue/green deployment support

---

## Example AWS Cloud Architecture

With more time, I would provide an infrastructure-as-code setup (e.g., AWS CDK or Terraform) to deploy this platform to AWS using modern, scalable serverless components:

- **Amazon API Gateway** for secure, scalable HTTP(S) ingress and routing.
- **AWS Lambda** for running the Spring Boot application as serverless functions (using AWS SnapStart or Java Lambda custom runtime).
- **Amazon RDS (Aurora/MySQL/Postgres) with RDS Proxy** for efficient, secure, and scalable database access.
- **Amazon S3** for storing logs or exported data, if needed.
- **Amazon CloudWatch** for monitoring, metrics, and alerting.
- **(Optional) AWS Secrets Manager** for managing DB/API secrets.

### **Proposed Serverless Architecture**

```plaintext
    [ Client Apps / Postman ]
               |
               v
        +--------------+
        |  API Gateway |
        +--------------+
               |
               v
        +-------------+
        |   Lambda    |  (Spring Boot Java handler)
        +-------------+
               |
               v
        +------------+        +-------------+
        |  RDS Proxy |------->|   RDS DB    |
        +------------+        +-------------+
               |
       (CloudWatch Logs)

```

### **Benefits**

- **No server management**: Scales to zero when idle; pay only for use.
- **Efficient connection pooling**: RDS Proxy prevents DB connection exhaustion.
- **Secure and auditable**: All access goes through managed AWS services.
- **Production-grade**: Can add WAF, VPC, private endpoints as needed.

---

## Learning Strategy

- Whenever a new library or tool was required (e.g., JOOQ), I followed this approach:

- Read through the official quickstarts and guides, prioritizing official documentation.

- Created minimal reproducible snippets to understand configuration and query syntax.

- Checked top-voted answers of the StackOverflow community for best practices and common pitfalls .

- Used ChatGPT for quick clarifications, code patterns, code snippets, troubleshooting, and brainstorming alternative approaches, while always validating results.

- Integrated with the project incrementally, refactoring as new capabilities became clear with minimal code changes.

- Wrote unit tests to ensure correct usage and understanding.

---

## Contact

- Artur Gomes Barreto
  + [LinkedIn](https://www.linkedin.com/in/arturgomesbarreto/)
  + [GitHub](https://github.com/ArturBarreto/VirtualCardIssuancePlatform)
  + [E-mail](mailto:artur.gomes.barreto@gmail.com)
  + [WhatsApp](https://api.whatsapp.com/send?phone=35677562008)
