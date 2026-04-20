# Skill: spring-boot-integration-testing

Writes and reviews Spring Boot integration tests in Kotlin following a strict philosophy: real Spring context, real infrastructure via Testcontainers, and only external HTTP calls mocked with MockServer.

## Usage

```
/spring-boot-integration-testing [path or description of the code to test]
/spring-boot-integration-testing review [path to existing test file]
```

## Behavior

When the command starts with `/spring-boot-integration-testing review`, run the **Review** flow.
Otherwise, run the **Write** flow.

---

## Philosophy

Integration tests simulate the application running in production. The real Spring context is started, real infrastructure (databases, queues) is provided via Testcontainers, and only external third-party HTTP calls are mocked with MockServer.

Every feature must be fully covered end-to-end: request validation, controller behaviour, service business rules, and database persistence.

---

## Testing Stack

- **Language:** Kotlin
- **Test framework:** JUnit 5 (Jupiter)
- **HTTP tests:** RestAssured
- **Spring context:** `@SpringBootTest`
- **Infrastructure:** Testcontainers (PostgreSQL, Redis, etc.)
- **External HTTP mocks:** MockServer

---

## Constraints

- Do not add comments to the code. Test names must be descriptive enough to make comments unnecessary.
- Do not use Mockito for integration tests.
- Do not mock repositories or internal services.
- Always start the real Spring context.
- Always use Testcontainers for every infrastructure dependency (database, queues, cache).
- External HTTP services must be mocked using MockServer.
- Never assert raw JSON strings; always deserialize responses into typed DTOs.
- Always verify the database state after write operations — query the repository and assert that persisted data matches what was sent.

---

## Write Flow

### Step 1 — Analyse the code

Before writing any test, read and understand the code being tested. Follow this checklist:

**Read the Request DTO**
- What fields exist?
- What validation annotations are present (`@NotBlank`, `@Size`, `@Email`, `@Pattern`, etc.)?
- Each constraint must have its own test case.

**Read the Controller**
- What HTTP method and path does the endpoint use?
- Does it require authentication/authorization headers?
- What does it return on success (status code + body)?
- Are there any controller-level guards or checks beyond request validation?

**Read the Service**
- What are the business rules? (e.g., uniqueness checks, state transitions, external calls)
- What exceptions are thrown and under what conditions?
- What is saved to the database and what fields are set?

### Step 2 — Define the test list

Only after reading all three layers, define the complete test list before writing code. Cover:

| Layer | Scenario type | Example |
|---|---|---|
| Request validation | Each field constraint | name too long → 400 |
| Controller | Auth missing or invalid | no token → 401 |
| Service (error) | Business rule violation | duplicate email → 409 |
| Service (success) | Happy path | valid payload → 201 + assert DB |

### Step 3 — Write the tests

Each test must follow **Arrange / Act / Assert**:
- **Arrange** — build the request payload and any required pre-state (e.g., seed a user to the DB).
- **Act** — perform the HTTP call via RestAssured.
- **Assert** — check the response status, response body, and (for writes) the database state.

---

## Test Structure

### Organising with @Nested

Group tests by context using JUnit 5 `@Nested` inner classes. Each nested class represents one logical group.

```kotlin
class UserIntegrationTest : IntegrationTest() {

    @Nested
    inner class UserCreation {

        @Nested
        inner class RequestValidation {

            @Test
            fun `given a request with name exceeding max length when creating user then return 400 bad request`() { }
        }

        @Nested
        inner class BusinessRules {

            @Test
            fun `given an email already in use when creating user then return 409 conflict`() { }
        }

        @Nested
        inner class HappyPath {

            @Test
            fun `given a valid request when creating user then return 201 and persist user`() { }
        }
    }
}
```

**Rules:**
- `@Nested` class names use PascalCase (e.g., `UserCreation`, `RequestValidation`, `BusinessRules`, `HappyPath`).
- The outer `@Nested` class describes the feature or endpoint.
- Inner `@Nested` classes describe the scenario group.
- Never use `// region` comments as a substitute for `@Nested`.

---

## Test Naming Convention

All tests must follow **Given / When / Then** semantics:

```
given <state or input condition> when <HTTP action> then <expected outcome with status code>
```

**Good examples:**
- `given a valid request when creating user then return 201 and persist user`
- `given a duplicate email when creating user then return 409 conflict`
- `given a request with name exceeding max length when creating user then return 400 bad request`
- `given an unauthenticated request when listing projects then return 401 unauthorized`

Use backticks in Kotlin:

