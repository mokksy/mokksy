# Mokksy Project Guidelines

## Development

- Use `git mv` when renaming files to preserve history
- Never commit or push to git automatically
- Use MCP servers when available to edit code and run tests
- Preserve backward compatibility when making changes

### Kotlin Style

- Prefer DSL builders (lambda blocks) over constructors; fall back to standard setters when DSL hurts readability
- Use fully qualified imports (no star imports)
- Use `// region <name>` / `// endregion` to group related members in long files

### Java Style

- Prefer fluent DSL style over bean getters/setters

### Testing

Prefer integration tests over unit tests for better coverage and less fragility.
Use unit tests for edge cases where integration tests are hard to write.

#### Kotlin Tests

Use [kotlin-test](https://github.com/JetBrains/kotlin/tree/master/libraries/kotlin.test)
and [Kotest-assertions](https://kotest.io/docs/assertions/assertions.html).
For Java tests use JUnit5, Mockito, AssertJ core.

- Backtick test names: `fun `should return 200 OK`()`
- No KDocs on tests (don't delete existing ones)
- Infix assertions: `shouldBe` not `assertEquals`; `shouldContain` not `.contains(...) shouldBe true`
- `withClue("reason")` only when the assertion is not self-evident
- Nullable fields: check null first — `params shouldNotBeNull { params.id shouldBe 1 }`
- JSON: use [Kotest-assertions-json](https://kotest.io/docs/assertions/json/json-overview.html)
  (`shouldEqualJson`), never substring comparison
- `assertSoftly(subject) { ... }` for multiple assertions on the same subject.
  Don't use for different subjects, single assertions, or with `this`.
  Combine same-scenario assertions into one test; don't split across multiple `@Test` functions
- Prefer `@ParameterizedTest` with `@ValueSource`/`@MethodSource` over duplicate `@Test` methods.
  For HTTP methods: `@ValueSource(strings = ["GET", "POST", ...])` with `HttpMethod.parse(methodName)`
- No `Random`/`UUID` in assertions — use `seed` only for path suffixes; fixed constants elsewhere
- Always use current non-deprecated API in tests

#### Multiplatform Tests

- Call `mokksy.awaitStarted()` at the start of each `commonTest` test body, even after `startSuspend()`
  in `@BeforeTest` — JS/wasmJS may start the test body before the server finishes binding
- **Path isolation**: in `@TestInstance(PER_CLASS)` tests (`AbstractIT` subclasses), include `seed` in
  every stub path (`"/resource-$seed"`) to prevent stub accumulation

#### Testing Ktor Request/Response

- **Never mock** `ApplicationRequest`, `ApplicationCall`, `Headers` — use `testApplication` from
  `ktor-server-test-host`. See `CapturedRequestTest`, `RequestSpecificationTest` for patterns
- `testApplication` uses internal `runBlocking` — do **not** wrap in `runTest`
- Install `DoubleReceive` when body is read more than once
- Install `ContentNegotiation { json() }` when deserializing typed request bodies

### Documentation

- Document interfaces and abstract classes; skip KDocs on overrides
- Use KDoc references: `[ClassName]` not `ClassName`
- Add brief code examples to KDoc
- Add links to specs only when verified accurate — never add broken links
- Documentation must match existing code — never describe unimplemented features

#### README Code Examples (KNIT)

README examples are compiled/tested via [Knit](https://github.com/Kotlin/kotlinx-knit).

- Config in `knit.properties`: `knit.dir=mokksy/build/generated/knit/test/kotlin/`,
  `knit.package=dev.mokksy.knit`
- Directives (HTML comments): `CLEAR` (reset), `INCLUDE` (wrapper code), `SUFFIX` (closing code),
  `KNIT filename.kt` (generate)
- Place `CLEAR` and opening `INCLUDE` immediately before the first `@Test` INCLUDE, not before
  prose code blocks
- Run `make knit` (or `./gradlew :mokksy:knit`) after editing examples to verify compilation

### Building

```shell
make          # or: gradle build
```

Generate API docs: `./gradlew :docs:dokkaGenerate` → output in `docs/public/apidocs/`
