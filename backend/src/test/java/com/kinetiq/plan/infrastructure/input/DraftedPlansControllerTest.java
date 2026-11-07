package com.kinetiq.plan.infrastructure.input;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.common.infrastructure.config.SecurityConfig;
import com.kinetiq.common.infrastructure.config.WebConfig;
import com.kinetiq.plan.domain.input.ListDraftedPlansUseCase;
import com.kinetiq.plan.domain.model.DraftedPlan;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(DraftedPlansController.class)
@Import({SecurityConfig.class, WebConfig.class})
class DraftedPlansControllerTest {

    private static final String ADDRESS = "/api/v1/approvals/drafted-plans";
    private static final AccountId TRAINER_ID = new AccountId(UUID.fromString("00000000-0000-4000-8000-000000000001"));
    private static final AccountId CLIENT_ID = new AccountId(UUID.fromString("00000000-0000-4000-8000-000000000003"));
    private static final RequestPostProcessor TRAINER = user(TRAINER_ID.value().toString()).roles("TRAINER");
    private static final RequestPostProcessor CLIENT = user(CLIENT_ID.value().toString()).roles("CLIENT");

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private ListDraftedPlansUseCase drafts;

    @Test
    void shouldListTheSignedInTrainersDrafts_inTheDocumentedShape() {
        when(drafts.draftedPlans(TRAINER_ID))
                .thenReturn(List.of(new DraftedPlan(
                        CLIENT_ID,
                        "Ola Kowalska",
                        DraftedPlan.Kind.FAILED,
                        12,
                        0,
                        Instant.parse("2026-11-07T09:30:00Z"))));

        var result = mvc.get().uri(ADDRESS).with(TRAINER).exchange();

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result)
                .bodyJson()
                .extractingPath("$.items[0]")
                .isEqualTo(Map.of(
                        "clientId", CLIENT_ID.value().toString(),
                        "clientName", "Ola Kowalska",
                        "kind", "FAILED",
                        "createdAt", "2026-11-07T09:30:00Z",
                        "weeks", 12,
                        "sessionsPerWeek", 0));
    }

    @Test
    void shouldAnswerAnEmptyList_whenNothingWaits() {
        when(drafts.draftedPlans(TRAINER_ID)).thenReturn(List.of());

        var result = mvc.get().uri(ADDRESS).with(TRAINER).exchange();

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("$.items").asArray().isEmpty();
    }

    @Test
    void shouldRefuse_whenCallerIsClientOrNobodyIsSignedIn() {
        var asClient = mvc.get().uri(ADDRESS).with(CLIENT).exchange();
        var anonymous = mvc.get().uri(ADDRESS).exchange();

        assertThat(asClient).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(asClient).bodyJson().extractingPath("$.code").isEqualTo("request.not-allowed");
        assertThat(anonymous).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(anonymous).bodyJson().extractingPath("$.code").isEqualTo("request.unauthenticated");
        verifyNoInteractions(drafts);
    }
}
