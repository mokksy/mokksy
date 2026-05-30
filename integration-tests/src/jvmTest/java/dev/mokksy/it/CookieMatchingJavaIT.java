package dev.mokksy.it;

import dev.mokksy.Mokksy;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CookieMatchingJavaIT {

    private final Mokksy mokksy = Mokksy.create();
    private HttpClient httpClient;

    @BeforeAll
    void setUp() {
        mokksy.start();
        httpClient = HttpClient.newHttpClient();
    }

    @AfterAll
    void tearDown() {
        mokksy.shutdown();
    }

    @Test
    void shouldMatchCookieByExactValue() throws IOException, InterruptedException {
        var path = "/java-cookie-exact-" + UUID.randomUUID();

        mokksy.get(spec -> spec
            .path(path)
            .cookie("session", "abc")
        ).respondsWith(rb -> rb.body("matched"));

        var matched = httpClient.send(
            HttpRequest.newBuilder()
                .uri(URI.create(mokksy.baseUrl() + path))
                .header("Cookie", "session=abc")
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );

        var notMatched = httpClient.send(
            HttpRequest.newBuilder()
                .uri(URI.create(mokksy.baseUrl() + path))
                .header("Cookie", "session=wrong")
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );

        assertThat(matched.statusCode()).isEqualTo(200);
        assertThat(matched.body()).isEqualTo("matched");
        assertThat(notMatched.statusCode()).isEqualTo(404);
    }

    @Test
    void shouldMatchCookieByPredicate() throws IOException, InterruptedException {
        var path = "/java-cookie-predicate-" + UUID.randomUUID();

        mokksy.get(spec -> spec
            .path(path)
            .cookieMatches("session", value -> value != null && value.startsWith("sess-"))
        ).respondsWith(rb -> rb.body("predicate-matched"));

        var matched = httpClient.send(
            HttpRequest.newBuilder()
                .uri(URI.create(mokksy.baseUrl() + path))
                .header("Cookie", "session=sess-xyz")
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );

        var notMatched = httpClient.send(
            HttpRequest.newBuilder()
                .uri(URI.create(mokksy.baseUrl() + path))
                .header("Cookie", "session=wrong")
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );

        assertThat(matched.statusCode()).isEqualTo(200);
        assertThat(matched.body()).isEqualTo("predicate-matched");
        assertThat(notMatched.statusCode()).isEqualTo(404);
    }

    @Test
    void shouldMatchAbsentCookie() throws IOException, InterruptedException {
        var path = "/java-cookie-absent-" + UUID.randomUUID();

        mokksy.get(spec -> spec
            .path(path)
            .cookieAbsent("session")
        ).respondsWith(rb -> rb.body("absent-matched"));

        var matched = httpClient.send(
            HttpRequest.newBuilder()
                .uri(URI.create(mokksy.baseUrl() + path))
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );

        var notMatched = httpClient.send(
            HttpRequest.newBuilder()
                .uri(URI.create(mokksy.baseUrl() + path))
                .header("Cookie", "session=abc")
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );

        assertThat(matched.statusCode()).isEqualTo(200);
        assertThat(matched.body()).isEqualTo("absent-matched");
        assertThat(notMatched.statusCode()).isEqualTo(404);
    }
}
