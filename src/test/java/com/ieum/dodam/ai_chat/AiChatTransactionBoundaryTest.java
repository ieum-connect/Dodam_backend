package com.ieum.dodam.ai_chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.ieum.dodam.ai_chat.command.application.AiChatCommandService;
import com.ieum.dodam.ai_chat.domain.ChatRole;
import com.ieum.dodam.ai_chat.domain.entity.ChatSession;
import com.ieum.dodam.ai_chat.domain.repository.ChatMessageRepository;
import com.ieum.dodam.ai_chat.domain.repository.ChatSessionRepository;
import com.ieum.dodam.ai_result.command.application.AiEvidenceRetriever;
import com.ieum.dodam.ai_result.domain.QuestionType;
import com.ieum.dodam.ai_result.domain.repository.AiResultDocumentRepository;
import com.ieum.dodam.ai_result.infrastructure.AiResultGenerationClient;
import com.ieum.dodam.global.status.AiJobStatus;
import com.ieum.dodam.medical_document.query.application.CareRelationPermissionService;
import com.ieum.dodam.user.domain.User;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;
import java.util.UUID;

@SpringBootTest
class AiChatTransactionBoundaryTest {
    @Autowired AiChatCommandService chats;
    @Autowired ChatSessionRepository sessions;
    @Autowired ChatMessageRepository messages;
    @Autowired AiResultDocumentRepository results;
    @MockBean CareRelationPermissionService permissions;
    @MockBean AiEvidenceRetriever evidenceRetriever;
    @MockBean AiResultGenerationClient client;

    @Test
    void aiCallSeesCommittedQuestionAndResultWithoutTransaction() {
        User actor = actor();
        UUID patientId = UUID.randomUUID();
        ChatSession session = sessions.save(ChatSession.open(patientId, actor.getId(), "새 채팅"));
        QuestionType type = QuestionType.DOCUMENT_EASY_EXPLANATION;
        when(permissions.resolveTargetPatient(actor, patientId)).thenReturn(patientId);
        when(evidenceRetriever.retrieve(eq(patientId), any())).thenReturn(List.of(evidence()));
        when(client.generate(any())).thenAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            assertThat(messages.findBySessionIdOrderByCreatedAtAsc(session.getId()))
                    .extracting(message -> message.getRole()).containsExactly(ChatRole.USER);
            assertThat(results.findAll()).anySatisfy(result ->
                    assertThat(result.getJobStatus()).isEqualTo(AiJobStatus.RUNNING));
            return new AiResultGenerationClient.GenerationResponse(
                    "COMPLETE", "답변", "확인한 내용입니다.", List.of(), false, false, null, null);
        });

        var conversation = chats.ask(actor, session.getId(), "결과를 알려줘", type, List.of(UUID.randomUUID()));

        assertThat(conversation).hasSize(2);
        assertThat(results.findAll()).anySatisfy(result ->
                assertThat(result.getJobStatus()).isEqualTo(AiJobStatus.SUCCEEDED));
    }

    @Test
    void aiFailureIsSavedAfterCallWithoutTransaction() {
        User actor = actor();
        UUID patientId = UUID.randomUUID();
        ChatSession session = sessions.save(ChatSession.open(patientId, actor.getId(), "새 채팅"));
        when(permissions.resolveTargetPatient(actor, patientId)).thenReturn(patientId);
        when(evidenceRetriever.retrieve(eq(patientId), any())).thenReturn(List.of(evidence()));
        when(client.generate(any())).thenAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            throw new ResourceAccessException("connection failed");
        });

        chats.ask(actor, session.getId(), "결과를 알려줘", QuestionType.DOCUMENT_EASY_EXPLANATION,
                List.of(UUID.randomUUID()));

        assertThat(results.findAll()).anySatisfy(result ->
                assertThat(result.getJobStatus()).isEqualTo(AiJobStatus.FAILED));
        assertThat(messages.findBySessionIdOrderByCreatedAtAsc(session.getId()))
                .extracting(message -> message.getRole()).containsExactly(ChatRole.USER, ChatRole.ASSISTANT);
    }

    private User actor() {
        User actor = new User();
        actor.setId(UUID.randomUUID());
        return actor;
    }

    private AiResultGenerationClient.Evidence evidence() {
        return new AiResultGenerationClient.Evidence(UUID.randomUUID(), "EXTRACTED_ITEM",
                UUID.randomUUID(), UUID.randomUUID(), 1, "검사", "검사 결과", "1", "mg", "0-2");
    }
}
