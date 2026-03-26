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

- Prefer integration tests over unit tests for better coverage and less fragility
- Use unit tests only for edge cases where integration tests are hard to write
- Never mock Ktor request/response types — use `testApplication` instead
- Test new and modified code

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
