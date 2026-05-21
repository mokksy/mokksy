package dev.mokksy.it;

import dev.mokksy.Mokksy;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DockerJavaIT extends AbstractFileConfigIT {

    private final String dockerImageName =
        System.getProperty("dockerImageName", "mokksy/server-jvm") + ":" +
        System.getProperty("dockerImageTag", "snapshot");

    @SuppressWarnings({"rawtypes", "unchecked"})
    private final GenericContainer container = new GenericContainer(DockerImageName.parse(dockerImageName))
        .withImagePullPolicy(imageName -> false)// never pull remote image
        .withEnv("MOKKSY_CONFIG", "/config/it-stubs.yaml")
        .withCopyFileToContainer(
            MountableFile.forClasspathResource("/it-stubs.yaml"),
            "/config/it-stubs.yaml"
        )
        .withExposedPorts(8080)
        .waitingFor(Wait.forLogMessage(".*Responding at.*", 1))
        .withLogConsumer((Consumer<OutputFrame>) frame -> System.out.println("🐳 " + frame.getUtf8StringWithoutLineEnding()))
        .withStartupTimeout(Duration.ofSeconds(10));

    @BeforeAll
    void beforeAll() {
        container.start();
    }

    @AfterAll
    void afterAll() {
        container.stop();
    }

    // region plain responses

    @Override
    protected String getBaseUrl() {
        return "http://" + container.getHost() + ":" + container.getFirstMappedPort();
    }

    void get_returnsPlainResponseFromFileConfig() throws Exception {
        var response = get("/ping");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("{\"response\":\"Pong\"}");
    }

    // endregion

    // region error cases

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
    void loadStubsFromEnv_throwsWhenNeitherEnvVarNorPropertyIsSet() {
        Assumptions.assumeTrue(System.getenv("MOKKSY_CONFIG") == null, "MOKKSY_CONFIG env var is set — skipping");
        System.clearProperty("mokksy.config");
        try (var server = Mokksy.create()) {
            assertThat(server.loadStubsFromEnv()).isSameAs(server);
        }
    }

    // endregion
}
