package com.chenhaonee.agents.domain.session.repository;

import com.chenhaonee.agents.domain.session.model.AgentMessage;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Agent 消息仓储。
 */
public interface AgentMessageRepository extends JpaRepository<AgentMessage, Long> {

    Optional<AgentMessage> findByCode(String code);

    Optional<AgentMessage> findTopBySessionCodeOrderByMessageIndexDesc(String sessionCode);

    List<AgentMessage> findBySessionCodeOrderByMessageIndexAsc(String sessionCode);

    Page<AgentMessage> findBySessionCodeOrderByMessageIndexAsc(String sessionCode, Pageable pageable);

    List<AgentMessage> findBySessionCodeAndTurnCodeInOrderByMessageIndexAsc(String sessionCode, Collection<String> turnCodes);

    boolean existsByTurnCode(String turnCode);

    boolean existsBySessionCodeAndTurnCode(String sessionCode, String turnCode);

    /**
     * 游标查询：获取最新的 N 个 turn code（按首条消息的 messageIndex 降序）。
     */
    @Query("""
            select m.turnCode
            from AgentMessage m
            where m.sessionCode = :sessionCode
            group by m.turnCode
            order by min(m.messageIndex) desc
            """)
    List<String> findLatestTurnCodesBySessionCodeDesc(
            @Param("sessionCode") String sessionCode, Pageable pageable);

    /**
     * 游标查询：获取首条 messageIndex 小于 cursor 的 N 个 turn code（降序）。
     */
    @Query("""
            select m.turnCode
            from AgentMessage m
            where m.sessionCode = :sessionCode
            group by m.turnCode
            having min(m.messageIndex) < :cursor
            order by min(m.messageIndex) desc
            """)
    List<String> findTurnCodesBySessionCodeBeforeCursorDesc(
            @Param("sessionCode") String sessionCode,
            @Param("cursor") int cursor,
            Pageable pageable);

    /**
     * 获取 messageIndex 大于等于 fromIndex 的消息，按 messageIndex 升序。
     */
    List<AgentMessage> findBySessionCodeAndMessageIndexGreaterThanEqualOrderByMessageIndexAsc(
            String sessionCode, int messageIndex);

    @Query(
            value = """
                    select m.turnCode
                    from AgentMessage m
                    where m.sessionCode = :sessionCode
                    group by m.turnCode
                    order by min(m.messageIndex)
                    """,
            countQuery = """
                    select count(distinct m.turnCode)
                    from AgentMessage m
                    where m.sessionCode = :sessionCode
                    """
    )
    Page<String> findTurnCodesBySessionCodeOrderByFirstMessageIndex(
            @Param("sessionCode") String sessionCode,
            Pageable pageable
    );
}
