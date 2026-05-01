package com.chenhaonee.agents.domain.task.repository;

import com.chenhaonee.agents.domain.task.model.Task;
import com.chenhaonee.agents.domain.task.model.TaskSource;
import com.chenhaonee.agents.domain.task.model.TaskStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * 任务仓储。
 */
public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    Optional<Task> findByCode(String code);

    List<Task> findByStatus(TaskStatus status);

    List<Task> findByStatusInAndValidIsTrue(List<TaskStatus> statuses);

    Page<Task> findByValidIsTrueOrderByUpdateTimeDesc(Pageable pageable);

    Page<Task> findBySourceAndValidIsTrueOrderByUpdateTimeDesc(TaskSource source, Pageable pageable);

    Page<Task> findByStatusAndValidIsTrueOrderByUpdateTimeDesc(TaskStatus status, Pageable pageable);

    Page<Task> findBySourceAndStatusAndValidIsTrueOrderByUpdateTimeDesc(TaskSource source, TaskStatus status, Pageable pageable);
}
