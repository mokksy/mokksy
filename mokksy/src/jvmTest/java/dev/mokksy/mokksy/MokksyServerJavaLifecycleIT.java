package dev.mokksy.mokksy;

import dev.mokksy.Mokksy;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class MokksyServerJavaLifecycleIT {
    @Test
    public void startAndShutdownWorkAsBlockingLifecycleMethods() {
        var mokksy = Mokksy.create().start();

        try {
            assertThat(mokksy.port()).isGreaterThan(0);
            assertThat(mokksy.baseUrl()).contains("http://");
            assertThat(mokksy.baseUrl()).contains(String.valueOf(mokksy.port()));
        } finally {
            mokksy.shutdown();
        }
    }

    @Test
    public void closeShutsDownTheServer() {
        try (var mokksy = Mokksy.create().start()) {
            assertThat(mokksy.port()).isGreaterThan(0);
        }
    }

    @Test
    public void shutdownWithCustomTimingsCompletesWithoutError() {
        var mokksy = Mokksy.create().start();
        try {
            mokksy.shutdown(100L, 200L);
        } finally {
            mokksy.shutdown();
        }
    }

    @Test
    public void serverRespondsAfterStart() throws IOException, InterruptedException {
        var mokksy = Mokksy.create().start();

        var httpClient = HttpClient.newHttpClient();
        try {
            mokksy.get(spec -> spec.path("/java-lifecycle-test"))
                .respondsWith(rb -> rb.body("alive"));

            var response = httpClient.send(
                HttpRequest.newBuilder()
                    .uri(URI.create(mokksy.baseUrl() + "/java-lifecycle-test"))
                    .GET()
                    .build(),
                HttpResponse.BodyHandlers.ofString()
            );

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).isEqualTo("alive");
        } finally {
            mokksy.shutdown();
        }
    }

    @Test
    public void getStubReturnsNamedStubHandle() {
        var mokksy = Mokksy.create().start();

        try {
            var expected = mokksy.get(new StubConfiguration("java-get-stub"), "/java-get-stub")
                .respondsWith("alive");

            assertThat(mokksy.getStub("java-get-stub").getName()).isEqualTo(expected.getName());
        } finally {
            mokksy.shutdown();
        }
    }

    @Test
    public void getStubThrowsWhenStubNotFound() {
        var mokksy = Mokksy.create().start();

        try {
            assertThatThrownBy(() -> mokksy.getStub("missing-stub"))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("No stub registered with name 'missing-stub'");
        } finally {
            mokksy.shutdown();
        }
    }
}
