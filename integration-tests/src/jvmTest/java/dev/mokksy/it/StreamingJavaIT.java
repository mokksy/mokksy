package dev.mokksy.it;

import dev.mokksy.Mokksy;
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
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StreamingJavaIT {

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
    void listChunks_shouldReturnConcatenatedBody() throws IOException, InterruptedException {
        mokksy.get(spec -> spec.path("/stream-list"))
            .respondsWithStream(builder -> builder
                .chunks(List.of("Hello", "World")));

        var response = get("/stream-list");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("HelloWorld");
        assertThat(response.headers().firstValue("Content-Type"))
            .hasValue("text/event-stream; charset=UTF-8");
    }

    @Test
    void streamChunks_shouldReturnConcatenatedBody() throws IOException, InterruptedException {
        mokksy.get(spec -> spec.path("/stream-stream"))
            .respondsWithStream(builder -> builder
                .chunks(Stream.of("Foo", "Bar")));

        var response = get("/stream-stream");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("FooBar");
        assertThat(response.headers().firstValue("Content-Type"))
            .hasValue("text/event-stream; charset=UTF-8");
    }

    @Test
    void customContentType_shouldOverrideDefault() throws IOException, InterruptedException {
        mokksy.get(spec -> spec.path("/stream-content-type"))
            .respondsWithStream(builder -> builder
                .chunks(List.of("{\"value\":1}", "{\"value\":2}"))
                .contentType("application/x-ndjson"));

        var response = get("/stream-content-type");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("{\"value\":1}{\"value\":2}");
        assertThat(response.headers().firstValue("Content-Type"))
            .hasValue("application/x-ndjson");
    }

    // endregion

    // region delay

    @Test
    void initialDelay_shouldDelayResponseByAtLeastSpecifiedMillis() {
        mokksy.get(spec -> spec.path("/stream-initial-delay"))
            .respondsWithStream(builder -> builder
                .chunks(List.of("ping"))
                .delayMillis(200L));

        var response = TimingAssertions.takesAtLeast(200L,
            () -> get("/stream-initial-delay"));

        assertThat(response.statusCode()).isEqualTo(200);
    }

    @Test
    void delayBetweenChunks_shouldAddDelayBetweenEachChunk() {
        mokksy.get(spec -> spec.path("/stream-between-delay"))
            .respondsWithStream(builder -> builder
                .chunks(List.of("A", "B", "C"))
                .delayBetweenChunksMillis(100L));

        var response = TimingAssertions.takesAtLeast(150L, // 2 × 100 ms between 3 chunks
            () -> get("/stream-between-delay"));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("ABC");
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

    // endregion
}