```kotlin
@Test
fun `given a duplicate email when creating user then return 409 conflict`() {
}
```

**Rules:**
- **Given** — the state or input condition (data seeded, request field value, auth context).
- **When** — the HTTP action (e.g., `when creating user`, `when authenticating`, `when listing projects`).
- **Then** — the observable outcome, always including the HTTP status code.
- Never use `should` in a test name.
- Never omit the `when` clause.

---

## Mocking External HTTP APIs with MockServer

When an endpoint calls an external HTTP API, mock it using a dedicated helper that extends `MockServerHolder`.

**How it works:**
- `MockServerHolder` is an abstract class that starts a `ClientAndServer` singleton on port 8081.
- Each external API gets its own object helper (e.g., `GithubHelper`) that extends `MockServerHolder` and lives under `src/test/kotlin/.../resources/`.
- The test `application.yml` must point the API client base URL to `http://127.0.0.1:8081`.
- `IntegrationTest.beforeEach` must call `<Helper>.reset()` to clear recorded expectations between tests.

**Helper structure:**

```kotlin
object GithubHelper : MockServerHolder() {
    override fun domainPath(): String = "/user"

    fun mockGetUserSuccessfully(response: GithubUserResponse) {
        mockServer()
            .`when`(
                HttpRequest.request()
                    .withMethod("POST")
                    .withPath("/user"),
            )
            .respond(
                HttpResponse.response()
                    .withStatusCode(HttpStatus.OK.value())
                    .withContentType(org.mockserver.model.MediaType.APPLICATION_JSON)
                    .withBody(objectMapper.writeValueAsString(response)),
            )
    }

    fun mockGetUserUnauthorized() {
        mockServer()
            .`when`(
                HttpRequest.request()
                    .withMethod("POST")
                    .withPath("/user"),
            )
            .respond(
                HttpResponse.response()
                    .withStatusCode(HttpStatus.UNAUTHORIZED.value()),
            )
    }
}
```

**Test usage:**

```kotlin
@Test
fun `given an invalid github access token when creating git account then return 401 unauthorized`() {
    GithubHelper.mockGetUserUnauthorized()

    given()
        .header("Authorization", "Bearer ${generateToken(userId)}")
        .header("Host", "my-project.localhost.com")
        .put("/v1/projects/git?access_token=invalid-token")
        .then()
        .statusCode(HttpStatus.SC_UNAUTHORIZED)
}
```

**Rules:**
- One object helper per external API domain (e.g., `GithubHelper`, `SlackHelper`).
- Each helper method mocks exactly one scenario — one method per response variant.
- Always call `<Helper>.reset()` in `beforeEach` so expectations do not leak between tests.
- Add `<api>.url: http://127.0.0.1:8081` to `src/test/resources/application.yml` for each mocked client.
- Never use MockServer for internal services — only for real external HTTP dependencies.

---

## Review Flow

When asked to review or fix existing integration tests, check every item below and report all findings before applying any change:

1. Check every test name against the Given/When/Then convention and rename those that don't comply.
2. Check that every field validation constraint on the request DTO has a corresponding test.
3. Check that every service-level business rule has a corresponding test.
4. Check that every success path test asserts the database state, not only the response.
5. Check for logic bugs: a test named for one scenario but actually triggering another (e.g., invalid email used in a password test).

### Bug Reporting Protocol

When a bug is found in an existing test (logic error, wrong assertion, mismatched scenario, incorrect status code, missing DB assertion, etc.), never apply the fix silently.

Follow this protocol:
1. **Describe the bug** — state clearly what is wrong and in which test.
2. **Explain the impact** — what false confidence does this test give? What real scenario is it failing to cover?
3. **Present correction options** — list at least one way to fix it, more if there are meaningful trade-offs.
4. **Wait for confirmation** — only apply the fix after the user approves the chosen approach.

**Report format:**

```
Bug found: `<test name>`

Problem: <what is wrong>
Impact: <what this test falsely guarantees or fails to cover>

Fix options:
  A) <description of option A>
  B) <description of option B — if applicable>

Awaiting your confirmation before applying.
```

**Example:**

```
Bug found: `given an password should create with a valid password`

Problem: the request uses `email = "email.com"` which is an invalid email format.
The test hits the email validation error before ever reaching password validation,
so it never actually tests the password constraint.

Impact: password format validation has zero real coverage despite appearing covered.

Fix options:
  A) Fix the email to a valid one (e.g., "john@example.com") so the password constraint is actually exercised.
  B) Remove this test and add a dedicated one with a valid email and an invalid password.

Awaiting your confirmation before applying.
```
