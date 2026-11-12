package com.kinetiq.client.infrastructure.input;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.client.domain.exception.ClientNotFoundException;
import com.kinetiq.client.domain.input.ViewClientsUseCase;
import com.kinetiq.client.domain.model.ClientSummary;
import com.kinetiq.client.domain.model.PlanState;
import com.kinetiq.common.infrastructure.config.SecurityConfig;
import com.kinetiq.common.infrastructure.config.WebConfig;
import com.kinetiq.quiz.domain.model.Goal;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(ClientController.class)
@Import({SecurityConfig.class, WebConfig.class, ClientWebMapperImpl.class})
class ClientControllerTest {

    private static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");
    private static final AccountId TRAINER_ID =
            new AccountId(UUID.fromString("00000000-0000-4000-8000-000000000001"));
    private static final RequestPostProcessor TRAINER = user(TRAINER_ID.value().toString()).roles("TRAINER");
    private static final RequestPostProcessor CLIENT =
            user("00000000-0000-4000-8000-000000000003").roles("CLIENT");

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private ViewClientsUseCase clients;

    @Test
    void shouldAnswerOneOfTheTrainersClients() {
        AccountId ola = new AccountId(UUID.fromString("00000000-0000-4000-8000-000000000003"));
        when(clients.getClient(TRAINER_ID, ola))
                .thenReturn(new ClientSummary(ola, "Ola Kowalska", Goal.BUILD_MUSCLE, PlanState.PLAN_TO_REVIEW, WARSAW));

        var result = mvc.get().uri("/api/v1/clients/" + ola.value()).with(TRAINER).exchange();

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("$.id").isEqualTo(ola.value().toString());
        assertThat(result).bodyJson().extractingPath("$.name").isEqualTo("Ola Kowalska");
        assertThat(result).bodyJson().extractingPath("$.goal").isEqualTo("BUILD_MUSCLE");
        assertThat(result).bodyJson().extractingPath("$.planState").isEqualTo("PLAN_TO_REVIEW");
    }

    @Test
    void shouldAnswerNotFound_whenTheClientIsUnknownOrAnotherTrainers() {
        AccountId other = new AccountId(UUID.fromString("00000000-0000-4000-8000-000000000009"));
        when(clients.getClient(TRAINER_ID, other)).thenThrow(new ClientNotFoundException());

        var result = mvc.get().uri("/api/v1/clients/" + other.value()).with(TRAINER).exchange();

        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("client.not-found");
    }

    @Test
    void shouldRefuseOneClient_whenCallerIsClientOrNobodyIsSignedIn() {
        String address = "/api/v1/clients/00000000-0000-4000-8000-000000000003";

        assertThat(mvc.get().uri(address).with(CLIENT).exchange()).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(mvc.get().uri(address).exchange()).hasStatus(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(clients);
    }
}
