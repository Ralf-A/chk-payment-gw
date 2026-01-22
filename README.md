### Summary

This is a simple payment gateway built with Spring Boot and Java. It exposes a small HTTP API to the merchant side to submit payments
and retrieve their status, validates requests at the gateway level, calls an external acquirer (simulated bank), and persists the outcome (for now, in-memory).

### Running the application

* Start the payment simulator with `docker-compose up`
* It will be available at `http://localhost:8090`.
* Run `./gradlew bootRun` or directly`PaymentGatewayApplication` to start the application.
* The application will be available at `http://localhost:8080`.
* Run tests with `./gradlew test`.

### Key design considerations

1. **Separation of concerns**

  * `PaymentGatewayController` only handles HTTP concerns and delegates to the service.
  * `PaymentGatewayService` owns the payment workflow: validate, call acquirer, persist, map to response.
  * `PaymentRequestValidator` centralizes gateway specific validation rules.
  * `BankClient` integrates with the acquirer.
  * `CommonExceptionHandler` maps domain exceptions to consistent HTTP responses and `payment-status` headers.

2. **Validation and rejection**

  * All merchant requests go through `PaymentRequestValidator` before any acquirer call.
  * Rules currently enforced:
    * Card number: numeric, 14–19 digits.
    * CVV: numeric, 3–4 digits.
    * Amount > 0.
    * Currency in `GBP`, `USD`, `EUR`. I had an idea to use ISO 4217 codes Java Object `Currency`, but the requirement was limited to three.
    * Expiry month in [1, 12].
    * Expiry year not in the past.
    * Full expiry `YearMonth` not before the current `YearMonth`.
    * Invalid data produces `InvalidRequestException`, mapped to, on such failures the acquirer is never called, 
    and the payment is stored and returned to as `REJECTED`. I decided against putting this exception in the exception handler, 
    instead letting the service handle it directly to ensure the payment is recorded as rejected (and response is as returned).
    * I had an idea initally to use Bean Validation annotations in the POJO, but opted for a dedicated validator class for clarity and 
    explicit control over the rules and error handling.

3. **Interaction with acquirer**

  * `BankHttpClient` uses `WebClient` to POST to the configured `acquirer.url`.
  * Network or `503` errors become `AcquirerUnavailableException`.
  * Acquirer `400` becomes `InvalidRequestException` (gateway treats it as a client issue toward acquirer).
  * For successful calls, `BankAcquiryResponse.authorized` maps to:
    * `AUTHORIZED` if `true`
    * `DECLINED` if `false`

4. **Domain model and persistence**

   * `Payment` is persisted via `PaymentsRepository`. I did a little change to the provided double-repository in the sample code, to use H2 with JPA repository.
  I thought that this small keeps the code simpler and more realistic.
  * Only last 4 digits of the card are stored, derived in `PaymentGatewayServiceImpl` only if validation is passed.
  * The stored `PaymentStatus` is one of `AUTHORIZED`, `DECLINED`, `REJECTED`.

5. **HTTP API surface**

  * `POST /payments`
    * Request: `PaymentRequest` (card number, expiry, currency, amount, cvv).
    * Response: `PaymentResponse` with generated id, status, last 4 digits, and echoed fields.
    * For valid requests:
      * 200 with `status = Authorized` or `Declined`.
    * For acquirer-level unavailability (card ending with 0 in simulator):
      * 200 with `status = Declined`.
    * For invalid requests (gateway-side validation failure, e.g. expiry, amount, or currency):
      * 200 with `status = Rejected`. Validation errors are handled inside the service and the payment is recorded as rejected.
    * For unexpected errors:
      * 503 with `code = INTERNAL_ERROR`.
  * `GET /payments/{id}` - id must be a valid UUID
    * 200 with `PaymentResponse` if found.
    * 404 with `code = NOT_FOUND` otherwise.

6. **Testing approach and assumptions**

  * Unit tests for:
    * `PaymentRequestValidator` (validation rules and expiry logic, using relative dates).
    * `PaymentGatewayController` (authorization/decline/rejection, acquirer unavailability, and retrieval, but from the API call).
    * `PaymentGatewayService` (authorization/decline/rejection, acquirer unavailability, and retrieval directly from the service mock).
    * Tests verifying:
      * Happy - Authorized and Unhappy - Declined paths with acquirer mock.
      * Invalid input -> rejection and no acquirer call.
      * Valid input -> correct status and last 4 digits.
      * Not-found and internal-error paths.
  * Assumptions:
    * Time semantics based on `YearMonth.now()` in the default timezone in which the application runs in.
    * Only last 4 digits persisted for security.

7. **Configuration**

* Dependencies in `build.gradle`:
  * updated springdoc-openapi-starter-webmvc-ui as it had a CVE 5.3 vulnerability to org.apache.commons:commons-lang3
  * added spring-boot-starter-webflux for WebClient
  * added spring-boot-starter-data-jpa and H2 for in-memory persistence  
  * lombok for boilerplate reduction
  * h2 for test runtime database
  
* `application.properties` and `application-test.properties` for test profile:
  * acquirer URL configurable via `acquirer.url` property for different environments

* structure - I used clear package structure:
  * `configuration` package for application config
  * `controller` package for incoming HTTP layer
  * `service` package for business logic
  * `client` package for acquirer integration
  * `model` package for domain and DTOs
  * `repository` package for persistence
  * `exception` package for custom exceptions and handler
  * `validation` package for request validation logic

### Limitations, what could be better

* Single acquirer integration only; no routing, failover, or multi-acquirer strategy.
* No authentication/authorization for merchants, the API is open.
* No rate limiting, idempotency keys for `POST /payments`.
* Persistence uses an in-memory database.
* Validation returns only a coarse `REJECTED` status; detailed error codes are not exposed to the merchant.

### Future enhancements

* **Merchant onboarding and auth**
  * API keys or OAuth2 client credentials.
  * Merchant-scoped payments and per-merchant configuration (currencies, limits, risk rules).

* **Idempotency and robustness**
  * Idempotency keys on `POST /payments` to make the operation safely retryable.

* **Richer responses and error model**
  * Future simulated error codes for actual real life validation failures (like `CARD_INVALID`, `AMOUNT_TOO_LOW`).
  * Should the merchant know the exact reason for rejection? I think maybe just a generic `REJECTED` is better for security.

* **Storage**
  * Swap the in-memory DB for PostgreSQL/MySQL
  * Add indexes on `merchantId` and `createdAt` to support reconciliation/reporting, and add other important payment metadata fields.

### Scalability and measurability

* **Scaling model**
  * The service is stateless at the HTTP layer; multiple instances can run with a load balancer.
  * Shared database is the main shared resource; can be scaled vertically and with read replicas if needed.
  * The acquirer call is done via non-blocking `WebClient`, which supports concurrency.

* **What could be measured**
  * Error rates - 2xx vs 4xx vs 5xx responses
  * Load test - transactions per second
  * Latency for POST and GET requests
  * Acquirer call latency and failure rates
  * Database query times