package com.kinetiq.messaging.infrastructure.output;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

interface JpaMessageRepository extends JpaRepository<MessageEntity, UUID> {

    @Modifying
    @Query(
            nativeQuery = true,
            value = """
                    insert into messages (id, conversation_id, sender_id, text, received_at, about_set_log_id,
                                          created_at, updated_at, version)
                    values (:id, :conversation, :sender, :text, :receivedAt, cast(:aboutSetLog as uuid),
                            now(), now(), 0)
                    on conflict (id) do nothing
                    """)
    // the set log id is given as text and cast, so that a null has a type the database accepts
    int insertIfAbsent(
            UUID id, UUID conversation, UUID sender, String text, Instant receivedAt, String aboutSetLog);

    @Query("""
            select m from MessageEntity m
            where m.conversationId = :conversation
            order by m.receivedAt desc, m.id desc
            """)
    List<MessageEntity> findNewest(UUID conversation, Limit limit);

    @Query("""
            select m from MessageEntity m
            where m.conversationId = :conversation
              and (m.receivedAt < :receivedAt or (m.receivedAt = :receivedAt and m.id < :id))
            order by m.receivedAt desc, m.id desc
            """)
    List<MessageEntity> findBefore(UUID conversation, Instant receivedAt, UUID id, Limit limit);
}
