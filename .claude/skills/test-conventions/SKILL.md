---
name: test-conventions
description: Enforces Mokksy testing conventions when writing or modifying test code, or implementing features that need test coverage.
user-invocable: true
---

# Mokksy Testing Conventions

You MUST follow these conventions when writing or modifying tests in this project.

## Test Coverage Rule

Before finishing any feature or bug fix, verify that all new and modified **production code**
has corresponding test coverage. Check the diff against the base branch or recent changes to
identify untested code paths. If coverage is missing, write the tests.

## General Principles

- Prefer **integration tests** over unit tests for better coverage and less fragility
- Use unit tests only for edge cases where integration tests are hard to write
- Always use current non-deprecated API in tests

## Kotlin Tests

Use Kotest-assertions. On multiplatorm use kotlin-test, on jvm target JUnit6. For Java tests use JUnit6, AssertJ core, Mockito.

### Naming and Structure

- Backtick test names: "fun `should return 200 OK`()"
- No KDocs on tests (don't delete existing ones)
- When testing suspend functions, make the test method `suspend` since JUnit6 supports it on JVM

### Assertions

- Infix assertions: `shouldBe` not `assertEquals`; `shouldContain` not `.contains(...) shouldBe true`
- `withClue("reason")` only when the assertion is not self-evident
- Nullable fields: check null first -- `params shouldNotBeNull { params.id shouldBe 1 }`
- JSON: use Kotest-assertions-json (`shouldEqualJson`), **never** substring comparison
- `assertSoftly(subject) { ... }` for multiple assertions on the same subject.
  Don't use for different subjects, single assertions, or with `this`.
  Combine same-scenario assertions into one test; don't split across multiple `@Test` functions

### Parameterized Tests

- Prefer `@ParameterizedTest` with `@ValueSource`/`@MethodSource` over duplicate `@Test` methods
- For HTTP methods: `@ValueSource(strings = ["GET", "POST", ...])` with `HttpMethod.parse(methodName)`

### Data in Tests

- No `Random`/`UUID` in assertions -- use `seed` only for path suffixes; fixed constants elsewhere

## Multiplatform Tests

- Call `mokksy.awaitStarted()` at the start of each `commonTest` test body, even after
  `startSuspend()` in `@BeforeTest` -- JS/wasmJS may start the test body before the server
  finishes binding
- **Path isolation**: in `@TestInstance(PER_CLASS)` tests (`AbstractIT` subclasses), include `seed`
  in every stub path (`"/resource-$seed"`) to prevent stub accumulation

## Testing Ktor Request/Response

- **Never mock** `ApplicationRequest`, `ApplicationCall`, `Headers` -- use `testApplication` from
  `ktor-server-test-host`. See `CapturedRequestTest`, `RequestSpecificationTest` for patterns
- `testApplication` uses internal `runBlocking` -- do **not** wrap in `runTest`
- Install `DoubleReceive` when body is read more than once
- Install `ContentNegotiation { json() }` when deserializing typed request bodies

## Java Tests

- Use JUnit5, Mockito, AssertJ core
- Prefer fluent DSL style over bean getters/setters

