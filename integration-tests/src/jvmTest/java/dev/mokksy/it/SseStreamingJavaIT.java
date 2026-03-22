package dev.mokksy.it;

import dev.mokksy.Mokksy;
import io.ktor.sse.ServerSentEvent;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SseStreamingJavaIT {

    private final Mokksy mokksy = Mokksy.create().start();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @AfterAll
    void tearDown() throws IOException {
        if (httpClient instanceof Closeable c) {
            c.close();
        }
        mokksy.close();
    }

    // region chunks

    @Test
    void sseChunks_shouldReturnSseWireFormat() throws IOException, InterruptedException {
        mokksy.get(spec -> spec.path("/sse-chunks"))
            .respondsWithSseStream(builder -> builder
                .chunk(new ServerSentEvent("Hello", null, null, null, null))
                .chunk(new ServerSentEvent("World", null, null, null, null)));

        var response = get("/sse-chunks");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("data: Hello\r\ndata: World\r\n");
        assertThat(response.headers().firstValue("Content-Type"))
            .hasValue("text/event-stream; charset=UTF-8");
    }

    @Test
    void sseListChunks_shouldReturnSseWireFormat() throws IOException, InterruptedException {
        mokksy.get(spec -> spec.path("/sse-list"))
            .respondsWithSseStream(builder -> builder
                .chunks(List.of(
                    new ServerSentEvent("event-1", null, null, null, null),
                    new ServerSentEvent("event-2", null, null, null, null))));

        var response = get("/sse-list");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("data: event-1\r\ndata: event-2\r\n");
        assertThat(response.headers().firstValue("Content-Type"))
            .hasValue("text/event-stream; charset=UTF-8");
    }

    // endregion

    // region delays

    @Test
    void sseInitialDelay_shouldDelayResponseByAtLeastSpecifiedMillis() {
        mokksy.get(spec -> spec.path("/sse-initial-delay"))
            .respondsWithSseStream(builder -> builder
                .chunk(new ServerSentEvent("ping", null, null, null, null))
                .delayMillis(200L));

        var response = TimingAssertions.takesAtLeast(200L,
            () -> get("/sse-initial-delay"));

        assertThat(response.statusCode()).isEqualTo(200);
    }

    @Test
    void sseDelayBetweenChunks_shouldAddDelayBetweenEachChunk() {
        mokksy.get(spec -> spec.path("/sse-between-delay"))
            .respondsWithSseStream(builder -> builder
                .chunk(new ServerSentEvent("A", null, null, null, null))
                .chunk(new ServerSentEvent("B", null, null, null, null))
                .chunk(new ServerSentEvent("C", null, null, null, null))
                .delayBetweenChunksMillis(100L));

        var response = TimingAssertions.takesAtLeast(200L,
            () -> get("/sse-between-delay"));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("data: A\r\ndata: B\r\ndata: C\r\n");
    }

    // endregion

    // region typed SSE

    @Test
    void typedSseChunks_shouldReturnSseWireFormat() throws IOException, InterruptedException {
        mokksy.get(spec -> spec.path("/sse-typed"))
            .respondsWithSseStream(String.class, builder -> builder
                .chunk(new ServerSentEvent("typed-event", null, null, null, null)));

        var response = get("/sse-typed");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("data: typed-event\r\n");
    }

    // endregion

    private HttpResponse<String> get(String path) throws IOException, InterruptedException {
        return httpClient.send(
            HttpRequest.newBuilder()
                .uri(URI.create(mokksy.baseUrl() + path))
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
    }
}
