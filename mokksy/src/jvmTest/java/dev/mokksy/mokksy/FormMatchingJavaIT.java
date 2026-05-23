package dev.mokksy.mokksy;

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
public class FormMatchingJavaIT {

    private final Mokksy mokksy = Mokksy.create().start();
    private HttpClient httpClient;

    @BeforeAll
    void setUp() {
        httpClient = HttpClient.newHttpClient();
    }

    @AfterAll
    void tearDown() {
        mokksy.shutdown();
    }

    @Test
    void shouldMatchUrlEncodedForm() throws IOException, InterruptedException {
        var path = "/java-form-url-" + UUID.randomUUID();

        mokksy.post(spec -> spec
            .path(path)
            .body(body -> body.form(form -> form
                .field("username", "JetBrains")
                .fieldMatches("email", value -> value != null && value.endsWith("@jetbrains.com"))
            ))
        ).respondsWith(rb -> rb.body("OK"));

        var response = httpClient.send(
            HttpRequest.newBuilder()
                .uri(URI.create(mokksy.baseUrl() + path))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(
                    "username=JetBrains&email=example%40jetbrains.com"
                ))
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("OK");
    }

    @Test
    void shouldMatchMultipartFormFile() throws IOException, InterruptedException {
        var path = "/java-form-file-" + UUID.randomUUID();

        mokksy.post(spec -> spec
            .path(path)
            .body(body -> body.form(form -> form
                .field("description", "Ktor logo")
                .file("image", file -> file
                    .filename("ktor_logo.png")
                    .contentType("image/png")
                    .text("dummy-content")
                )
            ))
        ).respondsWith(rb -> rb.body("OK"));

        var boundary = "----TestBoundary" + System.nanoTime();
        var body = "--" + boundary + "\r\n"
            + "Content-Disposition: form-data; name=\"description\"\r\n"
            + "\r\n"
            + "Ktor logo\r\n"
            + "--" + boundary + "\r\n"
            + "Content-Disposition: form-data; name=\"image\"; filename=\"ktor_logo.png\"\r\n"
            + "Content-Type: image/png\r\n"
            + "\r\n"
            + "dummy-content\r\n"
            + "--" + boundary + "--\r\n";

        var response = httpClient.send(
            HttpRequest.newBuilder()
                .uri(URI.create(mokksy.baseUrl() + path))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("OK");
    }

    @Test
    void shouldMatchRawBytes() throws IOException, InterruptedException {
        var path = "/java-raw-bytes-" + UUID.randomUUID();
        var payload = "binary-data".getBytes();

        mokksy.post(spec -> spec
            .path(path)
            .body(body -> body
                .bytes(payload)
                .contentType("application/octet-stream")
            )
        ).respondsWithStatus(202);

        var response = httpClient.send(
            HttpRequest.newBuilder()
                .uri(URI.create(mokksy.baseUrl() + path))
                .header("Content-Type", "application/octet-stream")
                .POST(HttpRequest.BodyPublishers.ofByteArray(payload))
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );

        assertThat(response.statusCode()).isEqualTo(202);
    }
}
