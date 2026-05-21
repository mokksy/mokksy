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
public class FormDataMatchingJavaIT {

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
    void shouldMatchFormDataFields() throws IOException, InterruptedException {
        var path = "/java-fd-" + UUID.randomUUID();

        mokksy.post(spec -> spec
            .path(path)
            .body(body -> body.formData(
                    fd -> fd.field("locale", "test")
                )
            )
        ).respondsWith(rb -> rb.body("OK"));

        var boundary = "----TestBoundary" + System.nanoTime();
        var body = "--" + boundary + "\r\n"
            + "Content-Disposition: form-data; name=\"locale\"\r\n"
            + "\r\n"
            + "test\r\n"
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
    void shouldFailWhenFormDataFieldValueDoesNotMatch() throws IOException, InterruptedException {
        var path = "/java-fd-wrong-" + UUID.randomUUID();

        mokksy.post(spec -> spec
            .path(path)
            .body(body -> body.formData(
                fd -> fd.field("locale", "expected")
            ))
        ).respondsWith(rb -> rb.body("OK"));

        var boundary = "----TestBoundary" + System.nanoTime();
        var body = "--" + boundary + "\r\n"
            + "Content-Disposition: form-data; name=\"locale\"\r\n"
            + "\r\n"
            + "wrong\r\n"
            + "--" + boundary + "--\r\n";

        var response = httpClient.send(
            HttpRequest.newBuilder()
                .uri(URI.create(mokksy.baseUrl() + path))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );

        assertThat(response.statusCode()).isEqualTo(404);
    }

    @Test
    void shouldMatchMultipleFormDataFields() throws IOException, InterruptedException {
        var path = "/java-fd-multi-" + UUID.randomUUID();

        mokksy.post(spec -> spec
            .path(path)
            .body(body -> body.formData(fd -> {
                fd.field("locale", "test");
                fd.field("code", "123");
            }))
        ).respondsWith(rb -> rb.body("OK"));

        var boundary = "----TestBoundary" + System.nanoTime();
        var body = "--" + boundary + "\r\n"
            + "Content-Disposition: form-data; name=\"locale\"\r\n"
            + "\r\n"
            + "test\r\n"
            + "--" + boundary + "\r\n"
            + "Content-Disposition: form-data; name=\"code\"\r\n"
            + "\r\n"
            + "123\r\n"
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
    }

    @Test
    void shouldMatchFilePartByFilename() throws IOException, InterruptedException {
        var path = "/java-fd-file-" + UUID.randomUUID();

        mokksy.post(spec -> spec
            .path(path)
            .body(body -> body.formData(fd -> fd
                .file("avatar", f -> f.filename("photo.jpg")))
            )
        ).respondsWith(rb -> rb.body("OK"));

        var boundary = "----TestBoundary" + System.nanoTime();
        var body = "--" + boundary + "\r\n"
            + "Content-Disposition: form-data; name=\"avatar\"; filename=\"photo.jpg\"\r\n"
            + "Content-Type: image/jpeg\r\n"
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
    }

    @Test
    void shouldFailWhenFilePartFilenameDoesNotMatch() throws IOException, InterruptedException {
        var path = "/java-fd-file-wrong-" + UUID.randomUUID();

        mokksy.post(spec -> spec
            .path(path)
            .body(body -> body.formData(fd -> fd
                .file("avatar", f -> f.filename("expected.jpg")))
            )
        ).respondsWith(rb -> rb.body("OK"));

        var boundary = "----TestBoundary" + System.nanoTime();
        var body = "--" + boundary + "\r\n"
            + "Content-Disposition: form-data; name=\"avatar\"; filename=\"actual.jpg\"\r\n"
            + "Content-Type: image/jpeg\r\n"
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

        assertThat(response.statusCode()).isEqualTo(404);
    }

    @Test
    void shouldNotMatchWhenRequestIsNotMultipart() throws IOException, InterruptedException {
        var path = "/java-fd-not-mp-" + UUID.randomUUID();

        mokksy.post(spec -> spec
            .path(path)
            .body(body -> body.formData(fd -> fd.field("locale", "test")))
        ).respondsWith(rb -> rb.body("OK"));

        var response = httpClient.send(
            HttpRequest.newBuilder()
                .uri(URI.create(mokksy.baseUrl() + path))
                .POST(HttpRequest.BodyPublishers.ofString("not multipart"))
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );

        assertThat(response.statusCode()).isEqualTo(404);
    }

    @Test
    void shouldMatchFormDataFieldWithPredicate() throws IOException, InterruptedException {
        var path = "/java-fd-predicate-" + UUID.randomUUID();

        mokksy.post(spec -> spec
            .path(path)
            .body(body -> body.formData(
                    fd -> fd.fieldMatches("locale", v -> v != null && v.startsWith("te"))
                )
            )
        ).respondsWith(rb -> rb.body("OK"));

        var boundary = "----TestBoundary" + System.nanoTime();
        var body = "--" + boundary + "\r\n"
            + "Content-Disposition: form-data; name=\"locale\"\r\n"
            + "\r\n"
            + "test\r\n"
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
    void shouldFailWhenFormDataFieldPredicateDoesNotMatch() throws IOException, InterruptedException {
        var path = "/java-fd-predicate-fail-" + UUID.randomUUID();

        mokksy.post(spec -> spec
            .path(path)
            .body(body -> body.formData(
                fd -> fd.fieldMatches("locale", v -> v != null && v.startsWith("no"))
            ))
        ).respondsWith(rb -> rb.body("OK"));

        var boundary = "----TestBoundary" + System.nanoTime();
        var body = "--" + boundary + "\r\n"
            + "Content-Disposition: form-data; name=\"locale\"\r\n"
            + "\r\n"
            + "test\r\n"
            + "--" + boundary + "--\r\n";

        var response = httpClient.send(
            HttpRequest.newBuilder()
                .uri(URI.create(mokksy.baseUrl() + path))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );

        assertThat(response.statusCode()).isEqualTo(404);
    }
}
