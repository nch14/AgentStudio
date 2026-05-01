package com.chenhaonee.agents.app.interfaces.http.task;

import com.chenhaonee.agents.app.application.task.TaskDetailView;
import com.chenhaonee.agents.app.interfaces.http.task.dto.TaskDetailDTO;
import org.springframework.stereotype.Component;

/**
 * task HTTP DTO 装配器。
 */
@Component
public class TaskHttpAssembler {

    public TaskDetailDTO toDetailResponse(TaskDetailView view) {
        return new TaskDetailDTO(
                view.task().getCode(),
                view.task().getTitle(),
                view.task().getContent(),
                view.task().getAgentCode(),
                view.task().getSource().name(),
                view.task().getSourceRef(),
                view.task().getStatus().name(),
                view.task().getCurrentTurnCode(),
                view.currentTurn() != null ? view.currentTurn().getTurnNo() : null,
                view.task().getProgress(),
                view.task().getResultSummary(),
                view.task().getFinishedAt() != null ? view.task().getFinishedAt().toString() : null);
    }
}
