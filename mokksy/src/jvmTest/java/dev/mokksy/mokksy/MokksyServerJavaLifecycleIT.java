package dev.mokksy.mokksy;

import dev.mokksy.Mokksy;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

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
        var mokksy = Mokksy.create().start();
        try {
            assertThat(mokksy.port()).isGreaterThan(0);
        } finally {
            mokksy.close();
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
}
