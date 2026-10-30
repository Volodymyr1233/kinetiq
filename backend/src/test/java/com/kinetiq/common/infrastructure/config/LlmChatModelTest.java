package com.kinetiq.common.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/** Runs the language model against a local server that answers like an OpenAI-compatible service. */
class LlmChatModelTest {

    private static final String KEY = "sk-test-secret-key";
    private static final String ANSWER = """
            {"id":"c1","object":"chat.completion","created":1,"model":"test-model",
             "choices":[{"index":0,"message":{"role":"assistant","content":"hello from the stand-in"},
             "finish_reason":"stop"}],
             "usage":{"prompt_tokens":1,"completion_tokens":1,"total_tokens":2}}""";

    private record Received(String method, String path, String authorization, String body) {}

    private final List<Received> received = new CopyOnWriteArrayList<>();
    private HttpServer server;
    private Consumer<HttpExchange> reply = exchange -> respond(exchange, 200, ANSWER);

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            received.add(new Received(
                    exchange.getRequestMethod(),
                    exchange.getRequestURI().getPath(),
                    exchange.getRequestHeaders().getFirst("Authorization"),
                    new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
            reply.accept(exchange);
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private static void respond(HttpExchange exchange, int status, String body) {
        try (exchange) {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void withModel(String timeout, Consumer<ChatModel> test) {
        new ApplicationContextRunner()
                .withUserConfiguration(LlmConfig.class)
                .withPropertyValues(
                        "kinetiq.llm.api-key=" + KEY,
                        "kinetiq.llm.base-url=http://127.0.0.1:" + server.getAddress().getPort() + "/v1",
                        "kinetiq.llm.model=test-model",
                        "kinetiq.llm.timeout=" + timeout)
                .run(context -> test.accept(context.getBean(ChatModel.class)));
    }

    @Test
    void shouldCallTheConfiguredAddressWithKeyAndModel_andReadTheAnswer() {
        withModel("PT5S", model -> {
            String text = model.call(new Prompt("Draft a plan")).getResult().getOutput().getText();

            assertThat(text).isEqualTo("hello from the stand-in");
            assertThat(received).singleElement().satisfies(request -> {
                assertThat(request.method()).isEqualTo("POST");
                assertThat(request.path()).isEqualTo("/v1/chat/completions");
                assertThat(request.authorization()).isEqualTo("Bearer " + KEY);
                assertThat(request.body().replaceAll("\\s", "")).contains("\"model\":\"test-model\"");
                assertThat(request.body()).contains("Draft a plan");
            });
        });
    }

    @Test
    void shouldAskOnlyOnce_whenTheServerFails() {
        reply = exchange -> respond(exchange, 500, "{\"error\":{\"message\":\"busy\"}}");

        withModel("PT5S", model -> {
            assertThatThrownBy(() -> model.call(new Prompt("Draft a plan"))).isInstanceOf(RuntimeException.class);

            // the two attempts of a draft are counted by the domain, so the client adds no retry of its own
            assertThat(received).hasSize(1);
        });
    }

    @Test
    void shouldNotLeakTheKey_whenTheServerRefusesIt() {
        reply = exchange -> respond(exchange, 401, "{\"error\":{\"message\":\"Incorrect API key provided\"}}");

        withModel("PT5S", model -> {
            Throwable failure = catchThrowable(() -> model.call(new Prompt("hi")));

            assertThat(failure).isNotNull();
            List<String> texts = new ArrayList<>();
            for (Throwable t = failure; t != null; t = t.getCause()) {
                texts.add(String.valueOf(t.getMessage()));
                texts.add(t.toString());
            }
            assertThat(String.join("\n", texts)).doesNotContain(KEY);
        });
    }

    @Test
    void shouldNotShowTheKey_inTheTextFormOfTheModelOrItsOptions() {
        withModel("PT5S", model -> {
            OpenAiChatModel openAi = (OpenAiChatModel) model;

            assertThat(openAi.toString() + openAi.getOptions()).doesNotContain(KEY);
        });
    }

    @Test
    void shouldNotShowTheKey_inTheTextFormOfTheSettings() {
        new ApplicationContextRunner()
                .withUserConfiguration(LlmConfig.class)
                .withPropertyValues("kinetiq.llm.api-key=" + KEY, "kinetiq.llm.model=test-model")
                .run(context ->
                        assertThat(context.getBean(LlmConfig.Settings.class).toString()).doesNotContain(KEY));
    }

    @Test
    void shouldFailInsteadOfHanging_whenTheServerIsSlowerThanTheTimeout() {
        reply = exchange -> {
            try {
                Thread.sleep(Duration.ofSeconds(4));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            respond(exchange, 200, ANSWER);
        };

        withModel("PT1S", model -> {
            long start = System.nanoTime();

            assertThatThrownBy(() -> model.call(new Prompt("hi"))).isInstanceOf(RuntimeException.class);

            assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(3));
        });
    }
}
