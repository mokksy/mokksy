package dev.mokksy.mokksy;

import dev.mokksy.Mokksy;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

public class MokksyServerJavaListenerIT {

    @Test
    public void addListenerFiresWhenStubMatches() throws Exception {
        var mokksy = Mokksy.create().start();
        var httpClient = HttpClient.newHttpClient();

        try {
            var callCount = new AtomicInteger(0);

            mokksy.addListener((request, response) -> {
                callCount.incrementAndGet();
                assertThat(request.getUri()).isEqualTo("/java-listener-test");
                assertThat(response.getHttpStatus().getValue()).isEqualTo(200);
            });

            mokksy.get("/java-listener-test")
                .respondsWith("ok");

            var serverResponse = httpClient.send(
                HttpRequest.newBuilder()
                    .uri(URI.create(mokksy.baseUrl() + "/java-listener-test"))
                    .GET()
                    .build(),
                HttpResponse.BodyHandlers.ofString()
            );

            assertThat(serverResponse.statusCode()).isEqualTo(200);
            assertThat(serverResponse.body()).isEqualTo("ok");
            assertThat(callCount.get()).isEqualTo(1);
        } finally {
            mokksy.shutdown();
        }
    }

    @Test
    public void addListenerIsNotInvokedForUnmatchedRequests() throws Exception {
        var mokksy = Mokksy.create().start();
        var httpClient = HttpClient.newHttpClient();

        try {
            var callCount = new AtomicInteger(0);

            mokksy.addListener((request, response) -> callCount.incrementAndGet());

            var serverResponse = httpClient.send(
                HttpRequest.newBuilder()
                    .uri(URI.create(mokksy.baseUrl() + "/unmatched-java-listener"))
                    .GET()
                    .build(),
                HttpResponse.BodyHandlers.ofString()
            );

            assertThat(serverResponse.statusCode()).isEqualTo(404);
            assertThat(callCount.get()).isEqualTo(0);
        } finally {
            mokksy.shutdown();
        }
    }

    @Test
    public void addListenerIsReplaceable() throws Exception {
        var mokksy = Mokksy.create().start();
        var httpClient = HttpClient.newHttpClient();

        try {
            var firstCallCount = new AtomicInteger(0);
            var secondCallCount = new AtomicInteger(0);

            mokksy.addListener((request, response) -> firstCallCount.incrementAndGet());
            mokksy.addListener((request, response) -> secondCallCount.incrementAndGet());

            mokksy.get("/java-replace-listener")
                .respondsWith("ok");

            httpClient.send(
                HttpRequest.newBuilder()
                    .uri(URI.create(mokksy.baseUrl() + "/java-replace-listener"))
                    .GET()
                    .build(),
                HttpResponse.BodyHandlers.ofString()
            );

            assertThat(firstCallCount.get()).isEqualTo(0);
            assertThat(secondCallCount.get()).isEqualTo(1);
        } finally {
            mokksy.shutdown();
        }
    }
}
