package com.chenhaonee.agents.app.interfaces.http.task;

import com.chenhaonee.agents.app.application.task.TaskCommandApplicationService;
import com.chenhaonee.agents.app.application.task.TaskDetailView;
import com.chenhaonee.agents.app.application.task.TaskQueryApplicationService;
import com.chenhaonee.agents.app.interfaces.http.common.ExceptionHandlers;
import com.chenhaonee.agents.app.interfaces.http.common.PageResponse;
import com.chenhaonee.agents.app.interfaces.http.common.Response;
import com.chenhaonee.agents.app.interfaces.http.coordination.dto.AnswerDTO;
import com.chenhaonee.agents.app.interfaces.http.coordination.dto.QuestionDTO;
import com.chenhaonee.agents.app.interfaces.http.coordination.dto.QuestionsDTO;
import com.chenhaonee.agents.app.interfaces.http.task.dto.TaskCancelRequest;
import com.chenhaonee.agents.app.interfaces.http.task.dto.TaskCreateRequest;
import com.chenhaonee.agents.app.interfaces.http.task.dto.TaskDetailDTO;
import com.chenhaonee.agents.app.interfaces.http.task.dto.TaskListItemDTO;
import com.chenhaonee.agents.app.interfaces.http.task.dto.TaskUpdateRequest;
import com.chenhaonee.agents.domain.coordination.model.Questions;
import com.chenhaonee.agents.domain.task.model.Task;
import com.chenhaonee.agents.domain.task.model.TaskSource;
import com.chenhaonee.agents.domain.task.model.TaskStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

/**
 * task 交互接口。
 */
