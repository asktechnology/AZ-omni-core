# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**AZ-omni-core** is a payment processing and transaction management microservice built with:
- **Spring Boot 3.3.0** with Java 17
- **Maven** for build management
- **Oracle Database** (primary persistence layer)
- **Redis** for caching
- **RabbitMQ** for message queuing (partially implemented)
- **Server Port**: 8040

## Build & Development Commands

### Build and Run
```bash
# Build the project
mvn clean package

# Run the application
mvn spring-boot:run

# Run all tests
mvn test

# Run a single test class
mvn test -Dtest=ClassName

# Run a specific test method
mvn test -Dtest=ClassName#methodName
```

### Code Quality
```bash
# Start SonarQube (requires Docker)
docker-compose up -d

# Run SonarQube analysis
mvn sonar:sonar

# Generate code coverage report
mvn jacoco:report
```

The SonarQube dashboard will be available at `http://localhost:9000` (admin/admin@123).

## Architecture Overview

### Layered Architecture
```
Controllers (REST API endpoints)
    ↓
Services (Business logic)
    ↓
Repositories (Data access with Spring Data JPA)
    ↓
Database (Oracle)
```

### Main Components

**Package: `com.az.payment`**

- **controller/** - REST API endpoints
  - `PaymentController`: Main payment processing API (`/api/v1/payment`)
  - `ServiceController`, `BillerController`, `CategoryController`, `ParameterController`: Management APIs

- **service/** - Business logic layer
  - `PaymentService`: Core payment processing logic
  - `ServiceService`, `BillerService`, etc.: Entity management services
  - `ClientApi`: HTTP client for external biller APIs

- **repository/** - Spring Data JPA repositories for data access

- **mapper/** - DTO ↔ Entity conversion
  - `RequestMapper`: Maps incoming requests to service parameters
  - `ResponseMapper`: Maps biller responses to application responses

- **domain/** - JPA entities (see Core Domain Model below)

- **request/** and **response/** - DTOs for API contracts

- **utils/** - Shared utilities
  - `ServiceUtils`, `CommonUtils`, `BillerUtils`: Business utilities
  - `TransactionLogUtils`: Transaction tracking helpers
  - `Validation`: Custom validation logic

- **config/** - Application configuration
  - `AppConfiguration`: RestTemplate bean
  - `RabbitmqConfiguration`: Message broker setup
  - `audit/`: Auditing aspect for entity tracking
  - `logging/`: Logging aspect for cross-cutting concerns

- **exception/** and **handler/** - Exception handling
  - `GlobalExceptionHandler`: Centralized error handling with `@RestControllerAdvice`
  - `BusinessException`, `ValidationException`: Custom exceptions

### Key Design Patterns

1. **Entity-DTO Pattern**: Separate domain entities from API contracts
2. **Repository Pattern**: Spring Data JPA repositories
3. **Service Layer Pattern**: Business logic encapsulation
4. **Aspect-Oriented Programming (AOP)**:
   - `LoggingAspect`: Cross-cutting logging
   - `AuditingAspect`: Entity audit trail
5. **Global Exception Handling**: Centralized error responses

## Core Domain Model

### Service Entity (`domain/Service.java`)
Defines payment services with:
- **ServiceType**: `INQUIRY`, `PAYMENT`, `BOTH`, `CHECKSTATUS`, `DATASOURCE`
- **Service Chaining**: Supports `beforeService` and `afterService` for workflows
- **Many-to-Many**: Linked to Parameters and Billers

### Parameter Entity (`domain/Parameter.java`)
Configures service input/output parameters:
- **Input Types**: `text`, `menu`, `checkbox`, `textarea`, `radio`, `shared`
- **JSON Types**: `primitive`, `object`, `array`, `arrayOfObject`
- **Special Flags**: `isAmount`, `isBillId`, `isResponseCode`, `isResponseMessage`, `isExternalRrn`
- **Linked to Options**: For dropdown/menu parameters

### TransactionLog & ChainTransactionLog
Complete audit trail for all transactions:
- **TransactionLog**: High-level transaction tracking (ID, RRN, biller, service, account, amount, status)
- **ChainTransactionLog**: Detailed step-by-step tracking for debugging and auditing

### Biller Entity (`domain/Biller.java`)
External payment provider configuration:
- Base URL for API calls
- Linked to Services and Categories

### Category Entity (`domain/Category.java`)
Groups services by category type

## Payment Processing Flow

1. **Receive Request**: Client sends `ProcessRequest` to `PaymentController.processPayment()`
2. **Create Transaction Log**: Generate unique RRN (Reference Retrieval Number)
3. **Fetch Service Configuration**: Look up Service entity by ID
4. **Map Request**: `RequestMapper` converts request to service-specific format
5. **Map Datasource Parameters**: Add external datasource parameters
6. **Call Biller API**: `ClientApi` sends HTTP request to external biller
7. **Process Response**: `ResponseMapper` converts biller response to standard format
8. **Store Chain Transaction Logs**: Detailed audit trail for each step
9. **Return Response**: Send `PaymentResponse` to client

**Key Endpoints:**
- `POST /api/v1/payment/process` - Process payment request
- `POST /api/v1/payment/postCheck` - Post-payment verification
- `POST /api/v1/payment/checkStatus` - Check transaction status

## Database & Infrastructure

### Oracle Database
- **Connection**: `10.113.10.76:1521:grtwo`
- **User**: `paygz`
- **ID Generation**: Sequence-based for all entities

### Redis
- Used for caching (via Spring Cache abstraction)

### RabbitMQ
- Configured but mostly disabled in current version
- Host: `localhost:5672`

## Key Conventions

### Multi-language Support
- Use `Language` header in API requests
- Supported: English (default), Arabic
- Impacts response messages and parameter labels

### Standard Response Format
All endpoints return `ApiResponse<T>`:
```json
{
  "code": 0,
  "message": "Success",
  "data": { ... }
}
```

### Exception Handling
- `GlobalExceptionHandler` provides consistent error responses
- Business exceptions return appropriate HTTP status codes
- All exceptions are logged

### Logging
Two separate log files with daily rotation:
- **Payment logs**: `logs/Payment_YY_MM_DD.log` (DEBUG level)
- **System logs**: `logs/System_YY_MM_DD.log` (INFO level)

Configuration: `src/main/resources/logback-spring.xml`

### AOP Aspects
- **LoggingAspect**: Logs method entry/exit and execution time
- **AuditingAspect**: Tracks entity changes with `@Auditable` annotation

## Important Files

### Entry Points
- `src/main/java/com/az/payment/PaymentApplication.java` - Application entry point

### Core Controllers
- `src/main/java/com/az/payment/controller/PaymentController.java` - Main payment API

### Core Services
- `src/main/java/com/az/payment/service/PaymentService.java` - Payment processing logic
- `src/main/java/com/az/payment/service/ClientApi.java` - External biller integration

### Configuration Files
- `src/main/resources/application.yml` - Main application configuration
- `src/main/resources/logback-spring.xml` - Logging configuration
- `pom.xml` - Maven dependencies and build configuration
- `docker-compose.yml` - SonarQube + PostgreSQL setup

### how to do changes in code
- any 'change request' will be proccessed in 'task'
- 'task' name will be choosen automaticly unless it's provided in the 'change request'.
- any task should have it's folder with task name inside ./__Claude_tasks.
- first full plan will be created in plan.md
- then the plan.md will be updated by done steps
- any needed test files for specific task that not part of the main code will be created in the task folder.