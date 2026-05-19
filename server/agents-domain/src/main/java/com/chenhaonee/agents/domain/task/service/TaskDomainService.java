package com.chenhaonee.agents.domain.task.service;

import com.chenhaonee.agents.domain.task.model.Task;
import com.chenhaonee.agents.domain.task.model.TaskSource;
import com.chenhaonee.agents.domain.task.model.TaskStatus;
import com.chenhaonee.agents.domain.task.repository.TaskRepository;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 任务领域服务。
 */
@Service
@RequiredArgsConstructor
public class TaskDomainService {

    private final TaskRepository taskRepository;

    @Transactional(rollbackFor = Exception.class)
    public Task startTurn(String taskCode, String turnCode) {
        Task task = requireTask(taskCode);
        task.startTurn(turnCode);
        return taskRepository.save(task);
    }


    @Transactional(rollbackFor = Exception.class)
    public Task markRunning(String taskCode) {
        Task task = requireTask(taskCode);
        task.markRunning();
        return taskRepository.save(task);
    }

    @Transactional(rollbackFor = Exception.class)
    public Task updateProgress(String taskCode, int progress) {
        Task task = requireTask(taskCode);
        task.updateProgress(progress);
        return taskRepository.save(task);
    }

    @Transactional(rollbackFor = Exception.class)
    public Task save(Task task) {
        return taskRepository.save(task);
    }

    @Transactional(rollbackFor = Exception.class)
    public Task complete(String taskCode, String resultSummary) {
        Task task = requireTask(taskCode);
        task.succeed(resultSummary);
        return taskRepository.save(task);
    }

    @Transactional(rollbackFor = Exception.class)
    public Task cancel(String taskCode, String resultSummary) {
        Task task = requireTask(taskCode);
        task.cancel(resultSummary);
        return taskRepository.save(task);
    }

    public Task requireTask(String taskCode) {
        Task task = taskRepository.findByCode(taskCode)
                .orElseThrow(() -> new NoSuchElementException("task not found: " + taskCode));
        if (!task.isValid()) {
            throw new NoSuchElementException("task not found: " + taskCode);
        }
        return task;
    }

    public Page<Task> listTasks(Pageable pageable) {
        return taskRepository.findByValidIsTrueOrderByUpdateTimeDesc(pageable);
    }

    public Page<Task> listTasksBySource(TaskSource source, Pageable pageable) {
        return taskRepository.findBySourceAndValidIsTrueOrderByUpdateTimeDesc(source, pageable);
    }

    public Page<Task> listTasksByStatus(TaskStatus status, Pageable pageable) {
        return taskRepository.findByStatusAndValidIsTrueOrderByUpdateTimeDesc(status, pageable);
    }

    public Page<Task> listTasks(TaskSource source, TaskStatus status, Pageable pageable) {
        return taskRepository.findBySourceAndStatusAndValidIsTrueOrderByUpdateTimeDesc(source, status, pageable);
    }

    /**
     * 支持 source / status / sourceRef 任意组合的分页查询。
     */
    public Page<Task> listTasks(TaskSource source, TaskStatus status, String sourceRef, Pageable pageable) {
        return taskRepository.findAll(buildListSpec(source, status, sourceRef), pageable);
    }

    private Specification<Task> buildListSpec(TaskSource source, TaskStatus status, String sourceRef) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isTrue(root.get("valid")));
            if (source != null) predicates.add(cb.equal(root.get("source"), source));
            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            if (sourceRef != null) predicates.add(cb.equal(root.get("sourceRef"), sourceRef));
            query.orderBy(cb.desc(root.get("updateTime")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
