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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StreamingJavaIT {

    private final Mokksy mokksy = Mokksy.create().start();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    /**
     * Performs class-level teardown by closing the HTTP client if closable and shutting down the Mokksy server.
     *
     * @throws IOException if closing the HTTP client or the Mokksy server fails
     */
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
    void streamChunks_shouldBeConsumedLazily()
        throws IOException, InterruptedException {
        var consumed = new AtomicBoolean(false);
        var stream = Stream.of("lazy-value").peek(x -> consumed.set(true));

        mokksy.get(spec -> spec.path("/stream-lazy"))
            .respondsWithStream(builder -> builder.chunks(stream));

        assertThat(consumed.get())
            .as("stream must NOT be consumed at stub registration time")
            .isFalse();

        var response = get("/stream-lazy");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("lazy-value");
        assertThat(consumed.get())
            .as("stream must be consumed at stub matching time")
            .isTrue();
    }

    /**
     * Verifies that a streaming response uses an explicitly specified Content-Type instead of the default.
     * <p>
     * Configures Mokksy to stream two JSON chunks with Content-Type "application/x-ndjson", performs a GET
     * request to "/stream-content-type", and asserts the concatenated body and overridden Content-Type header.
     *
     * @throws IOException          if an I/O error occurs while sending the request or reading the response
     * @throws InterruptedException if the request thread is interrupted
     */
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

    /**
     * Verifies that configuring an initial delay on a streaming response postpones the HTTP response by at least the configured duration.
     * <p>
     * The test configures the server to stream a single chunk with an initial delay of 200 ms, issues a request and asserts that the request takes at least 200 ms and that the response status is 200.
     */

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

    /**
     * Verifies that a streamed response with an inter-chunk delay pauses between chunks and concatenates them.
     * <p>
     * Asserts the request takes at least 150 ms (two intervals of 100 ms), the response status is 200 and the body equals "ABC".
     */
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

    /**
     * Send an HTTP GET to the Mokksy server at the given path and return the response.
     *
     * @param path the request path appended to the Mokksy base URL (for example "/stream-list")
     * @return the HTTP response with the response body decoded as a String and associated metadata
     * @throws IOException          if an I/O error occurs when sending or receiving
     * @throws InterruptedException if the operation is interrupted while waiting for the response
     */

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
