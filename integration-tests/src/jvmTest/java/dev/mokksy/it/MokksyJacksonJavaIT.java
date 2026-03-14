package dev.mokksy.it;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.mokksy.Mokksy;
import dev.mokksy.MokksyJackson;
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

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MokksyJacksonJavaIT {

    // region Fixtures

    record CreateItemRequest(
        String name,
        int quantity) {
    }

    // endregion

    private final Mokksy mokksy = MokksyJackson.create();
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

    // region Typed body matching

    @Test
    void post_shouldDeserializeJacksonBody_andMatchByFieldValue()
        throws IOException, InterruptedException {
        mokksy.post(
            CreateItemRequest.class,
            spec -> spec
                .path("/items")
                .bodyMatchesPredicate(request -> "widget".equals(request.name()))
        ).respondsWith(builder -> builder.body("{\"id\":\"1\"}").status(201));

        var matched = post("/items", "{\"name\":\"widget\",\"quantity\":3}");
        var notMatched = post("/items", "{\"name\":\"gadget\",\"quantity\":1}");

        assertThat(matched.statusCode()).isEqualTo(201);
        assertThat(matched.body()).isEqualTo("{\"id\":\"1\"}");
        assertThat(notMatched.statusCode()).isEqualTo(404);
    }

    @Test
    void post_shouldDeserializeJacksonBody_andMatchByMultipleFields()
        throws IOException, InterruptedException {
        mokksy.post(
            CreateItemRequest.class,
            spec -> spec
                .path("/items/validated")
                .bodyMatchesPredicate(
                    "name=widget and quantity>=5",
                    request -> "widget".equals(request.name()) && request.quantity() >= 5
                )
        ).respondsWith(builder -> builder.body("accepted").status(201));

        var accepted = post("/items/validated", "{\"name\":\"widget\",\"quantity\":10}");
        var tooFew = post("/items/validated", "{\"name\":\"widget\",\"quantity\":2}");

        assertThat(accepted.statusCode()).isEqualTo(201);
        assertThat(tooFew.statusCode()).isEqualTo(404);
    }

    // endregion

    // region Custom ObjectMapper configuration

    @Test
    void create_withCustomObjectMapperConfig_shouldDeserializeBody()
        throws IOException, InterruptedException {
        try (var server = MokksyJackson.create(ObjectMapper::findAndRegisterModules).start()) {
            server.post(
                CreateItemRequest.class,
                spec -> spec
                    .path("/items/custom")
                    .bodyMatchesPredicate(request -> request.quantity() > 0)
            ).respondsWith(builder -> builder.body("ok"));

            var response = httpClient.send(
                HttpRequest.newBuilder()
                    .uri(URI.create(server.baseUrl() + "/items/custom"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"name\":\"test\",\"quantity\":1}"))
                    .build(),
                HttpResponse.BodyHandlers.ofString()
            );

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).isEqualTo("ok");
        }
    }

    // endregion

    // region helpers

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
