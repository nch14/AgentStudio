package com.chenhaonee.agents.domain.task.repository;

import com.chenhaonee.agents.domain.task.model.TaskTurn;
import com.chenhaonee.agents.domain.task.model.TurnRunStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 任务回合仓储。
 */
public interface TaskTurnRepository extends JpaRepository<TaskTurn, Long> {

    Optional<TaskTurn> findByCode(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select taskTurn from TaskTurn taskTurn where taskTurn.code = :code")
    Optional<TaskTurn> lockByCode(@Param("code") String code);

    Optional<TaskTurn> findTopByTaskCodeOrderByTurnNoDesc(String taskCode);

    List<TaskTurn> findByTaskCodeOrderByTurnNoDesc(String taskCode);

    List<TaskTurn> findByRunStatus(TurnRunStatus runStatus);
}
