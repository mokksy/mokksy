package dev.mokksy.it;

import dev.mokksy.mokksy.MokksyServerJava;
import dev.mokksy.mokksy.StubConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MokksyJavaIT {

    private final MokksyServerJava mokksy = new MokksyServerJava();
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

    // region GET

    @Test
    void get_shouldReturn200WithBody() throws IOException, InterruptedException {
        mokksy.get(spec -> spec.path("/hello"))
            .respondsWith(builder -> builder.setBody("Hello, World!"));

        var response = get("/hello");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("Hello, World!");
    }

    @Test
    void get_shouldReturn404WhenNoStubMatches() throws IOException, InterruptedException {
        var response = get("/no-stub");

        assertThat(response.statusCode()).isEqualTo(404);
    }

    @Test
    void get_shouldMatchByHeaderValue() throws IOException, InterruptedException {
        mokksy.get(spec -> {
            spec.path("/secured");
            spec.containsHeader("X-Api-Key", "secret");
        }).respondsWith(builder -> builder.setBody("authorized"));

        var authorized = httpClient.send(
            HttpRequest.newBuilder()
                .uri(URI.create(mokksy.baseUrl() + "/secured"))
                .header("X-Api-Key", "secret")
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
        var unauthorized = get("/secured");

        assertThat(authorized.statusCode()).isEqualTo(200);
        assertThat(unauthorized.statusCode()).isEqualTo(404);
    }

    // endregion

    // region POST

    @Test
    void post_shouldReturn201WithLocationHeader() throws IOException, InterruptedException {
        var expectedBody = "{\"id\":\"42\"}";

        mokksy.post(spec -> spec.path("/items"))
            .respondsWith(builder -> {
                builder.setBody(expectedBody);
                builder.httpStatus(201);
                builder.addHeader("Location", "/items/42");
            });

        var response = post("/items", "{\"name\":\"widget\"}");

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.body()).isEqualTo(expectedBody);
        assertThat(response.headers().firstValue("Location")).hasValue("/items/42");
    }

    // endregion

    // region StubConfiguration

    @Test
    void removeAfterMatch_shouldReturn404OnSecondRequest() throws IOException, InterruptedException {
        mokksy.get(new StubConfiguration("once-only", true), spec -> spec.path("/once"))
            .respondsWith(builder -> builder.setBody("First!"));

        var first = get("/once");
        var second = get("/once");

        assertThat(first.statusCode()).isEqualTo(200);
        assertThat(second.statusCode()).isEqualTo(404);
    }

    // endregion

    // region Verification

    @Test
    void verifyNoUnmatchedStubs_shouldThrowWhenStubNeverCalled() {
        mokksy.get(spec -> spec.path("/never-called"))
            .respondsWith(builder -> builder.setBody("unreachable"));

        assertThatThrownBy(mokksy::verifyNoUnmatchedStubs)
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("/never-called");
    }

    @Test
    void verifyNoUnexpectedRequests_shouldThrowWhenRequestHadNoStub()
        throws IOException, InterruptedException {
        get("/unexpected");

        assertThatThrownBy(mokksy::verifyNoUnexpectedRequests)
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("/unexpected");
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
