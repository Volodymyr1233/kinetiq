package com.kinetiq.plan.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

/** The settings of the client against a local server, so that nothing here reaches the real service. */
class PlanChatModelTest {

    private static final String COMPLETION =
            """
            {"id":"1","object":"chat.completion","created":1,"model":"stub/model","choices":[
              {"index":0,"finish_reason":"%s","logprobs":null,
               "message":{"role":"assistant","content":"{}","refusal":null}}]}
            """;

    private final AtomicInteger requests = new AtomicInteger();
    private final AtomicReference<String> body = new AtomicReference<>();
    private HttpServer server;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private ChatModel model(Duration timeout) {
        return PlanConfig.chatModel(
                "http://127.0.0.1:%d/api/v1".formatted(server.getAddress().getPort()),
                "test-key",
                new PlanConfig.ModelSettings("stub/model", timeout, 16000, 0.2, "low"));
    }

    private void answer(int status, String json, long delayMillis) {
        server.createContext("/api/v1/chat/completions", exchange -> {
            requests.incrementAndGet();
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            pause(delayMillis);
            respond(exchange, status, json);
        });
    }

    private static void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private static void respond(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    @Test
    void shouldSendEverySetting_whenPromptHasNoOptions() {
        answer(200, COMPLETION.formatted("stop"), 0);

        model(Duration.ofSeconds(5)).call(new Prompt("plan"));

        assertThat(body.get())
                .contains("\"require_parameters\":true")
                .contains("\"reasoning\":{")
                .contains("\"effort\":\"low\"")
                .contains("\"exclude\":true")
                .contains("\"json_schema\"")
                .contains("\"restSeconds\"")
                .contains("\"max_tokens\":16000")
                .contains("\"model\":\"stub/model\"")
                .contains("\"temperature\":0.2");
    }

    @Test
    void shouldReportCutOffAnswer_whenServiceStopsAtTheLimit() {
        answer(200, COMPLETION.formatted("length"), 0);

        ChatResponse response = model(Duration.ofSeconds(5)).call(new Prompt("plan"));

        assertThat(response.getResult().getMetadata().getFinishReason()).isEqualToIgnoringCase("length");
    }

    @Test
    void shouldFailAtTheTimeLimit_whenServiceIsSlow() {
        answer(200, COMPLETION.formatted("stop"), 10_000);
        long started = System.nanoTime();

        assertThatThrownBy(() -> model(Duration.ofSeconds(1)).call(new Prompt("plan")))
                .isInstanceOf(RuntimeException.class);

        assertThat(Duration.ofNanos(System.nanoTime() - started)).isLessThan(Duration.ofSeconds(5));
    }

    @Test
    void shouldMakeExactlyOneRequest_whenServiceAnswersTooManyRequests() {
        answer(429, "{\"error\":{\"message\":\"slow down\"}}", 0);

        assertThatThrownBy(() -> model(Duration.ofSeconds(5)).call(new Prompt("plan")))
                .isInstanceOf(RuntimeException.class);

        assertThat(requests).hasValue(1);
    }
}