@Tag(name = "Task", description = "任务交互与观测")
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskQueryApplicationService taskQueryApplicationService;
    private final TaskCommandApplicationService taskCommandApplicationService;
    private final TaskHttpAssembler taskHttpAssembler;

    @Operation(summary = "分页查询任务列表")
    @GetMapping
    public PageResponse<TaskListItemDTO> list(
            @Parameter(description = "页码，从 0 开始")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "来源过滤（USER_CREATE/SCHEDULED_CREATE）")
            @RequestParam(required = false) String source,
            @Parameter(description = "状态过滤")
            @RequestParam(required = false) String status,
            @Parameter(description = "来源实体编码过滤，如 planCode 或 ownerCode")
            @RequestParam(required = false) String sourceRef) {
        try {
            TaskSource sourceEnum = source != null ? TaskSource.valueOf(source) : null;
            TaskStatus statusEnum = status != null ? TaskStatus.valueOf(status) : null;
            return PageResponse.from(
                    taskQueryApplicationService.listTasks(page, size, sourceEnum, statusEnum, sourceRef)
                            .map(this::toListItemResponse));
        } catch (Exception e) {
            return PageResponse.errorPage(ExceptionHandlers.handleException(e));
        }
    }

    @Operation(summary = "查询任务详情")
    @GetMapping("/{taskCode}")
    public Response<TaskDetailDTO> detail(
        @Parameter(description = "任务编码") @PathVariable String taskCode) {
        try {
            return Response.success(
                    taskHttpAssembler.toDetailResponse(taskQueryApplicationService.getTaskDetail(taskCode)));
        } catch (Exception e) {
            return Response.error(ExceptionHandlers.handleException(e));
        }
    }

    @Operation(summary = "取消任务")
    @PostMapping("/{taskCode}/cancel")
    public Response<TaskDetailDTO> cancel(
            @Parameter(description = "任务编码") @PathVariable String taskCode,
            @RequestBody(required = false) TaskCancelRequest request) {
        try {
            taskCommandApplicationService.cancel(taskCode, request != null ? request.reason() : null);
            return Response.success(
                    taskHttpAssembler.toDetailResponse(taskQueryApplicationService.getTaskDetail(taskCode)));
        } catch (Exception e) {
            return Response.error(ExceptionHandlers.handleException(e));
        }
    }

    @Operation(summary = "创建任务")
    @PostMapping
    public Response<TaskDetailDTO> create(@Valid @RequestBody TaskCreateRequest request) {
        try {
            Task task = taskCommandApplicationService.createTask(request.title(), request.agentCode(), request.description());
            return Response.success(
                    taskHttpAssembler.toDetailResponse(new TaskDetailView(task, null)));
        } catch (Exception e) {
            return Response.error(ExceptionHandlers.handleException(e));
        }
    }

    @Operation(summary = "修改任务属性")
    @PutMapping("/{taskCode}")
    public Response<TaskDetailDTO> update(
            @Parameter(description = "任务编码") @PathVariable String taskCode,
            @Valid @RequestBody TaskUpdateRequest request) {
        try {
            Task task = taskCommandApplicationService.updateTask(taskCode, request.title(), request.description());
            return Response.success(
                    taskHttpAssembler.toDetailResponse(new TaskDetailView(task, null)));
        } catch (Exception e) {
            return Response.error(ExceptionHandlers.handleException(e));
        }
    }

    @Operation(summary = "删除任务")
    @PostMapping("/{taskCode}/delete")
    public Response<Void> delete(
            @Parameter(description = "任务编码") @PathVariable String taskCode) {
        try {
            taskCommandApplicationService.deleteTask(taskCode);
            return Response.successWithMessage("任务已删除");
        } catch (Exception e) {
            return Response.error(ExceptionHandlers.handleException(e));
        }
    }

    @Operation(summary = "重试已结束任务")
    @PostMapping("/{taskCode}/retry")
    public Response<TaskDetailDTO> retry(
            @Parameter(description = "任务编码") @PathVariable String taskCode) {
        try {
            taskCommandApplicationService.retry(taskCode);
            return Response.success(
                    taskHttpAssembler.toDetailResponse(taskQueryApplicationService.getTaskDetail(taskCode)));
        } catch (Exception e) {
            return Response.error(ExceptionHandlers.handleException(e));
        }
    }

    @Operation(summary = "回滚已完成任务为 RUNNING")
    @PostMapping("/{taskCode}/rollback")
    public Response<TaskDetailDTO> rollback(
            @Parameter(description = "任务编码") @PathVariable String taskCode) {
        try {
            taskCommandApplicationService.rollback(taskCode);
            return Response.success(
                    taskHttpAssembler.toDetailResponse(taskQueryApplicationService.getTaskDetail(taskCode)));
        } catch (Exception e) {
            return Response.error(ExceptionHandlers.handleException(e));
        }
    }

    @Operation(summary = "查询当前待回答问题集")
    @GetMapping("/{taskCode}/questions/active")
    public ResponseEntity<Response<QuestionsDTO>> activeQuestions(
            @Parameter(description = "任务编码") @PathVariable String taskCode) {
        try {
            return taskQueryApplicationService.getActiveQuestions(taskCode)
                    .map(questions -> ResponseEntity.ok(Response.success(toQuestionsDTO(questions))))
                    .orElseGet(() -> ResponseEntity.noContent().build());
        } catch (Exception e) {
            return ResponseEntity.ok(Response.error(ExceptionHandlers.handleException(e)));
        }
    }

    private TaskListItemDTO toListItemResponse(Task task) {
        return new TaskListItemDTO(
                task.getCode(),
                task.getTitle(),
                task.getAgentCode(),
                task.getSource().name(),
                task.getStatus().name(),
                task.getProgress(),
                task.getFinishedAt() != null ? task.getFinishedAt().toString() : null,
                task.getCreateTime() != null ? task.getCreateTime().toString() : null,
                task.getUpdateTime() != null ? task.getUpdateTime().toString() : null);
    }

    private QuestionsDTO toQuestionsDTO(Questions questions) {
        return new QuestionsDTO(
                questions.getCode(),
                questions.isResolved(),
                questions.getOpenedAt() != null ? questions.getOpenedAt().toString() : null,
                questions.getResolvedAt() != null ? questions.getResolvedAt().toString() : null,
                questions.getQuestions() != null
                        ? questions.getQuestions().stream()
                        .map(q -> new QuestionDTO(q.code(), q.questionText(), q.options()))
                        .toList()
                        : java.util.List.of(),
                questions.getAnswers() != null
                        ? questions.getAnswers().stream()
                        .map(a -> new AnswerDTO(a.questionCode(), a.selectedOption(), a.userInput()))
                        .toList()
                        : java.util.List.of());
    }
}
