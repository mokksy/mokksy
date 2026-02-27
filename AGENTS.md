# Mokksy and AI-Mocks Project Guidelines

Read the project overview from README.md

## Development Guidelines

- Use `git mv` command when renaming files to preserve git history, if possible
- Never commit or push changes to git automatically. It should be done manually.
- Ensure new code follows existing code style and design patterns.
- Use MCP servers, if available, to edit code and run tests. Run terminal commands directly.

### Code Style

#### Kotlin

- Follow Kotlin coding conventions
- Use the provided `.editorconfig` for consistent formatting
- Use Kotlin typesafe DSL builders where possible and prioritize fluent builders style over standard builder methods.
  If DSL builders produce less readable code, use standard setter methods.
- Prefer DSL builder style (method with lambda blocks) over constructors, if possible.
- Use Kotlin's `val` for immutable properties and `var` for mutable properties
- Use fully qualified imports instead of star imports
- Ensure to preserve backward compatibility when making changes
- Use `// region <name>` / `// endregion` comments to group related members in long files, enabling IntelliJ IDEA code folding

#### Java

- Use the provided `.editorconfig` for consistent formatting
- For Java code, prefer fluent DSL style over standard bean getters and setter methods

### Testing

- Write comprehensive tests for new features
- Use function `Names with backticks` for test methods in Kotlin, e.g. "fun `should return 200 OK`()"
- Avoid writing KDocs for tests, keep code self-documenting
- Write Kotlin tests with [kotlin-test](https://github.com/JetBrains/kotlin/tree/master/libraries/kotlin.test),
  [mockk](https://mockk.io/) and [Kotest-assertions](https://kotest.io/docs/assertions/assertions.html)
  with infix form assertions `shouldBe` instead of `assertEquals`.
- Use Kotest's `withClue("<failure reason>")` to describe failure reasons, but only when the assertion is NOT obvious.
  Remove obvious cases for simplicity.
- If multiple assertions exist against nullable field, first check for null, e.g.:
  `params shouldNotBeNull { params.id shouldBe 1 }`
- For testing json serialization use [Kotest-assertions-json](https://kotest.io/docs/assertions/json/json-overview.html)
  assertions, e.g. `shouldEqualJson` and never compare substrings.
- Use `assertSoftly(subject) { ... }` to perform multiple assertions. Never use `assertSoftly { }` to verify properties
  of different subjects, or when there is only one assertion per subject. Avoid using `assertSoftly(this) { ... }`
- Combine tests that exercise the **same scenario** but assert different properties into a single test using
  `assertSoftly(subject) { ... }`. Do not split one logical test case across multiple `@Test` functions just to
  have one assertion per function.
- Use `shouldContain` (from `io.kotest.matchers.string`) for string containment instead of `.contains(...) shouldBe true`.
- Prioritize test readability
- When asked to write tests in Java: use JUnit5, Mockito, AssertJ core

#### Testing Ktor request/response handling

- **Never mock Ktor internal classes** (`ApplicationRequest`, `ApplicationCall`, `Headers`, etc.) with mockk.
  These classes carry live pipeline state that cannot be faithfully reproduced by mocks, causing
  `ConcurrentModificationException` and other subtle failures.
- **Use `testApplication`** from `ktor-server-test-host` to test any code that operates on a real
  `ApplicationRequest`. See `CapturedRequestTest` and `RequestSpecificationTest` for reference patterns.
- `testApplication` is **not** a suspend function — it runs its block via an internal `runBlocking`.
  Do **not** wrap it in `runTest`; that creates a nested event loop and virtual-time control is lost.
  Use it directly as the test body: `fun myTest() = testApplication { ... }`.
- Install `DoubleReceive` whenever the code under test reads the request body more than once (e.g.
  when both `body` and `bodyString` matchers are active in `RequestSpecification`).
- Install `ContentNegotiation { json() }` when the handler needs to deserialize a typed request body
  via `call.receive(MyClass::class)`.

### Documentation

- Update README files when adding new features
- Document API changes in the appropriate module's documentation
- Write tutorials in Hugo markdown /docs/content/docs
- Make sure that in production code interfaces and abstract classes are properly documented. Avoid adding KDocs to
  override functions to avoid verbosity.
- Update KDocs when api is changed.
- When referring classes in KDoc, use references: `[SendMessageRequest]` instead of `SendMessageRequest`.
- Add brief code examples to KDoc
- Add links to specifications, if known. Double-check that the link actual and pointing exactly to the specification.
  Never add broken or not accurate links.

#### README code examples and KNIT

README.md code examples are compiled and executed as tests via the
[Knit](https://github.com/Kotlin/kotlinx-knit) tool. This ensures the README stays in sync with the
actual API.

- **Configuration**: `knit.properties` at the project root defines:
  - `knit.dir=mokksy/build/generated/knit/test/kotlin/` — where generated files land
  - `knit.package=dev.mokksy.knit` — package for generated test classes
- **Directives** embedded as HTML comments in the Markdown:
  - `<!--- CLEAR -->` — resets accumulated includes for a new standalone example
  - `<!--- INCLUDE ... -->` — code inserted before/around the next visible snippet (imports, class
    wrapper, `@Test` method open); not rendered in the docs
  - `<!--- SUFFIX ... -->` — code appended after all preceding snippets (closing braces)
  - `<!--- KNIT filename.kt -->` — triggers generation of `filename.kt` in `knit.dir`; all
    preceding INCLUDE/SUFFIX/code blocks since the last CLEAR are concatenated into this file
- Each generated file becomes a compilable test class. Run `./gradlew :mokksy:knit` to regenerate,
  then `./gradlew :mokksy:jvmTest` to verify examples compile and pass.
- Visible code blocks between INCLUDE and a closing INCLUDE/SUFFIX are included verbatim; the
  surrounding class/function context comes from INCLUDE directives.
- Code snippets **outside** KNIT blocks (reference sections, bullet examples) are plain Markdown
  fenced code and are not compiled — keep them syntactically correct but they are not executed.

#### README vs Hugo docs

`README.md` (in `mokksy-server/`) and the Hugo page (`docs/content/docs/mokksy.md`) cover the same
feature set from different angles:

- README is the canonical quick-reference; uses KNIT-compiled examples.
- Hugo docs are the full website narrative; uses Hugo shortcodes (`{{< tabs >}}` etc.) and may
  include installation tables, richer explanations, and cross-links.
- **Keep both in sync** when adding or changing features. A feature documented in one must appear
  in the other.

The following Mokksy features must be documented in both places when implemented:
`respondsWithStream` (chunked streaming), response `delay`/`delayMillis`, stub `priority`,
`removeAfterMatch` (`StubConfiguration`), other HTTP methods (PUT/PATCH/DELETE/HEAD/OPTIONS),
and typed body matching via `bodyMatchesPredicate`.

### Project Documentation

The project uses **Dokka** - For API documentation generation from code

#### Local Documentation Generation

To generate documentation locally:

1. Generate API documentation with Dokka:
   ```shell
   ./gradlew :docs:dokkaGenerate
   ```
   This will generate API documentation in `docs/public/apidocs/`.

#### Documentation Content Guidelines

When creating or updating documentation, focus on the following aspects:

1. **API Documentation**:

- Ensure all public APIs have proper KDoc/Javadoc comments
  - Include examples of how to use the API
  - Document parameters, return values, and exceptions
  - Explain the purpose and behavior of each class and method

2. **User Guides and Tutorials**:

- Start with a clear introduction explaining what the module does
- Include step-by-step instructions with code examples
- Provide complete working examples that users can copy and adapt.
- Explain common use cases and best practices
- Include troubleshooting information for common issues
- **Double-check that documentation matches the existing code and tests. Never make up anything not implemented as
  code!**

4. **Contributions**

- Follow the guidelines in CONTRIBUTING.md
- Create pull requests for new features or bug fixes
- Ensure all tests pass before submitting
- Never commit and push automatically

### Getting Started

For detailed usage instructions and examples, refer to:

- Main README.md for project overview
- Module-specific README files for detailed API documentation
- Sample code in each module's samples directory

### Building the Project

```shell
gradle build
```

or using Make:

```shell
make
```
