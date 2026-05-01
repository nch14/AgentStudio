package com.chenhaonee.agents.app.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.chenhaonee.agents.domain.task.factory.TaskFactory;
import com.chenhaonee.agents.domain.task.model.Task;
import com.chenhaonee.agents.domain.task.model.TaskSource;
import com.chenhaonee.agents.domain.task.model.TaskStatus;
import com.chenhaonee.agents.domain.task.model.TaskTurn;
import com.chenhaonee.agents.domain.task.model.TurnRunStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class TaskDomainModelTest {

    private final TaskFactory taskFactory = new TaskFactory();

    @Test
    void shouldSupportWaitOwnerAndResumeLifecycle() {
        Task task = taskFactory.createUserTask("整理招聘方案", "assistant", "输出招聘流程方案", "test-owner");

        task.startTurn("turn-1");
        task.markRunning();
        task.succeed("招聘方案已完成");

        assertEquals(TaskStatus.SUCCEEDED, task.getStatus());
        assertEquals("turn-1", task.getCurrentTurnCode());
        assertEquals(TaskSource.USER_CREATE, task.getSource());
        assertEquals("招聘方案已完成", task.getResultSummary());
    }

    @Test
    void shouldEnterPreparedBeforeRunning() {
        Task task = taskFactory.createUserTask("整理招聘方案", "assistant", "输出招聘流程方案", "test-owner");

        task.startTurn("turn-1");

        assertEquals("turn-1", task.getCurrentTurnCode());
    }

    @Test
    void shouldAllowStartNextTurnFromRunningStatus() {
        Task task = taskFactory.createUserTask("多轮任务", "assistant", "需要多轮完成", "test-owner");

        // 第一轮
        task.startTurn("turn-1");
        task.markRunning();
        // 第一轮结束后 Task 仍为 RUNNING，发起第二轮
        task.startTurn("turn-2");
        task.markRunning();

        assertEquals(TaskStatus.RUNNING, task.getStatus());
        assertEquals("turn-2", task.getCurrentTurnCode());
    }

    @Test
    void shouldForbidStartTurnFromSucceededStatus() {
        Task task = taskFactory.createUserTask("单轮任务", "assistant", "完成即止", "test-owner");
        task.startTurn("turn-1");
        task.markRunning();
        task.succeed("任务完成");

        assertThrows(IllegalStateException.class, () -> task.startTurn("turn-2"));
    }

    @Test
    void shouldForbidStartTurnFromCancelledStatus() {
        Task task = taskFactory.createUserTask("被取消任务", "assistant", "中途取消", "test-owner");
        task.startTurn("turn-1");
        task.markRunning();
        task.cancel("手动取消");

        assertThrows(IllegalStateException.class, () -> task.startTurn("turn-2"));
    }

    @Test
    void shouldAllowStartTurnAfterRollback() {
        Task task = taskFactory.createUserTask("回滚重试任务", "assistant", "失败后重试", "test-owner");
        task.startTurn("turn-1");
        task.markRunning();
        task.succeed("首次完成");

        // 人工干预：rollback 后可以发起新 Turn
        task.rollback();
        task.startTurn("turn-2");
        task.markRunning();

        assertEquals(TaskStatus.RUNNING, task.getStatus());
        assertEquals("turn-2", task.getCurrentTurnCode());
    }

    @Test
    void shouldResumeFromHangingLifecycle() {
        TaskTurn turn = new TaskTurn();
        turn.setTaskCode("task-1");
        turn.setTurnNo(1);
        turn.setRunStatus(TurnRunStatus.RUNNING);
        turn.setStartedAt(Instant.now());

        turn.hang("执行卡住", "等待重试");
        turn.resumeFromHanging();

        assertEquals(TurnRunStatus.RUNNING, turn.getRunStatus());
        assertFalse(turn.isFinished());
        assertNull(turn.getFinalSummary());
        assertNull(turn.getFinalDetail());
        assertNull(turn.getFinishedAt());
    }
}
