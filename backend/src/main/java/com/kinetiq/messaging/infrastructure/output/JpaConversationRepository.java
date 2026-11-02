package com.kinetiq.messaging.infrastructure.output;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

interface JpaConversationRepository extends JpaRepository<ConversationEntity, UUID> {

    Optional<ConversationEntity> findByClientIdAndTrainerId(UUID clientId, UUID trainerId);

    @Modifying
    @Query(
            nativeQuery = true,
            value = """
                    insert into conversations (id, client_id, trainer_id, created_at, updated_at, version)
                    values (:id, :client, :trainer, now(), now(), 0)
                    on conflict (client_id, trainer_id) do nothing
                    """)
    void insertIfAbsent(UUID id, UUID client, UUID trainer);

    // one statement, so two people marking read at once never collide and the time never moves back
    @Modifying
    @Query(
            nativeQuery = true,
            value = """
                    insert into conversation_reads (conversation_id, account_id, last_read_at)
                    values (:conversation, :reader, :at)
                    on conflict (conversation_id, account_id) do update
                    set last_read_at = greatest(conversation_reads.last_read_at, excluded.last_read_at)
                    """)
    void markRead(UUID conversation, UUID reader, Instant at);
}
