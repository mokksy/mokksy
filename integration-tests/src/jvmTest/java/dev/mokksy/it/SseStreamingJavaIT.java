package dev.mokksy.it;

import dev.mokksy.Mokksy;
import dev.mokksy.mokksy.SseEvent;
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
        assertThat(response.body()).isEqualTo("data: Hello\r\n\r\ndata: World\r\n\r\n");
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
        assertThat(response.body()).isEqualTo("data: event-1\r\n\r\ndata: event-2\r\n\r\n");
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
        assertThat(response.body()).isEqualTo("data: A\r\n\r\ndata: B\r\n\r\ndata: C\r\n\r\n");
    }

    // endregion

    // region SseEvent factory

    @Test
    void sseEventData_shouldReturnSseWireFormat() throws IOException, InterruptedException {
        mokksy.get(spec -> spec.path("/sse-factory-data"))
            .respondsWithSseStream(builder -> builder
                .chunk(SseEvent.data("Hello"))
                .chunk(SseEvent.data("World")));

        var response = get("/sse-factory-data");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("data: Hello\r\n\r\ndata: World\r\n\r\n");
    }

    @Test
    void sseEventBuilder_shouldReturnSseWireFormatWithEventType() throws IOException, InterruptedException {
        mokksy.get(spec -> spec.path("/sse-factory-builder"))
            .respondsWithSseStream(builder -> builder
                .chunk(SseEvent.builder().data("payload").event("chat").id("1").build()));

        var response = get("/sse-factory-builder");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("event: chat\r\ndata: payload\r\nid: 1\r\n\r\n");
    }

    @Test
    void sseEventData_withPathShortcut_shouldWork() throws IOException, InterruptedException {
        mokksy.get("/sse-shortcut")
            .respondsWithSseStream(builder -> builder
                .chunk(SseEvent.data("shortcut-event")));

        var response = get("/sse-shortcut");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("data: shortcut-event\r\n\r\n");
    }

    @Test
    void sseEventBuilder_withEventField_shouldSerializeEventType() throws IOException, InterruptedException {
        mokksy.get("/sse-builder-event")
            .respondsWithSseStream(builder -> builder
                .chunk(SseEvent.builder().data("msg").event("chat.completion").build()));

        var response = get("/sse-builder-event");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("event: chat.completion\r\ndata: msg\r\n\r\n");
    }

    @Test
    void sseEventBuilder_withIdField_shouldSerializeId() throws IOException, InterruptedException {
        mokksy.get("/sse-builder-id")
            .respondsWithSseStream(builder -> builder
                .chunk(SseEvent.builder().data("msg").id("42").build()));

        var response = get("/sse-builder-id");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("data: msg\r\nid: 42\r\n\r\n");
    }

    @Test
    void sseEventBuilder_withRetryField_shouldSerializeRetry() throws IOException, InterruptedException {
        mokksy.get("/sse-builder-retry")
            .respondsWithSseStream(builder -> builder
                .chunk(SseEvent.builder().data("msg").retry(5000L).build()));

        var response = get("/sse-builder-retry");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("data: msg\r\nretry: 5000\r\n\r\n");
    }

    @Test
    void sseEventBuilder_withCommentsField_shouldSerializeComments() throws IOException, InterruptedException {
        mokksy.get("/sse-builder-comments")
            .respondsWithSseStream(builder -> builder
                .chunk(SseEvent.builder().data("msg").comments("keep-alive").build()));

        var response = get("/sse-builder-comments");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("data: msg\r\n: keep-alive\r\n\r\n");
    }

    @Test
    void sseEventBuilder_withAllFields_shouldSerializeAll() throws IOException, InterruptedException {
        mokksy.get("/sse-builder-all")
            .respondsWithSseStream(builder -> builder
                .chunk(SseEvent.builder()
                    .data("payload")
                    .event("message")
                    .id("99")
                    .retry(3000L)
                    .comments("debug")
                    .build()));

        var response = get("/sse-builder-all");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo(
            "event: message\r\ndata: payload\r\nid: 99\r\nretry: 3000\r\n: debug\r\n\r\n");
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
        assertThat(response.body()).isEqualTo("data: typed-event\r\n\r\n");
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
