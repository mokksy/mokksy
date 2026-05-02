package dev.mokksy.it;

import dev.mokksy.Mokksy;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MokksyJavaFileConfigIT extends AbstractFileConfigIT {

    private final Mokksy mokksy = Mokksy.create();

    @Override
    protected String getBaseUrl() {
        return mokksy.baseUrl();
    }

    @BeforeAll
    void setUp() throws URISyntaxException {
        var file = new File(requireNonNull(getClass().getResource("/it-stubs.yaml")).toURI());
        mokksy.start();
        mokksy.loadStubsFromFile(file);
    }

    @AfterAll
    void tearDown() {
        mokksy.shutdown();
    }

    // endregion

    // region error cases

    @Test
    void loadStubsFromFile_failsWithClearMessageWhenFileMissing() {
        try (var server = Mokksy.create()) {
            var missing = new File("/nonexistent/path/stubs.yaml");

            assertThatThrownBy(() -> server.loadStubsFromFile(missing))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found")
                .hasMessageContaining(missing.getAbsolutePath());
        }
    }

    @Test
    void loadStubsFromFile_failsWithClearMessageForInvalidYaml() throws IOException {
        try (var server = Mokksy.create()) {
            Path badYaml = Files.createTempFile("bad", ".yaml");
            Files.writeString(badYaml, "stubs: [{ path: /x, response: { status: not-a-number } }]");

            try {
                assertThatThrownBy(() -> server.loadStubsFromFile(badYaml.toFile()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid YAML");
            } finally {
                Files.deleteIfExists(badYaml);
            }
        }
    }

    @Test
    void loadStubsFromFile_failsWithClearMessageForUnknownMethod() throws IOException {
        try (var server = Mokksy.create()) {
            // language=yaml
            var yaml = """
                    stubs:
                      - name: bad-method
                        method: BREW
                        path: /coffee
                        response:
                          body: ok
                    """;
            Path file = Files.createTempFile("stubs", ".yaml");
            Files.writeString(file, yaml);

            try {
                assertThatThrownBy(() -> server.loadStubsFromFile(file.toFile()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("BREW");
            } finally {
                Files.deleteIfExists(file);
            }
        }
    }

    @Test
    void loadStubsFromFile_failsWithClearMessageForSseWithNoChunks() throws IOException {
        try (var server = Mokksy.create()) {
            // language=yaml
            var yaml = """
                stubs:
                  - name: empty-sse
                    path: /sse
                    response:
                      type: sse
                """;
            Path file = Files.createTempFile("stubs", ".yaml");
            Files.writeString(file, yaml);

            try {
                assertThatThrownBy(() -> server.loadStubsFromFile(file.toFile()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("empty-sse")
                    .hasMessageContaining("chunk");
            } finally {
                Files.deleteIfExists(file);
            }
        }
    }

    // endregion

    // region env / system property loading

    @Test
    void loadStubsFromEnv_readsPathFromSystemProperty() throws URISyntaxException {
        Assumptions.assumeTrue(
            System.getenv("MOKKSY_CONFIG") == null,
            "MOKKSY_CONFIG env var is set — skipping"
        );
        var file = new File(requireNonNull(getClass().getResource("/it-stubs.yaml")).toURI());
        System.setProperty("mokksy.config", file.getAbsolutePath());
        try {
            try (var server = Mokksy.create()) {
                server.loadStubsFromEnv(); // should not throw
            }
        } finally {
            System.clearProperty("mokksy.config");
        }
    }

    @Test
    void loadStubsFromEnv_continueIfNeitherEnvVarNorPropertyIsSet() {
        Assumptions.assumeTrue(System.getenv("MOKKSY_CONFIG") == null, "MOKKSY_CONFIG env var is set — skipping");
        System.clearProperty("mokksy.config");
        try (var server = Mokksy.create()) {
            assertThat(server.loadStubsFromEnv()).isSameAs(server);
        }
    }

    // endregion
}
