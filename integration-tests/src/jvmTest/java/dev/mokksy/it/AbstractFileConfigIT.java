package dev.mokksy.it;

import dev.mokksy.Mokksy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractFileConfigIT {

    protected final HttpClient httpClient = HttpClient.newHttpClient();

    protected abstract String getBaseUrl();

    // region plain responses

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
    void get_returnsPlainResponseFromFileConfig() throws Exception {
        var response = get("/ping");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("{\"response\":\"Pong\"}");
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

    // endregion

    // region plain text stream

    @Test
    void get_returnsPlainTextStreamFromFileConfig() throws Exception {
        var response = get("/stream");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type")).hasValue("text/plain; charset=UTF-8");
        assertThat(response.body()).isEqualTo("Hello World");
    }

    protected HttpResponse<String> get(String path) throws IOException, InterruptedException {
        return httpClient.send(
            HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + path))
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
    }

    protected HttpResponse<String> post(String path, String body) throws IOException, InterruptedException {
        return httpClient.send(
            HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
    }
}
