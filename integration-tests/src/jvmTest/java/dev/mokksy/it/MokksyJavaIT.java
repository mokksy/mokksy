package dev.mokksy.it;

import dev.mokksy.Mokksy;
import dev.mokksy.mokksy.StubConfiguration;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MokksyJavaIT {

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

    // region GET

    @Test
    void get_shouldReturn200WithBody() throws IOException, InterruptedException {
        mokksy.get(spec -> spec.path("/hello"))
            .respondsWith(builder -> builder.body("Hello, World!"));

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
        }).respondsWith(builder -> builder.body("authorized"));

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

    // region PUT / DELETE / PATCH / HEAD / OPTIONS

    @ParameterizedTest
    @ValueSource(strings = {"PUT", "DELETE", "PATCH"})
    void shouldRespondToMutatingMethods(String method) throws IOException, InterruptedException {
        var path = "/" + method.toLowerCase() + "-test";
        var expectedBody = "response-for-" + method;

        mokksy.method(method, spec -> spec.path(path))
            .respondsWith(b -> b.body(expectedBody));

        var response = send(method, path, null);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo(expectedBody);
    }

    @Test
    void head_shouldReturn200WithEmptyBody() throws IOException, InterruptedException {
        mokksy.head(spec -> spec.path("/head-test"))
            .respondsWith(builder -> builder.body("ignored-by-protocol"));

        var response = send("HEAD", "/head-test", null);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEmpty();
    }

    @Test
    void options_shouldReturn200WithBody() throws IOException, InterruptedException {
        mokksy.options(spec -> spec.path("/options-test"))
            .respondsWith(builder -> builder.body("OK"));

        var response = send("OPTIONS", "/options-test", null);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("OK");
    }

    // endregion

    // region POST

    @Test
    void post_shouldReturn201WithLocationHeader() throws IOException, InterruptedException {
        var expectedBody = "{\"id\":\"42\"}";

        mokksy.post(spec -> spec.path("/items"))
            .respondsWith(builder -> builder
                .body(expectedBody)
                .status(201)
                .header("Location", "/items/42"));

        var response = post("/items", "{\"name\":\"widget\"}");

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.body()).isEqualTo(expectedBody);
        assertThat(response.headers().firstValue("Location")).hasValue("/items/42");
    }

    @Test
    void post_shouldMatchByBodyContains_and_return404WhenBodyDoesNotMatch()
        throws IOException, InterruptedException {
        mokksy.post(spec -> {
            spec.path("/body-contains");
            spec.bodyContains("expected-token");
        }).respondsWith(builder -> builder.body("matched"));

        var matched = post("/body-contains", "{\"token\": \"expected-token\"}");
        var notMatched = post("/body-contains", "{\"token\": \"other\"}");

        assertThat(matched.statusCode()).isEqualTo(200);
        assertThat(notMatched.statusCode()).isEqualTo(404);
    }

    @Test
    void post_shouldMatchByBodyPredicate() throws IOException, InterruptedException {
        mokksy.post(spec -> {
            spec.path("/body-predicate");
            spec.bodyMatchesPredicate(body -> body.contains("match-me"));
        }).respondsWith(builder -> builder.body("predicate-matched"));

        var matched = post("/body-predicate", "{\"value\": \"match-me\"}");
        var notMatched = post("/body-predicate", "{\"value\": \"no\"}");

        assertThat(matched.statusCode()).isEqualTo(200);
        assertThat(notMatched.statusCode()).isEqualTo(404);
    }

    // endregion

    // region StubConfiguration

    @Test
    void removeAfterMatch_shouldReturn404OnSecondRequest() throws IOException, InterruptedException {
        mokksy.get(
            new StubConfiguration("once-only", true), spec ->
                spec.path("/once")
        ).respondsWith(builder ->
            builder.body("First!")
        );

        var first = get("/once");
        var second = get("/once");

        assertThat(first.statusCode()).isEqualTo(200);
        assertThat(second.statusCode()).isEqualTo(404);
    }

    // endregion

    // region Stub priority

    @Test
    void stub_higherPriorityShouldWin() throws IOException, InterruptedException {
        mokksy.get(spec -> {
            spec.path("/priority");
            spec.priority(10);
        }).respondsWith(builder -> builder.body("low-priority"));

        mokksy.get(spec -> {
            spec.path("/priority");
            spec.priority(1);
        }).respondsWith(builder -> builder.body("high-priority"));

        var response = get("/priority");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("high-priority");
    }

    // endregion

    // region Verification

    @Test
    void verifyNoUnmatchedStubs_shouldThrowWhenStubNeverCalled() {
        mokksy.get(spec -> spec.path("/never-called"))
            .respondsWith(builder -> builder.body("unreachable"));

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

    @Test
    void resetMatchState_shouldMakeMatchedStubsUnmatchedAgain()
        throws IOException, InterruptedException {
        try (Mokksy fresh = Mokksy.create().start()) {
            fresh.get(spec -> spec.path("/reset-test"))
                .respondsWith(builder -> builder.body("ok"));

            httpClient.send(
                HttpRequest.newBuilder()
                    .uri(URI.create(fresh.baseUrl() + "/reset-test"))
                    .GET()
                    .build(),
                HttpResponse.BodyHandlers.ofString()
            );

            fresh.verifyNoUnmatchedStubs();
            fresh.verifyNoUnexpectedRequests();

            fresh.resetMatchState();

            assertThatThrownBy(fresh::verifyNoUnmatchedStubs)
                .isInstanceOf(AssertionError.class);
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

    private HttpResponse<String> send(String method, String path, @Nullable String body)
        throws IOException, InterruptedException {
        var publisher = body != null
            ? HttpRequest.BodyPublishers.ofString(body)
            : HttpRequest.BodyPublishers.noBody();
        return httpClient.send(
            HttpRequest.newBuilder()
                .uri(URI.create(mokksy.baseUrl() + path))
                .method(method, publisher)
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
    }

    // endregion
}
