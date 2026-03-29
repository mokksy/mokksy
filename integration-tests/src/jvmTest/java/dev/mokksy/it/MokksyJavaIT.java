package dev.mokksy.it;

import dev.mokksy.Mokksy;
import dev.mokksy.mokksy.StubConfiguration;
import org.jspecify.annotations.Nullable;
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

    @Test
    void put_shouldReturn200WithBody() throws IOException, InterruptedException {
        mokksy.put(spec -> spec.path("/put-test"))
            .respondsWith(builder -> builder.body("put-ok"));

        var response = send("PUT", "/put-test", null);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("put-ok");
    }

    @Test
    void delete_shouldReturn200WithBody() throws IOException, InterruptedException {
        mokksy.delete(spec -> spec.path("/delete-test"))
            .respondsWith(builder -> builder.body("deleted"));

        var response = send("DELETE", "/delete-test", null);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("deleted");
    }

    @Test
    void patch_shouldReturn200WithBody() throws IOException, InterruptedException {
        mokksy.patch(spec -> spec.path("/patch-test"))
            .respondsWith(builder -> builder.body("patched"));

        var response = send("PATCH", "/patch-test", null);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("patched");
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

    // region respondsWithStatus

    @Test
    void respondsWithStatus_shouldReturnStatusCodeAndEmptyBody() throws IOException, InterruptedException {
        mokksy.get(spec -> spec.path("/status-only"))
            .respondsWithStatus(204);

        var response = get("/status-only");

        assertThat(response.statusCode()).isEqualTo(204);
        assertThat(response.body()).isEmpty();
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
            spec.priority(1);
        }).respondsWith(builder -> builder.body("low-priority"));

        mokksy.get(spec -> {
            spec.path("/priority");
            spec.priority(10);
        }).respondsWith(builder -> builder.body("high-priority"));

        var response = get("/priority");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("high-priority");
    }

    @Test
    void stub_defaultPriorityVsPositive_positiveShouldWin() throws IOException, InterruptedException {
        mokksy.get(spec -> spec.path("/priority-default-vs-positive"))
            .respondsWith(builder -> builder.body("default-priority"));

        mokksy.get(spec -> {
            spec.path("/priority-default-vs-positive");
            spec.priority(1);
        }).respondsWith(builder -> builder.body("positive-priority"));

        var response = get("/priority-default-vs-positive");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("positive-priority");
    }

    @Test
    void stub_defaultPriorityVsNegative_defaultShouldWin() throws IOException, InterruptedException {
        mokksy.get(spec -> {
            spec.path("/priority-default-vs-negative");
            spec.priority(-1);
        }).respondsWith(builder -> builder.body("negative-priority"));

        mokksy.get(spec -> spec.path("/priority-default-vs-negative"))
            .respondsWith(builder -> builder.body("default-priority"));

        var response = get("/priority-default-vs-negative");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("default-priority");
    }

    // endregion

    // region Catch-all fallback pattern

    @Test
    void catchAll_fallbackPatternWithPriority() throws IOException, InterruptedException {
        // Catch-all stub: matches any POST to /v1/chat/completions, returns 400
        mokksy.post(spec -> {
            spec.path("/v1/chat/completions");
            spec.bodyMatchesPredicate(body -> true);
            spec.priority(-1);
        }).respondsWith(builder -> builder
            .body("{\"error\":\"unsupported request\"}")
            .status(400));

        // Specific stub: matches only when body contains "gpt-4", returns 200
        mokksy.post(spec -> {
            spec.path("/v1/chat/completions");
            spec.bodyContains("gpt-4");
            spec.priority(1);
        }).respondsWith(builder -> builder
            .body("{\"model\":\"gpt-4\"}")
            .status(200));

        // Specific request → both stubs match with the same score, so priority decides
        var specificResponse = post("/v1/chat/completions", "{\"model\":\"gpt-4\"}");
        assertThat(specificResponse.statusCode()).isEqualTo(200);
        assertThat(specificResponse.body()).isEqualTo("{\"model\":\"gpt-4\"}");

        // Unmatched request → catch-all fallback kicks in
        var fallbackResponse = post("/v1/chat/completions", "{\"model\":\"other\"}");
        assertThat(fallbackResponse.statusCode()).isEqualTo(400);
        assertThat(fallbackResponse.body()).isEqualTo("{\"error\":\"unsupported request\"}");
    }

    // endregion

    // region Path shortcut API

    @Test
    void pathShortcut_get_shouldReturn200WithBody() throws IOException, InterruptedException {
        mokksy.get("/shortcut-get")
            .respondsWith(builder -> builder.body("shortcut-ok"));

        var response = get("/shortcut-get");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("shortcut-ok");
    }

    @Test
    void pathShortcut_post_shouldReturn201() throws IOException, InterruptedException {
        mokksy.post("/shortcut-post")
            .respondsWith(builder -> builder.body("created").status(201));

        var response = post("/shortcut-post", "{}");

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.body()).isEqualTo("created");
    }

    @Test
    void pathShortcut_put_shouldReturn200() throws IOException, InterruptedException {
        mokksy.put("/shortcut-put")
            .respondsWith(builder -> builder.body("put-ok"));

        var response = send("PUT", "/shortcut-put", null);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("put-ok");
    }

    @Test
    void pathShortcut_delete_shouldReturn200() throws IOException, InterruptedException {
        mokksy.delete("/shortcut-delete")
            .respondsWith(builder -> builder.body("deleted"));

        var response = send("DELETE", "/shortcut-delete", null);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("deleted");
    }

    @Test
    void pathShortcut_patch_shouldReturn200() throws IOException, InterruptedException {
        mokksy.patch("/shortcut-patch")
            .respondsWith(builder -> builder.body("patched"));

        var response = send("PATCH", "/shortcut-patch", null);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("patched");
    }

    @Test
    void pathShortcut_head_shouldReturn200() throws IOException, InterruptedException {
        mokksy.head("/shortcut-head")
            .respondsWith(builder -> builder.body("ignored"));

        var response = send("HEAD", "/shortcut-head", null);

        assertThat(response.statusCode()).isEqualTo(200);
    }

    @Test
    void pathShortcut_options_shouldReturn200() throws IOException, InterruptedException {
        mokksy.options("/shortcut-options")
            .respondsWith(builder -> builder.body("OK"));

        var response = send("OPTIONS", "/shortcut-options", null);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("OK");
    }

    // endregion

    // region respondsWith shortcut API

    @Test
    void respondsWithBody_shouldReturn200WithStringBody() throws IOException, InterruptedException {
        mokksy.get("/responds-body")
            .respondsWith("simple-body");

        var response = get("/responds-body");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("simple-body");
    }

    @Test
    void respondsWithBodyAndStatus_shouldReturnCustomStatus() throws IOException, InterruptedException {
        mokksy.post("/responds-body-status")
            .respondsWith("{\"id\":42}", 201);

        var response = post("/responds-body-status", "{}");

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.body()).isEqualTo("{\"id\":42}");
    }

    // endregion

    // region StubConfiguration.once()

    @Test
    void stubConfigOnce_shouldReturn404OnSecondRequest() throws IOException, InterruptedException {
        mokksy.get(StubConfiguration.once("once-factory"), "/once-factory")
            .respondsWith("First!");

        var first = get("/once-factory");
        var second = get("/once-factory");

        assertThat(first.statusCode()).isEqualTo(200);
        assertThat(first.body()).isEqualTo("First!");
        assertThat(second.statusCode()).isEqualTo(404);
    }

    @Test
    void stubConfigOnce_noName_shouldReturn404OnSecondRequest() throws IOException, InterruptedException {
        mokksy.get(StubConfiguration.once(), "/once-no-name")
            .respondsWith("One-shot");

        var first = get("/once-no-name");
        var second = get("/once-no-name");

        assertThat(first.statusCode()).isEqualTo(200);
        assertThat(second.statusCode()).isEqualTo(404);
    }

    // endregion

    // region Combined shortcut API

    @Test
    void combinedShortcuts_pathAndBodyAndStatus() throws IOException, InterruptedException {
        mokksy.get(StubConfiguration.once("combined"), "/combined-shortcut")
            .respondsWith("one-time", 200);

        var first = get("/combined-shortcut");
        var second = get("/combined-shortcut");

        assertThat(first.statusCode()).isEqualTo(200);
        assertThat(first.body()).isEqualTo("one-time");
        assertThat(second.statusCode()).isEqualTo(404);
    }

    // endregion

    // region Arbitrary method

    @Test
    void method_withConsumer_shouldReturn200() throws IOException, InterruptedException {
        mokksy.method("PATCH", spec -> spec.path("/method-consumer"))
            .respondsWith(builder -> builder.body("method-ok"));

        var response = send("PATCH", "/method-consumer", null);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("method-ok");
    }

    @Test
    void method_withPathShortcut_shouldReturn200() throws IOException, InterruptedException {
        mokksy.method("PATCH", "/method-shortcut")
            .respondsWith("method-shortcut-ok");

        var response = send("PATCH", "/method-shortcut", null);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("method-shortcut-ok");
    }

    // endregion

    // region delay

    @Test
    void get_withDelayMillis_shouldDelayResponse() throws Exception {
        mokksy.get(spec -> spec.path("/delayed"))
            .respondsWith(builder -> builder
                .body("ok")
                .delayMillis(200L));

        var response = TimingAssertions.takesAtLeast(200L,
            () -> get("/delayed"));

        assertThat(response.statusCode()).isEqualTo(200);
    }

    // endregion

    // region Verification

    @Test
    void findAllUnmatchedStubs_shouldReturnUnmatchedStub() throws IOException, InterruptedException {
        try (var fresh = Mokksy.create().start()) {
            fresh.get(spec -> spec.path("/unmatched-stub"))
                .respondsWith(builder -> builder.body("never-called"));

            assertThat(fresh.findAllUnmatchedStubs()).hasSize(1);

            httpClient.send(
                HttpRequest.newBuilder()
                    .uri(URI.create(fresh.baseUrl() + "/unmatched-stub"))
                    .GET().build(),
                HttpResponse.BodyHandlers.ofString()
            );

            assertThat(fresh.findAllUnmatchedStubs()).isEmpty();
        }
    }

    @Test
    void findAllUnexpectedRequests_shouldReturnUnexpectedRequest() throws IOException, InterruptedException {
        try (var fresh = Mokksy.create().start()) {
            assertThat(fresh.findAllUnexpectedRequests()).isEmpty();

            httpClient.send(
                HttpRequest.newBuilder()
                    .uri(URI.create(fresh.baseUrl() + "/no-stub-here"))
                    .GET().build(),
                HttpResponse.BodyHandlers.ofString()
            );

            assertThat(fresh.findAllUnexpectedRequests()).hasSize(1);
        }
    }

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
