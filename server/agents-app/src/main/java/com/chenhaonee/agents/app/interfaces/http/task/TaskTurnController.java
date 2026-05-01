package com.chenhaonee.agents.app.interfaces.http.task;

import com.chenhaonee.agents.app.application.task.TaskCommandApplicationService;
import com.chenhaonee.agents.app.application.task.TaskQueryApplicationService;
import com.chenhaonee.agents.app.interfaces.http.common.ExceptionHandlers;
import com.chenhaonee.agents.app.interfaces.http.common.Response;
import com.chenhaonee.agents.app.interfaces.http.task.dto.TaskTurnDetailDTO;
import com.chenhaonee.agents.app.interfaces.http.task.dto.TaskTurnListItemDTO;
import com.chenhaonee.agents.domain.task.model.TaskTurn;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 任务回合交互接口。
 */
@Tag(name = "TaskTurn", description = "任务回合查看与重置")
@RestController
@RequestMapping("/api/task-turns")
@RequiredArgsConstructor
public class TaskTurnController {

    private final TaskQueryApplicationService taskQueryApplicationService;
    private final TaskCommandApplicationService taskCommandApplicationService;

    @Operation(summary = "查询任务的所有回合列表")
    @GetMapping("/task/{taskCode}")
    public Response<List<TaskTurnListItemDTO>> listByTask(
            @Parameter(description = "任务编码") @PathVariable String taskCode) {
        try {
            List<TaskTurn> turns = taskQueryApplicationService.listTurns(taskCode);
            return Response.success(turns.stream().map(this::toListItemResponse).toList());
        } catch (Exception e) {
            return Response.error(ExceptionHandlers.handleException(e));
        }
    }

    @Operation(summary = "查询回合详情")
    @GetMapping("/{turnCode}")
    public Response<TaskTurnDetailDTO> detail(
            @Parameter(description = "回合编码") @PathVariable String turnCode) {
        try {
            TaskTurn turn = taskQueryApplicationService.getTurn(turnCode);
            return Response.success(toDetailResponse(turn));
        } catch (Exception e) {
            return Response.error(ExceptionHandlers.handleException(e));
        }
    }

    @Operation(summary = "重置 HANGING 状态的回合为 RUNNING")
    @PostMapping("/{turnCode}/resume")
    public Response<TaskTurnDetailDTO> resume(
            @Parameter(description = "回合编码") @PathVariable String turnCode) {
        try {
            taskCommandApplicationService.resumeTurn(turnCode);
            TaskTurn turn = taskQueryApplicationService.getTurn(turnCode);
            return Response.success(toDetailResponse(turn));
        } catch (Exception e) {
            return Response.error(ExceptionHandlers.handleException(e));
        }
    }

    private TaskTurnListItemDTO toListItemResponse(TaskTurn turn) {
        return new TaskTurnListItemDTO(
                turn.getCode(),
                turn.getTurnNo(),
                turn.getRunStatus().name(),
                turn.getProgress(),
                turn.getStartedAt().toString(),
                turn.getFinishedAt() != null ? turn.getFinishedAt().toString() : null,
                turn.getFinalSummary());
    }

    private TaskTurnDetailDTO toDetailResponse(TaskTurn turn) {
        return new TaskTurnDetailDTO(
                turn.getCode(),
                turn.getTaskCode(),
                turn.getTurnNo(),
                turn.getRunStatus().name(),
                turn.getProgress(),
                turn.getStartedAt().toString(),
                turn.getFinishedAt() != null ? turn.getFinishedAt().toString() : null,
                turn.getFinalSummary(),
                turn.getFinalDetail());
    }
}
