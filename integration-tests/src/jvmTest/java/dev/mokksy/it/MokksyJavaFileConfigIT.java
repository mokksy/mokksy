package dev.mokksy.it;

import dev.mokksy.Mokksy;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MokksyJavaFileConfigIT {

    private final Mokksy mokksy = Mokksy.create();
    private HttpClient httpClient;

    @BeforeAll
    void setUp() throws URISyntaxException {
        var file = new File(getClass().getResource("/it-stubs.yaml").toURI());
        mokksy.start();
        mokksy.loadStubsFromFile(file);
        httpClient = HttpClient.newHttpClient();
    }

    @AfterAll
    void tearDown() {
        mokksy.shutdown();
    }

    // region plain responses

    @Test
    void get_returnsPlainResponseFromFileConfig() throws Exception {
        var response = get("/ping");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("{\"response\":\"Pong\"}");
    }

    @Test
    void post_withBodyMatch_returnsConfiguredStatusAndHeaders() throws Exception {
        var response = post("/things", "{\"id\":\"42\"}");

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.body()).isEqualTo("{\"id\":\"42\",\"name\":\"thing-42\"}");
        assertThat(response.headers().firstValue("Location")).hasValue("/things/42");
        assertThat(response.headers().firstValue("Foo")).hasValue("bar");
    }

    @Test
    void post_withoutBodyMatch_returns404() throws Exception {
        var response = post("/things", "{\"id\":\"99\"}");

        assertThat(response.statusCode()).isEqualTo(404);
    }

    @Test
    void get_delayedStub_returnsResponse() throws Exception {
        var response = get("/delayed");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("ok");
    }

    // endregion

    // region SSE stream

    @Test
    void post_returnsSseStreamFromFileConfig() throws Exception {
        var response = post("/sse", "");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type")).hasValue("text/event-stream; charset=UTF-8");
        assertThat(response.body()).isEqualTo("data: One\r\n\r\ndata: Two\r\n\r\n");
    }

    // endregion

    // region plain text stream

    @Test
    void get_returnsPlainTextStreamFromFileConfig() throws Exception {
        var response = get("/stream");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type")).hasValue("text/plain; charset=UTF-8");
        assertThat(response.body()).isEqualTo("Hello World");
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
        var server = Mokksy.create();
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

            assertThatThrownBy(server::loadStubsFromEnv)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MOKKSY_CONFIG")
                .hasMessageContaining("mokksy.config");
        }
    }

    // endregion

    // region helpers

    private HttpResponse<String> get(String path) throws IOException, InterruptedException {
        return httpClient.send(
            HttpRequest.newBuilder()
                .uri(URI.create(mokksy.baseUrl() + path))
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
    }

    private HttpResponse<String> post(String path, String body) throws IOException, InterruptedException {
        return httpClient.send(
            HttpRequest.newBuilder()
                .uri(URI.create(mokksy.baseUrl() + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
    }

    // endregion
}
