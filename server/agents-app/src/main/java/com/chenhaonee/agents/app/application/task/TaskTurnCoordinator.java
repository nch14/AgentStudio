package com.chenhaonee.agents.app.application.task;

import com.chenhaonee.agents.domain.task.model.TaskTurn;
import com.chenhaonee.agents.domain.task.service.TaskDomainService;
import com.chenhaonee.agents.domain.task.service.TaskTurnDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 协调 Turn 创建与 Task 状态绑定，保证两者在同一事务中原子提交。
 */
@Service
@RequiredArgsConstructor
public class TaskTurnCoordinator {

    private final TaskTurnDomainService taskTurnDomainService;
    private final TaskDomainService taskDomainService;

    /**
     * 创建下一个 Turn 并将 Task 绑定到该 Turn，进入执行态。
     * 三步操作在同一事务中，任意一步失败全部回滚，不会产生孤儿 Turn。
     */
    @Transactional(rollbackFor = Exception.class)
    public TaskTurn startNextTurn(String taskCode, int currentProgress) {
        TaskTurn newTurn = taskTurnDomainService.createNextTurn(taskCode, currentProgress);
        taskDomainService.startTurn(taskCode, newTurn.getCode());
        taskDomainService.markRunning(taskCode);
        return newTurn;
    }
}
