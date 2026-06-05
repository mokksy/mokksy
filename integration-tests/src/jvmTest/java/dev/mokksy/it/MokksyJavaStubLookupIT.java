package dev.mokksy.it;

import dev.mokksy.Mokksy;
import dev.mokksy.mokksy.ExperimentalMokksyApi;
import dev.mokksy.mokksy.StubConfiguration;
import dev.mokksy.mokksy.StubPredicate;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@ExperimentalMokksyApi
class MokksyJavaStubLookupIT {

    @Test
    void findStubById_shouldReturnHandleForRegisteredStub() throws Exception {
        try (Mokksy mokksy = Mokksy.create().start()) {
            HttpClient httpClient = HttpClient.newHttpClient();

            var handle = mokksy.get(spec -> spec.path("/find-by-id"))
                .respondsWith(builder -> builder.body("ok"));

            assertThat(handle).isNotNull();
            assertThat(handle.getId()).isNotEmpty();

            var found = mokksy.findStubById(handle.getId());
            assertThat(found).isNotNull();
            assertThat(found.getId()).isEqualTo(handle.getId());
            assertThat(found.matchCount()).isEqualTo(0);
        }
    }

    @Test
    void findStubById_shouldReturnNullForUnknownId() {
        try (Mokksy mokksy = Mokksy.create().start()) {
            var found = mokksy.findStubById("nonexistent-id");
            assertThat(found).isNull();
        }
    }

    @Test
    void findStubByName_shouldReturnHandleForNamedStub() throws Exception {
        try (Mokksy mokksy = Mokksy.create().start()) {
            HttpClient httpClient = HttpClient.newHttpClient();

            mokksy.get(new StubConfiguration("lookup-by-name"), spec ->
                    spec.path("/lookup-by-name")
                ).respondsWith(builder -> builder.body("ok"));

            var found = mokksy.findStubByName("lookup-by-name");
            assertThat(found).isNotNull();
            assertThat(found.getName()).isEqualTo("lookup-by-name");
            assertThat(found.matchCount()).isEqualTo(0);
            assertThat(found.getId()).isNotNull();

            httpClient.send(
                HttpRequest.newBuilder()
                    .uri(URI.create(mokksy.baseUrl() + "/lookup-by-name"))
                    .GET().build(),
                HttpResponse.BodyHandlers.ofString()
            );

            assertThat(found.matchCount()).isEqualTo(1);
        }
    }

    @Test
    void findStubByName_shouldReturnNullForUnknownName() {
        try (Mokksy mokksy = Mokksy.create().start()) {
            var found = mokksy.findStubByName("nonexistent");
            assertThat(found).isNull();
        }
    }

    @Test
    void findStubByBothIdAndName_shouldReturnSameStub() throws Exception {
        try (Mokksy mokksy = Mokksy.create().start()) {
            HttpClient httpClient = HttpClient.newHttpClient();

            var handle = mokksy.get(new StubConfiguration("dual-lookup"), spec ->
                    spec.path("/dual-lookup")
                ).respondsWith(builder -> builder.body("ok"));

            var byId = mokksy.findStubById(handle.getId());
            var byName = mokksy.findStubByName("dual-lookup");

            assertThat(byId).isNotNull();
            assertThat(byName).isNotNull();
            assertThat(byId.getId()).isEqualTo(byName.getId());
            assertThat(byId.getName()).isEqualTo(byName.getName());

            httpClient.send(
                HttpRequest.newBuilder()
                    .uri(URI.create(mokksy.baseUrl() + "/dual-lookup"))
                    .GET().build(),
                HttpResponse.BodyHandlers.ofString()
            );

            assertThat(byId.matchCount()).isEqualTo(1);
            assertThat(byName.matchCount()).isEqualTo(1);
        }
    }

    @Test
    void findStub_withPredicate_shouldReturnFirstMatch() {
        try (Mokksy mokksy = Mokksy.create().start()) {
            mokksy.get(spec -> spec.path("/predicate-a"))
                .respondsWith(builder -> builder.body("a"));
            mokksy.get(spec -> spec.path("/predicate-b"))
                .respondsWith(builder -> builder.body("b"));

            StubPredicate anyStub = stub -> !stub.getId().isEmpty();
            var found = mokksy.findStub(anyStub);
            assertThat(found).isNotNull();
            assertThat(found.getId()).isNotEmpty();
        }
    }

    @Test
    void findStub_withPredicate_shouldReturnNullWhenNoMatch() {
        try (Mokksy mokksy = Mokksy.create().start()) {
            mokksy.get(spec -> spec.path("/no-predicate-match"))
                .respondsWith(builder -> builder.body("ok"));

            var found = mokksy.findStub(stub -> "missing".equals(stub.getName()));
            assertThat(found).isNull();
        }
    }

    @Test
    void findStubs_withPredicate_shouldReturnAllMatches() {
        try (Mokksy mokksy = Mokksy.create().start()) {
            mokksy.get(new StubConfiguration("java-match-a"), spec -> spec.path("/java-match-a"))
                .respondsWith(builder -> builder.body("a"));
            mokksy.get(new StubConfiguration("java-match-b"), spec -> spec.path("/java-match-b"))
                .respondsWith(builder -> builder.body("b"));
            mokksy.get(new StubConfiguration("java-other"), spec -> spec.path("/java-other"))
                .respondsWith(builder -> builder.body("other"));

            var matches = mokksy.findStubs(stub -> stub.getName() != null && stub.getName().startsWith("java-match-"));
            assertThat(matches).hasSize(2);
        }
    }

    @Test
    void findStubs_withPredicate_shouldReturnEmptyListWhenNoMatch() {
        try (Mokksy mokksy = Mokksy.create().start()) {
            mokksy.get(spec -> spec.path("/java-nothing"))
                .respondsWith(builder -> builder.body("ok"));

            var matches = mokksy.findStubs(stub -> "missing".equals(stub.getName()));
            assertThat(matches).isEmpty();
        }
    }
}
